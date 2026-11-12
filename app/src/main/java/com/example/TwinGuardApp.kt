package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.data.db.TwinGuardDatabase

class TwinGuardApp : Application() {

    companion object {
        lateinit var instance: TwinGuardApp
            private set
        var batteryPercent: Int = 100
        var isCharging: Boolean = false
        var batteryTemperature: Float = 28.0f
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100) / scale
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                batteryTemperature = temp / 10.0f
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize database
        TwinGuardDatabase.getDatabase(this)

        // Register battery monitor
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }
}
