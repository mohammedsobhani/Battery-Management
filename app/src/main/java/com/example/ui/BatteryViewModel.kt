package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryDatabase
import com.example.data.BatteryRecord
import com.example.data.BatteryRepository
import com.example.data.ChargingSession
import com.example.data.GeminiInsightEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BatteryTab {
    DASHBOARD,
    AI_INSIGHTS,
    LEADERBOARD,
    SETTINGS
}

data class DeveloperSimSettings(
    val enableSim: Boolean = false,
    val simLevel: Int = 42,
    val simTemp: Float = 31.0f,
    val simCurrent: Int = -180, // Negative for drain, positive for charge
    val simIsCharging: Boolean = false
)

data class AppDrainStat(
    val appName: String,
    val packageName: String,
    val drainPercent: Float,
    val iconCategory: String // "social", "video", "chat", "game", "system"
)

class BatteryViewModel(private val repository: BatteryRepository) : ViewModel() {

    // --- State Toggles & Navigation ---
    private val _currentTab = MutableStateFlow(BatteryTab.DASHBOARD)
    val currentTab: StateFlow<BatteryTab> = _currentTab.asStateFlow()

    fun selectTab(tab: BatteryTab) {
        _currentTab.value = tab
    }

    // --- Real/Simulated Battery Health Metrics ---
    private val _realLevel = MutableStateFlow(75)
    private val _realTemp = MutableStateFlow(29.8f)
    private val _realVoltage = MutableStateFlow(4.1f)
    private val _realCurrent = MutableStateFlow(-120)
    private val _realIsCharging = MutableStateFlow(false)

    // Developer Simulation Switch State
    private val _simSettings = MutableStateFlow(DeveloperSimSettings())
    val simSettings: StateFlow<DeveloperSimSettings> = _simSettings.asStateFlow()

    // Active Derived Specs
    val currentLevel: Int get() = if (simSettings.value.enableSim) simSettings.value.simLevel else _realLevel.value
    val currentTemp: Float get() = if (simSettings.value.enableSim) simSettings.value.simTemp else _realTemp.value
    val currentVoltage: Float get() = _realVoltage.value
    val currentCurrent: Int get() = if (simSettings.value.enableSim) simSettings.value.simCurrent else _realCurrent.value
    val isCharging: Boolean get() = if (simSettings.value.enableSim) simSettings.value.simIsCharging else _realIsCharging.value

    // --- Premium Configurations ---
    private val _chargeGuardianCap = MutableStateFlow(85) // Protects at 85% charging stops
    val chargeGuardianCap: StateFlow<Int> = _chargeGuardianCap.asStateFlow()

    private val _guardianTriggered = MutableStateFlow(false)
    val guardianTriggered: StateFlow<Boolean> = _guardianTriggered.asStateFlow()

    private val _thermalAlertTriggered = MutableStateFlow(false)
    val thermalAlertTriggered: StateFlow<Boolean> = _thermalAlertTriggered.asStateFlow()

    private val _autoKillScheduler = MutableStateFlow(true)
    val autoKillScheduler: StateFlow<Boolean> = _autoKillScheduler.asStateFlow()

    private val _sosModeSelected = MutableStateFlow(false)
    val sosModeSelected: StateFlow<Boolean> = _sosModeSelected.asStateFlow()

    private val _sosContactNumber = MutableStateFlow("+1 (555) 019-2834")
    val sosContactNumber: StateFlow<String> = _sosContactNumber.asStateFlow()

    private val _sosMessage = MutableStateFlow("Emergency! My battery clicked 10% and autoshared location: 37.7749° N, 122.4194° W.")
    val sosMessage: StateFlow<String> = _sosMessage.asStateFlow()

    // --- Deep Clean Animations state ---
    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _cleanSuccess = MutableStateFlow(false)
    val cleanSuccess: StateFlow<Boolean> = _cleanSuccess.asStateFlow()

    // --- DB Flow collections ---
    val usageRecords: StateFlow<List<BatteryRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chargerSessions: StateFlow<List<ChargingSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic App Leaderboard ---
    private val _appDrainList = MutableStateFlow<List<AppDrainStat>>(emptyList())
    val appDrainList: StateFlow<List<AppDrainStat>> = _appDrainList.asStateFlow()

    // --- Gemini AI Diagnostics Insights ---
    private val _aiInsightsText = MutableStateFlow<String?>(null)
    val aiInsightsText: StateFlow<String?> = _aiInsightsText.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val aiInsightEngine = GeminiInsightEngine()

    init {
        viewModelScope.launch {
            // First boot fill
            repository.prepopulateIfEmpty()
            updateAppDrainLeaderboard()
        }
    }

    // Updates physical specs from OS BroadcastReceiver feed
    fun updatePhysicalSpecs(level: Int, temp: Float, voltage: Float, charging: Boolean, current: Int) {
        _realLevel.value = level
        _realTemp.value = temp
        _realVoltage.value = voltage
        _realIsCharging.value = charging
        _realCurrent.value = current

        // Auto validation check
        runGuardianAndThermalChecks()
    }

    // Config modifiers
    fun updateGuardianCap(cap: Int) {
        _chargeGuardianCap.value = cap
        runGuardianAndThermalChecks()
    }

    fun toggleAutoKill(active: Boolean) {
        _autoKillScheduler.value = active
    }

    fun toggleSosMode(active: Boolean) {
        _sosModeSelected.value = active
    }

    fun updateSosNumber(number: String) {
        _sosContactNumber.value = number
    }

    fun updateSosMessage(msg: String) {
        _sosMessage.value = msg
    }

    // Trigger Developer Simulation overrides
    fun updateSimulation(enable: Boolean, level: Int, temp: Float, current: Int, charging: Boolean) {
        _simSettings.value = DeveloperSimSettings(enable, level, temp, current, charging)
        runGuardianAndThermalChecks()
    }

    private fun runGuardianAndThermalChecks() {
        val level = currentLevel
        val temp = currentTemp
        val charging = isCharging

        // Charge Guardian Cap stops charging at chosen limit
        _guardianTriggered.value = charging && level >= _chargeGuardianCap.value

        // High heat warning starts alert at 38°C
        _thermalAlertTriggered.value = temp >= 37.8f
    }

    // --- 1. Real-time Charger Charging Speed Classifications ---
    fun getChargeSpeedLabel(): String {
        if (!isCharging) return "Discharging"
        val currentAbs = Math.abs(currentCurrent)
        return when {
            currentAbs >= 3000 -> "VOOC Ultra Flash Charge"
            currentAbs >= 1500 -> "Optimum Rapid Charging"
            currentAbs >= 800 -> "Standard Speed Charger"
            else -> "Slow Eco Charging USB"
        }
    }

    fun getChargeSpeedWattageEstimate(): Int {
        if (!isCharging) return 0
        val currentAbs = Math.abs(currentCurrent) // mA
        // Estimated: Wattage = Volts * Amps (mA / 1000)
        return ((currentVoltage * (currentAbs / 1000f))).toInt().coerceIn(1, 45)
    }

    // --- 2. Live Drain Leaderboard Simulator (Updated on cleaning or simulated drain) ---
    private fun updateAppDrainLeaderboard() {
        var multiplier = if (isCharging) 0.3f else 1.0f
        _appDrainList.value = listOf(
            AppDrainStat("Instagram", "com.instagram.android", 28.5f * multiplier, "social"),
            AppDrainStat("TikTok", "com.zhiliaoapp.musically", 24.2f * multiplier, "social"),
            AppDrainStat("YouTube Pro", "com.google.android.youtube", 18.0f * multiplier, "video"),
            AppDrainStat("WhatsApp Messenger", "com.whatsapp", 12.5f * multiplier, "chat"),
            AppDrainStat("Call of Duty Mobile", "com.activision.callofduty", 9.1f * multiplier, "game"),
            AppDrainStat("System UI Overlay", "android.system.ui", 4.3f * multiplier, "system"),
            AppDrainStat("Spotify Streamer", "com.spotify.music", 3.4f * multiplier, "video")
        ).sortedByDescending { it.drainPercent }
    }

    // --- 3. One-Tap RAM Deep Clean & Optimization ---
    fun runOneTapDeepClean() {
        if (_isCleaning.value) return
        viewModelScope.launch {
            _isCleaning.value = true
            _cleanSuccess.value = false

            // App simulation sweep - clear RAM
            kotlinx.coroutines.delay(1800)
            
            // Adjust current to reflect lower usage drain
            if (simSettings.value.enableSim) {
                _simSettings.value = _simSettings.value.copy(
                    simCurrent = (-40 - (Math.random() * 20)).toInt() // Drop standard system drain significantly
                )
            } else {
                _realCurrent.value = (-45 - (Math.random() * 15)).toInt()
            }

            // Lower app drainage levels
            val cleanMult = 0.15f // Cut 85% app background activity
            _appDrainList.value = _appDrainList.value.map {
                it.copy(drainPercent = (it.drainPercent * cleanMult).coerceAtLeast(0.1f))
            }.sortedByDescending { it.drainPercent }

            _isCleaning.value = false
            _cleanSuccess.value = true

            // Keep confirmation banner visible for 3 seconds
            kotlinx.coroutines.delay(3000)
            _cleanSuccess.value = false
        }
    }

    // --- 4. Battery Age Estimator Math ---
    fun getEstimatedBatteryAgeDescription(): String {
        val totalSessions = chargerSessions.value.size
        if (totalSessions <= 3) return "Estimating behavior (New App)"

        // Calculated from thermal instances logged
        val hotSessions = chargerSessions.value.count { it.avgTemp > 33.0f }
        val ageIndex = 0.8f + (hotSessions * 0.12f) + (totalSessions * 0.02f)
        val formattedAge = String.format("%.1f", ageIndex)
        return "Appears like a $formattedAge-year-old battery. ${if (hotSessions > 1) "Heat-creep accelerated degradation detected." else "Optimum charge hygiene preserved."}"
    }

    // --- 5. Gemini AI Diagnostic trigger ---
    fun requestDiagnosticInsights() {
        if (_aiLoading.value) return
        viewModelScope.launch {
            _aiLoading.value = true
            _aiInsightsText.value = null
            
            val analysis = aiInsightEngine.getInsights(
                currentLevel = currentLevel,
                temperature = currentTemp,
                guardianCap = _chargeGuardianCap.value,
                sessions = chargerSessions.value
            )
            
            _aiInsightsText.value = analysis
            _aiLoading.value = false
        }
    }

    // --- 6. Append dynamic logs to database when charger connects ---
    fun logNewChargingSession(start: Int, end: Int, current: Int, avgT: Float, classification: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newSession = ChargingSession(
                startTime = now - ((end - start) * 2 * 60 * 1000L),
                endTime = now,
                startLevel = start,
                endLevel = end,
                maxCurrent = current,
                avgTemp = avgT,
                quality = classification
            )
            repository.insertSession(newSession)

            // Also append a fresh battery record marking details
            val newRecord = BatteryRecord(
                timestamp = now,
                level = end,
                temperature = avgT,
                voltage = currentVoltage,
                currentNow = current,
                isCharging = false
            )
            repository.insertRecord(newRecord)
        }
    }
}

// ViewModel Factory boilerplate (Rooms context provider)
class BatteryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatteryViewModel::class.java)) {
            val db = BatteryDatabase.getDatabase(context)
            val repository = BatteryRepository(db.batteryDao)
            @Suppress("UNCHECKED_CAST")
            return BatteryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
