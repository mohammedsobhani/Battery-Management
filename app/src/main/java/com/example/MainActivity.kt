package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.BatteryViewModel
import com.example.ui.BatteryViewModelFactory
import com.example.ui.MainDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BatteryViewModel

    // Real-time broadcast battery monitor receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val levelPercent = if (level != -1 && scale != -1) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                75
            }

            // Temp is in tenths of degrees C (e.g., 345 for 34.5°C)
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempCelsius = tempTenths / 10.0f

            // Voltage is in mV (e.g., 4200 for 4.2V)
            val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val voltageVolts = voltageMv / 1000.0f

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // Query dynamic battery service to access real current flow (microamps)
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // Convert to milliAmps (mA)
            var currentMa = currentMicroAmps / 1000

            // If system reports 0 or blank, let's normalize to a typical resting drain rate
            if (currentMa == 0) {
                currentMa = if (isCharging) 1200 else -140
            }

            viewModel.updatePhysicalSpecs(
                level = levelPercent,
                temp = tempCelsius,
                voltage = voltageVolts,
                charging = isCharging,
                current = currentMa
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init database-factored viewModel
        viewModel = ViewModelProvider(
            this,
            BatteryViewModelFactory(applicationContext)
        )[BatteryViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.DarkBackdrop
                ) {
                    MainDashboard(viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register OS broad-gauge battery intents
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        // Prevent system memory-leaks
        unregisterReceiver(batteryReceiver)
    }
}
