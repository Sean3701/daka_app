package local.daily.habits.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReminderTest {
    private val date=LocalDate.of(2026,11,9)
    @Test fun completedHabitDoesNotRemind() {
        val habit=Habit("h","阅读","2026-11-01",reminder="09:00")
        assertFalse(ReminderPolicy.shouldNotify(habit,date,listOf(Completion("h",date.toString()))))
        assertTrue(ReminderPolicy.shouldNotify(habit,date,emptyList()))
    }
    @Test fun archivedAndDisabledRemindersStaySilent() {
        assertFalse(ReminderPolicy.shouldNotify(Habit("h","阅读","2026-11-01"),date,emptyList()))
        assertFalse(ReminderPolicy.shouldNotify(Habit("h","阅读","2026-11-01",reminder="09:00",archivedOn="2026-11-08"),date,emptyList()))
    }
}
