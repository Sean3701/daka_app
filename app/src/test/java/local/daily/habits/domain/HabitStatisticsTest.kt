package local.daily.habits.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class HabitStatisticsTest {
    private fun records(vararg dates: String) = dates.map { Completion("h", it) }
    private fun stats(h: Habit, today: String, vararg dates: String) =
        Statistics.habit(h, records(*dates), LocalDate.parse(today))

    @Test fun scheduledDaysSkipRestDaysAndKeepTodayPending() {
        val h = Habit("h", "运动", "2026-11-02", "WEEK_DAYS", listOf(1, 3, 5))
        val s = stats(h, "2026-11-06", "2026-11-02", "2026-11-04")
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.longestStreak)
        assertEquals(0, stats(h, "2026-11-07", "2026-11-02", "2026-11-04").currentStreak)
    }

    @Test fun backfillRepairsDailyStreakAndFutureRecordsAreIgnored() {
        val h = Habit("h", "阅读", "2026-11-01")
        assertEquals(1, stats(h, "2026-11-04", "2026-11-01", "2026-11-03").currentStreak)
        val s = stats(h, "2026-11-04", "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-05")
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.completedDays)
    }

    @Test fun quotaCountsSuccessfulPeriodsAndKeepsOpenPeriodPending() {
        val h = Habit("h", "运动", "2026-11-02", "WEEK_QUOTA", target = 2)
        assertEquals(1, stats(h, "2026-11-11", "2026-11-02", "2026-11-03").currentStreak)
        assertEquals(2, stats(h, "2026-11-11", "2026-11-02", "2026-11-03", "2026-11-09", "2026-11-10").currentStreak)
        assertEquals(0, stats(h, "2026-11-16", "2026-11-02", "2026-11-03").currentStreak)
    }

    @Test fun partialFirstPeriodAndDuplicateDays() {
        val h = Habit("h", "运动", "2026-11-08", "WEEK_QUOTA", target = 7)
        assertEquals(1, stats(h, "2026-11-09", "2026-11-08", "2026-11-08").currentStreak)
    }

    @Test fun missingMonthDayIsNotAFailure() {
        val h = Habit("h", "月末", "2028-01-01", "MONTH_DAYS", listOf(31))
        assertEquals(2, stats(h, "2028-04-01", "2028-01-31", "2028-03-31").currentStreak)
    }

    @Test fun monthlyQuotaAndArchiveStopAtLastActiveDate() {
        val h = Habit("h", "阅读", "2026-09-01", "MONTH_QUOTA", target = 1, archivedOn = "2026-11-01")
        val s = stats(h, "2026-11-11", "2026-09-02", "2026-10-02")
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.longestStreak)
    }

    @Test fun frequencyChangeStartsANewStreakUnit() {
        val h = Habit("h", "阅读", "2026-11-01", "WEEK_QUOTA", target = 1,
            versions = listOf(ScheduleVersion("2026-11-01", "DAILY", emptyList(), 1),
                ScheduleVersion("2026-11-09", "WEEK_QUOTA", emptyList(), 1)))
        val s = stats(h, "2026-11-10", "2026-11-07", "2026-11-08", "2026-11-09")
        assertEquals(1, s.currentStreak)
        assertEquals("周", s.unit)
    }
}
