package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

@Entity(tableName = "battery_records")
data class BatteryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val level: Int,
    val temperature: Float, // in Celsius
    val voltage: Float, // in Volts
    val currentNow: Int, // in mA (negative for discharging, positive for charging)
    val isCharging: Boolean
)

@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val maxCurrent: Int, // max charging speed in mA
    val avgTemp: Float,
    val quality: String // "Slow Charger", "Fast Charger", "Premium VOOC", "Optimum USB"
)

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_records ORDER BY timestamp ASC")
    fun getAllRecords(): Flow<List<BatteryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BatteryRecord)

    @Query("SELECT * FROM battery_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): BatteryRecord?

    @Query("DELETE FROM battery_records")
    suspend fun clearRecords()

    // Charging session DAOs
    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSession)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearSessions()
}

@Database(entities = [BatteryRecord::class, ChargingSession::class], version = 1, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract val batteryDao: BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "voltmax_battery_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
