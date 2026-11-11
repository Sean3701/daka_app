package local.daily.habits.domain

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

object Statistics {
    // Ordinary habits count scheduled days; quotas count completed calendar periods.
    // A still-open day/period does not break the preceding streak.
    fun habit(habit: Habit, completions: List<Completion>, today: LocalDate): HabitStats {
        val start = LocalDate.parse(habit.start)
        val end = minOf(today, habit.archivedOn?.let { LocalDate.parse(it).minusDays(1) } ?: today)
        val dates = completions.asSequence().filter { it.taskId == habit.id }
            .map { LocalDate.parse(it.date) }.filter { it >= start && it <= end }.toSet()
        fun unit(frequency: String) = when (frequency) {
            "WEEK_QUOTA" -> "周"
            "MONTH_QUOTA" -> "月"
            else -> "次"
        }
        var currentUnit = unit(ScheduleEngine.effective(habit, end).frequency)
        var current = 0
        var longest = 0
        var cursor = start
        while (cursor <= end) {
            val active = ScheduleEngine.effective(habit, cursor)
            val nextUnit = unit(active.frequency)
            if (nextUnit != currentUnit) { current = 0; longest = 0; currentUnit = nextUnit }
            if (active.frequency.endsWith("QUOTA")) {
                val period = ScheduleEngine.period(habit, cursor, emptyList())
                val nextVersion = habit.versions.map { LocalDate.parse(it.from) }.filter { it > cursor }.minOrNull()
                val segmentEnd = minOf(period.end, nextVersion?.minusDays(1) ?: period.end)
                val available = java.time.temporal.ChronoUnit.DAYS.between(cursor, segmentEnd).toInt() + 1
                val target = minOf(active.target, available)
                val done = dates.count { it >= cursor && it <= segmentEnd }
                if (done >= target) current++ else if (segmentEnd < today || end < today) current = 0
                cursor = segmentEnd.plusDays(1)
            } else {
                if (ScheduleEngine.isDue(habit, cursor, emptyList())) {
                    if (cursor in dates) current++ else if (cursor < today) current = 0
                }
                cursor = cursor.plusDays(1)
            }
            longest = maxOf(longest, current)
        }
        return HabitStats(dates.size, current, longest, currentUnit)
    }

    fun flight(events: List<FlightEvent>, today: LocalDate): FlightStats {
        val dates = events.map { LocalDate.parse(it.date) }.filter { !it.isAfter(today) }
        val unique = dates.toSortedSet()
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        unique.forEach { date ->
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }
        var current = 0
        var cursor = if (today in unique) today else today.minusDays(1)
        while (cursor in unique) { current++; cursor = cursor.minusDays(1) }
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return FlightStats(dates.count { it == today }, dates.count { !it.isBefore(weekStart) },
            dates.count { it.year == today.year && it.month == today.month }, dates.size, unique.size, current, longest)
    }
}

data class HabitStats(val completedDays: Int, val currentStreak: Int, val longestStreak: Int, val unit: String)
