package com.chumian.miansecurity

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.chumian.miansecurity.core.Prefs
import com.google.android.material.color.DynamicColors

class MianApp : Application() {
    companion object {
        lateinit var instance: MianApp
            private set
        const val CHANNEL_PROTECT = "protect"
        const val CHANNEL_SCAN = "scan"
        const val CHANNEL_EMERGENCY = "emergency"
        const val CHANNEL_VOLUME = "volume"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        createNotificationChannels()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(CHANNEL_PROTECT, "实时守护", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_SCAN, "扫描通知", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_EMERGENCY, "急救箱", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_VOLUME, "音量保护", NotificationManager.IMPORTANCE_DEFAULT)
            ).forEach { nm.createNotificationChannel(it) }
        }
    }
}
