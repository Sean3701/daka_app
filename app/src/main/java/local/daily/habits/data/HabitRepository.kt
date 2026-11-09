package local.daily.habits.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import local.daily.habits.domain.*
import java.time.LocalDate
import java.util.UUID

@Serializable data class Snapshot(val habits: List<Habit> = emptyList(), val completions: List<Completion> = emptyList(), val flights: List<FlightEvent> = emptyList(), val temporary: List<TemporaryTask> = emptyList())
data class DayState(val habits: List<Habit>, val temporary: List<TemporaryTask>, val completions: List<Completion>, val flights: List<FlightEvent>, val stats: FlightStats)
class HabitRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    var onChanged: suspend () -> Unit = {}
    val snapshot: Flow<Snapshot> = combine(dao.habits(), dao.completions(), dao.flights(), dao.temporary()) { h, c, f, t ->
        Snapshot(h.map { json.decodeFromString<Habit>(it.payload) }, c.map { Completion(it.taskId,it.date,it.backfill,it.recordedAt) }, f.map { FlightEvent(it.id,it.date,it.backfill,it.recordedAt) }, t.map { TemporaryTask(it.id,it.name,it.date) })
    }
    fun observeDay(date: LocalDate): Flow<DayState> = snapshot.map { s -> DayState(s.habits.filter { ScheduleEngine.isDue(it,date,s.completions) },s.temporary.filter { it.date == date.toString() },s.completions.filter { it.date == date.toString() },s.flights.filter { it.date == date.toString() },Statistics.flight(s.flights,LocalDate.now())) }
    suspend fun addFlight(date: LocalDate, operationId: String): FlightEvent {
        validateRecordDate(date,LocalDate.now())
        val event = FlightEvent(operationId,date.toString(),date < LocalDate.now())
        dao.putFlight(FlightRow(event.id,event.date,event.backfill,event.recordedAt)); onChanged(); return event
    }
    suspend fun deleteFlight(id: String) { dao.removeFlight(id); onChanged() }
    suspend fun setCompleted(taskId: String, date: LocalDate, completed: Boolean) {
        validateRecordDate(date,LocalDate.now())
        val s=snapshot.first()
        require(s.habits.any { it.id==taskId && date.toString()>=it.start } || s.temporary.any { it.id==taskId && it.date==date.toString() }) { "任务不存在或尚未开始" }
        if(completed) dao.putCompletion(CompletionRow(taskId,date.toString(),date<LocalDate.now(),System.currentTimeMillis())) else dao.removeCompletion(taskId,date.toString())
        onChanged()
    }
    suspend fun saveHabit(habit: Habit) {
        require(habit.name.isNotBlank()) { "请输入名称" }
        require(habit.target in 1..31)
        val old=snapshot.first().habits.find { it.id==habit.id }
        val today=LocalDate.now()
        val changed=old!=null && (old.frequency!=habit.frequency || old.days!=habit.days || old.target!=habit.target)
        val saved=if(changed && old!=null) {
            val effective=ScheduleEngine.effective(old,today)
            val from=if(effective.frequency.endsWith("QUOTA")) ScheduleEngine.period(old,today,emptyList()).end.plusDays(1) else today
            val history=if(old.versions.isEmpty()) listOf(ScheduleVersion(old.start,old.frequency,old.days,old.target)) else old.versions
            habit.copy(versions=history.filter { it.from < from.toString() }+ScheduleVersion(from.toString(),habit.frequency,habit.days,habit.target))
        } else habit
        dao.putHabit(HabitRow(saved.id,json.encodeToString(saved))); onChanged()
    }
    suspend fun archiveHabit(id: String) { snapshot.first().habits.find { it.id==id }?.let { dao.putHabit(HabitRow(id,json.encodeToString(it.copy(archivedOn=LocalDate.now().toString())))) }; onChanged() }
    suspend fun addTemporary(name: String) { require(name.isNotBlank()); dao.putTemporary(TemporaryRow(UUID.randomUUID().toString(),name.trim(),LocalDate.now().toString())); onChanged() }
    suspend fun deleteTemporary(id: String) { db.withTransaction { val t=snapshot.first().temporary.find { it.id==id }; if(t!=null) dao.removeCompletion(id,t.date); dao.removeTemporary(id) }; onChanged() }
    suspend fun replace(s: Snapshot) { db.withTransaction {
        dao.clearCompletions(); dao.clearFlights(); dao.clearTemporary(); dao.clearHabits()
        s.habits.forEach { dao.putHabit(HabitRow(it.id,json.encodeToString(it))) }
        s.completions.forEach { dao.putCompletion(CompletionRow(it.taskId,it.date,it.backfill,it.recordedAt)) }
        s.flights.forEach { dao.putFlight(FlightRow(it.id,it.date,it.backfill,it.recordedAt)) }
        s.temporary.forEach { dao.putTemporary(TemporaryRow(it.id,it.name,it.date)) }
    }; onChanged() }
}
