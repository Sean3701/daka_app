package local.daily.habits.domain

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

object Statistics {
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
