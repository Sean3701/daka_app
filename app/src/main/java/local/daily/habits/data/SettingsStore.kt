package local.daily.habits.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")
class SettingsStore(context: Context) {
    private val store=context.applicationContext.settingsDataStore
    val values=store.data.map { p -> p.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() } }
    suspend fun set(key: String,value: String) { store.edit { it[stringPreferencesKey(key)]=value } }
    suspend fun replace(values: Map<String,String>) { store.edit { p -> p.clear(); values.forEach { (k,v)->p[stringPreferencesKey(k)]=v } } }
}
