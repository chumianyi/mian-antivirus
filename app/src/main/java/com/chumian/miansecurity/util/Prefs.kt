package com.chumian.miansecurity.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("mian_security", Context.MODE_PRIVATE)
    }

    var theme: String
        get() = sp.getString("theme", "system") ?: "system"
        set(v) = sp.edit().putString("theme", v).apply()

    var currentMode: String
        get() = sp.getString("current_mode", "basic") ?: "basic"
        set(v) = sp.edit().putString("current_mode", v).apply()

    var rootModeEnabled: Boolean
        get() = sp.getBoolean("root_mode", false)
        set(v) = sp.edit().putBoolean("root_mode", v).apply()

    var protectEnabled: Boolean
        get() = sp.getBoolean("protect_enabled", false)
        set(v) = sp.edit().putBoolean("protect_enabled", v).apply()

    var protectMethodNotification: Boolean
        get() = sp.getBoolean("protect_method_notification", true)
        set(v) = sp.edit().putBoolean("protect_method_notification", v).apply()

    var protectMethodVolume: Boolean
        get() = sp.getBoolean("protect_method_volume", false)
        set(v) = sp.edit().putBoolean("protect_method_volume", v).apply()

    var protectMethodShake: Boolean
        get() = sp.getBoolean("protect_method_shake", false)
        set(v) = sp.edit().putBoolean("protect_method_shake", v).apply()

    var volumeProtectEnabled: Boolean
        get() = sp.getBoolean("volume_protect", false)
        set(v) = sp.edit().putBoolean("volume_protect", v).apply()

    var virusDbVersion: String
        get() = sp.getString("virus_db_version", "0") ?: "0"
        set(v) = sp.edit().putString("virus_db_version", v).apply()

    var virusDbLastUpdate: Long
        get() = sp.getLong("virus_db_update", 0)
        set(v) = sp.edit().putLong("virus_db_update", v).apply()

    var lastScanTime: Long
        get() = sp.getLong("last_scan", 0)
        set(v) = sp.edit().putLong("last_scan", v).apply()

    var lastScanThreats: Int
        get() = sp.getInt("last_scan_threats", 0)
        set(v) = sp.edit().putInt("last_scan_threats", v).apply()

    var autoStartProtect: Boolean
        get() = sp.getBoolean("auto_start_protect", false)
        set(v) = sp.edit().putBoolean("auto_start_protect", v).apply()

    var whitelist: Set<String>
        get() = sp.getStringSet("whitelist", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("whitelist", v).apply()

    var scanSystemApps: Boolean
        get() = sp.getBoolean("scan_system_apps", true)
        set(v) = sp.edit().putBoolean("scan_system_apps", v).apply()

    var scanArchiveFiles: Boolean
        get() = sp.getBoolean("scan_archive", true)
        set(v) = sp.edit().putBoolean("scan_archive", v).apply()

    var quarantineThreats: Boolean
        get() = sp.getBoolean("quarantine", true)
        set(v) = sp.edit().putBoolean("quarantine", v).apply()

    var notifyOnThreat: Boolean
        get() = sp.getBoolean("notify_threat", true)
        set(v) = sp.edit().putBoolean("notify_threat", v).apply()

    var vibrateOnTrigger: Boolean
        get() = sp.getBoolean("vibrate_trigger", true)
        set(v) = sp.edit().putBoolean("vibrate_trigger", v).apply()

    var ignoreSystemApps: Boolean
        get() = sp.getBoolean("ignore_system_apps", true)
        set(v) = sp.edit().putBoolean("ignore_system_apps", v).apply()

    var volumeViolationCount: Int
        get() = sp.getInt("volume_violations", 0)
        set(v) = sp.edit().putInt("volume_violations", v).apply()

    var lastVolumeViolation: String
        get() = sp.getString("last_volume_violation", "") ?: ""
        set(v) = sp.edit().putString("last_volume_violation", v).apply()

    fun addToWhitelist(pkg: String) {
        whitelist = whitelist + pkg
    }

    fun removeFromWhitelist(pkg: String) {
        whitelist = whitelist - pkg
    }

    fun isWhitelisted(pkg: String): Boolean = whitelist.contains(pkg)
}
