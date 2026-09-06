package com.chumian.miansecurity.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("mian_security", Context.MODE_PRIVATE)
    }

    var theme: String
        get() = sp.getString("theme", "system") ?: "system"
        set(value) {
            sp.edit().putString("theme", value).apply()
            when (value) {
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

    var protectEnabled: Boolean
        get() = sp.getBoolean("protect_enabled", false)
        set(value) = sp.edit().putBoolean("protect_enabled", value).apply()

    var protectMethod: String
        get() = sp.getString("protect_method", "notification") ?: "notification"
        set(value) = sp.edit().putString("protect_method", value).apply()

    var volumeProtectEnabled: Boolean
        get() = sp.getBoolean("volume_protect", false)
        set(value) = sp.edit().putBoolean("volume_protect", value).apply()

    var lastScanTime: Long
        get() = sp.getLong("last_scan_time", 0)
        set(value) = sp.edit().putLong("last_scan_time", value).apply()

    var lastScanDangerCount: Int
        get() = sp.getInt("last_scan_danger", 0)
        set(value) = sp.edit().putInt("last_scan_danger", value).apply()

    var whitelist: Set<String>
        get() = sp.getStringSet("whitelist", emptySet()) ?: emptySet()
        set(value) = sp.edit().putStringSet("whitelist", value).apply()

    fun isWhitelisted(pkg: String): Boolean = whitelist.contains(pkg)

    fun addToWhitelist(pkg: String) {
        whitelist = whitelist + pkg
    }

    fun removeFromWhitelist(pkg: String) {
        whitelist = whitelist - pkg
    }
}
