package local.daily.habits.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable data class Habit(
    val id: String, val name: String, val start: String,
    val frequency: String = "DAILY", val days: List<Int> = emptyList(), val target: Int = 1,
    val symbol: String = "★", val color: Long = 0xFF6246D7,
    val group: String = "默认", val reminder: String = "", val archivedOn: String? = null,
    val versions: List<ScheduleVersion> = emptyList()
)
@Serializable data class ScheduleVersion(val from: String, val frequency: String, val days: List<Int>, val target: Int)
@Serializable data class Completion(val taskId: String, val date: String, val backfill: Boolean = false, val recordedAt: Long = System.currentTimeMillis())
@Serializable data class FlightEvent(val id: String, val date: String, val backfill: Boolean = false, val recordedAt: Long = System.currentTimeMillis())
@Serializable data class TemporaryTask(val id: String, val name: String, val date: String)
data class FlightStats(val todayCount: Int = 0, val weekCount: Int = 0, val monthCount: Int = 0, val totalCount: Int = 0, val activeDays: Int = 0, val currentStreak: Int = 0, val longestStreak: Int = 0)
data class PeriodProgress(val done: Int, val target: Int, val start: LocalDate, val end: LocalDate)

fun validateRecordDate(date: LocalDate, today: LocalDate) { require(!date.isAfter(today)) { "不能给未来日期打卡" } }
