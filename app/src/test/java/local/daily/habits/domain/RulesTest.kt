package local.daily.habits.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private val today = LocalDate.of(2026, 11, 8)
    @Test fun multipleFlightsCountEventsButOnlyOneDay() {
        val result = Statistics.flight(listOf("a", "b", "c").map { FlightEvent(it, today.toString()) }, today)
        assertEquals(3, result.todayCount)
        assertEquals(3, result.totalCount)
        assertEquals(1, result.activeDays)
        assertEquals(1, result.currentStreak)
    }
    @Test fun backfillRepairsStreakAndDeletionBreaksIt() {
        val events = listOf(5, 6, 8).map { FlightEvent("$it", "2026-11-0$it") }
        assertEquals(1, Statistics.flight(events, today).currentStreak)
        val fixed = Statistics.flight(events + FlightEvent("7", "2026-11-07", backfill = true), today)
        assertEquals(4, fixed.currentStreak)
        assertEquals(4, fixed.longestStreak)
    }
    @Test fun yesterdayStreakSurvivesUntilTodayEnds() {
        assertEquals(2, Statistics.flight(listOf(FlightEvent("6", "2026-11-06"), FlightEvent("7", "2026-11-07")), today).currentStreak)
    }
    @Test fun monthlyDatesSkipMissingDayAndHonorLeapYear() {
        val habit = Habit("h", "测试", "2028-01-01", frequency = "MONTH_DAYS", days = listOf(29, 31))
        assertFalse(ScheduleEngine.isDue(habit, LocalDate.of(2028, 2, 28), emptyList()))
        assertTrue(ScheduleEngine.isDue(habit, LocalDate.of(2028, 2, 29), emptyList()))
    }
    @Test fun periodTargetHidesRemainingDaysButKeepsCompletedDay() {
        val habit = Habit("h", "测试", "2026-11-01", frequency = "WEEK_QUOTA", target = 2)
        val done = listOf(Completion("h", "2026-11-02"), Completion("h", "2026-11-03"))
        assertFalse(ScheduleEngine.isDue(habit, today, done))
        assertTrue(ScheduleEngine.isDue(habit, LocalDate.of(2026, 11, 3), done))
    }
    @Test fun futureWritesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { validateRecordDate(today.plusDays(1), today) }
    }
    @Test fun weekAndMonthCountsUseCalendarBoundaries() {
        val events = listOf("2026-10-31", "2026-11-01", "2026-11-02", "2026-11-08")
            .map { FlightEvent(it, it) }
        val stats = Statistics.flight(events, today)
        assertEquals(2, stats.weekCount)
        assertEquals(3, stats.monthCount)
        assertEquals(4, stats.totalCount)
    }
    @Test fun firstPartialPeriodCapsTargetToAvailableDays() {
        val habit = Habit("h", "测试", "2026-11-08", frequency = "WEEK_QUOTA", target = 7)
        assertTrue(ScheduleEngine.isDue(habit, today, emptyList()))
        assertFalse(ScheduleEngine.isDue(habit, today.minusDays(1), emptyList()))
    }
    @Test fun archivedHabitKeepsPastButStopsNewSchedule() {
        val habit = Habit("h", "测试", "2026-11-01", archivedOn = "2026-11-07")
        assertTrue(ScheduleEngine.isDue(habit, today.minusDays(2), emptyList()))
        assertFalse(ScheduleEngine.isDue(habit, today, emptyList()))
    }
}
