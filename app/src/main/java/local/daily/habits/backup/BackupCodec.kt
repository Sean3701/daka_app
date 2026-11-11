package local.daily.habits.backup

import kotlinx.serialization.Serializable
import local.daily.habits.data.Snapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable data class BackupDocument(val schemaVersion:Int=1,val data:Snapshot=Snapshot(),val settings:Map<String,String> = emptyMap())
object BackupCodec {
    private val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
    fun encode(document:BackupDocument):String = json.encodeToString(document)
    fun decode(text:String):BackupDocument {
        val doc=json.decodeFromString<BackupDocument>(text)
        require(doc.schemaVersion==1){"不支持此备份版本"}
        val s=doc.data
        val taskIds = s.habits.map { it.id } + s.temporary.map { it.id }
        require(taskIds.all { it.isNotBlank() } && taskIds.distinct().size == taskIds.size) { "任务标识为空或重复" }
        s.habits.forEach { h ->
            validateSchedule(h.frequency, h.days, h.target)
            require(h.versions.map { it.from }.distinct().size == h.versions.size) { "历史计划日期重复" }
            h.versions.forEach { validateSchedule(it.frequency, it.days, it.target) }
            if (h.reminder.isNotBlank()) java.time.LocalTime.parse(h.reminder)
        }
        require(s.habits.map{it.id}.distinct().size==s.habits.size)
        require(s.flights.map{it.id}.distinct().size==s.flights.size)
        require(s.temporary.map{it.id}.distinct().size==s.temporary.size)
        require(s.completions.map{it.taskId to it.date}.distinct().size==s.completions.size)
        s.flights.forEach{LocalDate.parse(it.date);require(it.id.isNotBlank())}
        s.temporary.forEach{LocalDate.parse(it.date);require(it.name.isNotBlank())}
        s.habits.forEach{h->LocalDate.parse(h.start);require(h.name.isNotBlank() && h.target in 1..31);require(h.frequency in setOf("DAILY","WEEK_DAYS","MONTH_DAYS","WEEK_QUOTA","MONTH_QUOTA"));h.archivedOn?.let{LocalDate.parse(it)};h.versions.forEach{LocalDate.parse(it.from)}}
        s.completions.forEach{c->LocalDate.parse(c.date);require(s.habits.any{it.id==c.taskId}||s.temporary.any{it.id==c.taskId}){"记录引用的任务不存在"}}
        listOf("wallpaper","audio").forEach { key->doc.settings[key]?.takeIf{it.isNotEmpty()}?.let{require(safePath("assets/$it") && !it.contains('/'))} }
        return doc
    }
    private fun validateSchedule(frequency: String, days: List<Int>, target: Int) {
        require(frequency in setOf("DAILY", "WEEK_DAYS", "MONTH_DAYS", "WEEK_QUOTA", "MONTH_QUOTA")) { "无效的计划频率" }
        require(target in 1..if (frequency == "WEEK_QUOTA") 7 else 31) { "目标天数超出范围" }
        if (frequency.endsWith("DAYS")) {
            require(days.isNotEmpty() && days.distinct().size == days.size && days.all { it in 1..if (frequency == "WEEK_DAYS") 7 else 31 }) { "计划日期超出范围或重复" }
        }
    }
    fun safePath(path:String):Boolean = path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') && !path.contains(':') && path.split('/').all{it.isNotBlank() && it!="." && it!=".."}
}
