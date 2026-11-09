package local.daily.habits.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits") data class HabitRow(@PrimaryKey val id: String, val payload: String)
@Entity(tableName = "completions", primaryKeys = ["taskId", "date"]) data class CompletionRow(val taskId: String, val date: String, val backfill: Boolean, val recordedAt: Long)
@Entity(tableName = "flights") data class FlightRow(@PrimaryKey val id: String, val date: String, val backfill: Boolean, val recordedAt: Long)
@Entity(tableName = "temporary") data class TemporaryRow(@PrimaryKey val id: String, val name: String, val date: String)
@Dao interface HabitDao {
    @Query("SELECT * FROM habits") fun habits(): Flow<List<HabitRow>>
    @Query("SELECT * FROM completions") fun completions(): Flow<List<CompletionRow>>
    @Query("SELECT * FROM flights ORDER BY recordedAt DESC") fun flights(): Flow<List<FlightRow>>
    @Query("SELECT * FROM temporary") fun temporary(): Flow<List<TemporaryRow>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabit(row: HabitRow)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun putCompletion(row: CompletionRow)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun putFlight(row: FlightRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTemporary(row: TemporaryRow)
    @Query("DELETE FROM completions WHERE taskId=:id AND date=:date") suspend fun removeCompletion(id: String, date: String)
    @Query("DELETE FROM flights WHERE id=:id") suspend fun removeFlight(id: String)
    @Query("DELETE FROM temporary WHERE id=:id") suspend fun removeTemporary(id: String)
    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM completions") suspend fun clearCompletions()
    @Query("DELETE FROM flights") suspend fun clearFlights()
    @Query("DELETE FROM temporary") suspend fun clearTemporary()
}
@Database(entities = [HabitRow::class, CompletionRow::class, FlightRow::class, TemporaryRow::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): HabitDao
    companion object {
        fun memory(context: Context) = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "habits.db").build().also { instance = it }
        }
    }
}
