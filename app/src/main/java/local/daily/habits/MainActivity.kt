package local.daily.habits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import local.daily.habits.data.*
import local.daily.habits.ui.AppRoot
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import local.daily.habits.backup.BackupService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo=HabitRepository(AppDatabase.get(this))
        val settings=SettingsStore(this)
        repo.onChanged={local.daily.habits.reminders.ReminderScheduler(this).reschedule()}
        lifecycleScope.launch { BackupService(this@MainActivity,repo,settings).recover();repo.onChanged();setContent { AppRoot(repo,settings) } }
    }
}
