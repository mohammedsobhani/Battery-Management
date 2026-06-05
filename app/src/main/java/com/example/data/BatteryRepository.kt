package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class BatteryRepository(private val batteryDao: BatteryDao) {

    val allRecords: Flow<List<BatteryRecord>> = batteryDao.getAllRecords()
    val allSessions: Flow<List<ChargingSession>> = batteryDao.getAllSessions()

    suspend fun insertRecord(record: BatteryRecord) {
        batteryDao.insertRecord(record)
    }

    suspend fun getLatestRecord(): BatteryRecord? {
        return batteryDao.getLatestRecord()
    }

    suspend fun insertSession(session: ChargingSession) {
        batteryDao.insertSession(session)
    }

    suspend fun clearAll() {
        batteryDao.clearRecords()
        batteryDao.clearSessions()
    }

    /**
     * Fills database with 48 hours of simulated usage if empty
     * to prevent showing a blank graph and empty logs initially.
     */
    suspend fun prepopulateIfEmpty() {
        // Just checking if there is any record
        val latest = getLatestRecord()
        if (latest != null) return // Already populated

        val now = System.currentTimeMillis()
        val oneHourMs = TimeUnit.HOURS.toMillis(1)

        // Generate 48 hourly records
        val records = mutableListOf<BatteryRecord>()
        for (i in 48 downTo 0) {
            val recordTime = now - (i * oneHourMs)
            
            // Generate a natural-looking cycle:
            // Let's create a 20-hour cycle (discharge for 16h, charge for 4h)
            val cycleHour = (i % 24)
            val isCharging = cycleHour in 0..4
            
            val level = if (isCharging) {
                // Charge from 15% to 90%
                15 + (cycleHour * 18)
            } else {
                // Discharge from 90% to 15%
                90 - ((cycleHour - 4) * 4)
            }

            // Bind values to fit nicely
            val cleanLevel = level.coerceIn(5, 100)
            
            val temperature = if (isCharging) {
                34.5f + (cycleHour * 0.8f) // warmer when charging
            } else {
                28.2f + ((cycleHour % 6) * 0.4f)
            }

            val voltage = 3.6f + (cleanLevel / 100f) * 0.6f // 3.6V to 4.2V
            
            val currentNow = if (isCharging) {
                3200 - (cycleHour * 200) // current tapers off as it fills
            } else {
                -120 - ((cycleHour % 5) * 80) // discharging mA
            }

            records.add(
                BatteryRecord(
                    timestamp = recordTime,
                    level = cleanLevel,
                    temperature = temperature.coerceIn(24f, 44f),
                    voltage = voltage.coerceIn(3.5f, 4.35f),
                    currentNow = currentNow,
                    isCharging = isCharging
                )
            )
        }

        // Insert all
        records.forEach { batteryDao.insertRecord(it) }

        // Generate beautiful charging logs
        val sessions = listOf(
            ChargingSession(
                startTime = now - TimeUnit.HOURS.toMillis(40),
                endTime = now - TimeUnit.HOURS.toMillis(38),
                startLevel = 15,
                endLevel = 90,
                maxCurrent = 4250,
                avgTemp = 36.8f,
                quality = "Optimum Fast Charger (45W)"
            ),
            ChargingSession(
                startTime = now - TimeUnit.HOURS.toMillis(18),
                endTime = now - TimeUnit.HOURS.toMillis(15),
                startLevel = 30,
                endLevel = 85,
                maxCurrent = 1800,
                avgTemp = 31.2f,
                quality = "Standard Desk Port (15W)"
            ),
            ChargingSession(
                startTime = now - TimeUnit.HOURS.toMillis(6),
                endTime = now - TimeUnit.HOURS.toMillis(5),
                startLevel = 10,
                endLevel = 65,
                maxCurrent = 850,
                avgTemp = 28.5f,
                quality = "Slow Laptop USB (5W)"
            )
        )
        sessions.forEach { batteryDao.insertSession(it) }
    }
}
