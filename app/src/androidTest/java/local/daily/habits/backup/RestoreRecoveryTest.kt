package local.daily.habits.backup

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import local.daily.habits.data.*
import local.daily.habits.domain.Habit
import org.junit.Assert.*
import org.junit.Test

class RestoreRecoveryTest {
    @Test fun interruptedRestoreRecoversDataAndSettingsAndIsIdempotent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.memory(context)
        val settings = SettingsStore(context)
        val originalSettings = settings.values.first()
        val journal = File(context.filesDir, "restore-journal.json")
        check(!journal.exists()) { "Recovery fixture requires no pending restore" }
        try {
            val repo = HabitRepository(db)
            val old = BackupDocument(data = Snapshot(habits = listOf(Habit("old", "阅读", "2026-11-01"))),
                settings = mapOf("theme" to "深色"))
            journal.writeText(BackupCodec.encode(old))
            repo.replace(Snapshot(habits = listOf(Habit("new", "运动", "2026-11-02"))))
            settings.replace(mapOf("theme" to "浅色"))
            val service = BackupService(context, repo, settings)
            service.recover()
            assertEquals(old.data, repo.snapshot.first())
            assertEquals(old.settings, settings.values.first())
            assertFalse(journal.exists())
            service.recover()
            assertEquals(old.data, repo.snapshot.first())
        } finally {
            journal.delete()
            settings.replace(originalSettings)
            db.close()
        }
    }

    @Test fun malformedImportDoesNotReplaceExistingRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.memory(context)
        try {
            val repo = HabitRepository(db)
            val before = Snapshot(habits = listOf(Habit("keep", "保留", "2026-11-01")))
            repo.replace(before)
            try {
                BackupService(context, repo, SettingsStore(context)).inspect("{broken".byteInputStream())
                fail("Malformed backup accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, repo.snapshot.first())
        } finally { db.close() }
    }
}
