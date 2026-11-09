package local.daily.habits.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class RepositoryTest {
    @Test fun retryIsIdempotentAndIndependentClicksRemainSeparate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.memory(context)
        try {
            val repo = HabitRepository(db)
            val date = LocalDate.now()
            val op = UUID.randomUUID().toString()
            repo.addFlight(date, op)
            repo.addFlight(date, op)
            assertEquals(1, repo.observeDay(date).first().flights.size)
            val second = repo.addFlight(date, UUID.randomUUID().toString())
            assertEquals(2, repo.observeDay(date).first().flights.size)
            repo.deleteFlight(second.id)
            assertEquals(1, repo.observeDay(date).first().flights.size)
        } finally { db.close() }
    }
    @Test fun pastRecordsAreBackfillsAndFutureRecordsRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.memory(context)
        try {
            val repo = HabitRepository(db)
            assertTrue(repo.addFlight(LocalDate.now().minusDays(1), "past").backfill)
            try { repo.addFlight(LocalDate.now().plusDays(1), "future"); fail("Future date accepted") }
            catch (_: IllegalArgumentException) { }
        } finally { db.close() }
    }
}
