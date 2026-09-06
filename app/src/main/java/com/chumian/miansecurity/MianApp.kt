package com.chumian.miansecurity

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.chumian.miansecurity.util.Prefs

class MianApp : Application() {
    companion object {
        lateinit var instance: MianApp
            private set

        const val CHANNEL_PROTECT = "protect"
        const val CHANNEL_SCAN = "scan"
        const val CHANNEL_UPDATE = "update"
        const val CHANNEL_EMERGENCY = "emergency"
        const val CHANNEL_VOLUME = "volume"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        createNotificationChannels()
        applyTheme()
        // Android 12+ 动态取色
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(CHANNEL_PROTECT, "实时守护", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "实时安全守护通知"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_SCAN, "扫描通知", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "病毒扫描进度通知"
                },
                NotificationChannel(CHANNEL_UPDATE, "更新通知", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "应用更新通知"
                },
                NotificationChannel(CHANNEL_EMERGENCY, "急救箱", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "急救箱运行通知"
                },
                NotificationChannel(CHANNEL_VOLUME, "音量保护", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "音量保护通知"
                }
            )
            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    private fun applyTheme() {
        when (Prefs.theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
