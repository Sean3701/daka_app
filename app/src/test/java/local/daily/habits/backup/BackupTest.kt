package local.daily.habits.backup

import org.junit.Assert.*
import org.junit.Test
import local.daily.habits.domain.FlightEvent
import local.daily.habits.data.Snapshot

class BackupTest {
    @Test fun invalidScheduleHistoryIsRejected() {
        val habit = local.daily.habits.domain.Habit("h", "阅读", "2026-11-01",
            versions = listOf(local.daily.habits.domain.ScheduleVersion("2026-11-02", "WEEK_QUOTA", emptyList(), 0)))
        val doc = BackupDocument(data = Snapshot(habits = listOf(habit)))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(doc)) }
    }
    @Test fun overlappingTaskIdsAreRejected() {
        val doc = BackupDocument(data = Snapshot(
            habits = listOf(local.daily.habits.domain.Habit("h", "阅读", "2026-11-01")),
            temporary = listOf(local.daily.habits.domain.TemporaryTask("h", "临时", "2026-11-01"))))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(doc)) }
    }
    @Test fun invalidScheduledDaysAreRejected() {
        val doc = BackupDocument(data = Snapshot(habits = listOf(
            local.daily.habits.domain.Habit("h", "阅读", "2026-11-01", "WEEK_DAYS", listOf(8)))))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(doc)) }
    }
    @Test fun roundTripRetainsMultipleEventsAndBackfillFlag() {
        val source=BackupDocument(data=Snapshot(flights=listOf(FlightEvent("a","2026-11-01"),FlightEvent("b","2026-11-01",true))))
        assertEquals(source,BackupCodec.decode(BackupCodec.encode(source)))
    }
    @Test fun futureSchemaIsRejected() { assertThrows(IllegalArgumentException::class.java){BackupCodec.decode("{\"schemaVersion\":99}")} }
    @Test fun archivePathsCannotEscapeOrAlias() {
        listOf("../secret","/absolute","C:/file","assets/../file","assets\\file","assets//file").forEach { assertFalse(it,BackupCodec.safePath(it)) }
        assertTrue(BackupCodec.safePath("assets/wallpaper.png"))
    }
    @Test fun duplicateFlightIdsAreRejected() {
        val source=BackupDocument(data=Snapshot(flights=listOf(FlightEvent("a","2026-11-01"),FlightEvent("a","2026-11-02"))))
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(BackupCodec.encode(source))}
    }
}
