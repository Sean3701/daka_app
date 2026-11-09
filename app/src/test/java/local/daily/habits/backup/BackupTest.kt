package local.daily.habits.backup

import org.junit.Assert.*
import org.junit.Test
import local.daily.habits.domain.FlightEvent
import local.daily.habits.data.Snapshot

class BackupTest {
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
