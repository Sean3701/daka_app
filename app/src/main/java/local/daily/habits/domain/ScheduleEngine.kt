package local.daily.habits.domain

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

object ScheduleEngine {
    fun effective(habit: Habit, date: LocalDate): Habit {
        val version = habit.versions.filter { it.from <= date.toString() }.maxByOrNull { it.from } ?: return habit
        return habit.copy(frequency = version.frequency, days = version.days, target = version.target)
    }
    fun period(habit: Habit, date: LocalDate, completions: List<Completion>): PeriodProgress {
        val active = effective(habit, date)
        val start = if (active.frequency == "WEEK_QUOTA") date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else date.withDayOfMonth(1)
        val end = if (active.frequency == "WEEK_QUOTA") start.plusDays(6) else start.plusMonths(1).minusDays(1)
        val effectiveStart = maxOf(start, LocalDate.parse(active.start))
        val available = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, end).toInt() + 1
        val done = completions.filter { it.taskId == habit.id && it.date >= effectiveStart.toString() && it.date <= end.toString() }.map { it.date }.distinct().size
        return PeriodProgress(done, minOf(active.target, available.coerceAtLeast(1)), start, end)
    }
    fun isDue(habit: Habit, date: LocalDate, completions: List<Completion>): Boolean {
        if (date.toString() < habit.start) return false
        if (completions.any { it.taskId == habit.id && it.date == date.toString() }) return true
        if (habit.archivedOn != null && date.toString() >= habit.archivedOn) return false
        val active = effective(habit, date)
        return when (active.frequency) {
            "DAILY" -> true
            "WEEK_DAYS" -> date.dayOfWeek.value in active.days
            "MONTH_DAYS" -> date.dayOfMonth in active.days
            "WEEK_QUOTA", "MONTH_QUOTA" -> period(habit, date, completions).let { it.done < it.target }
            else -> false
        }
    }
}
