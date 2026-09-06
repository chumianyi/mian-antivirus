package com.chumian.miansecurity.features.applock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chumian.miansecurity.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AppLockManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "app_lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isPasswordSet(): Boolean {
        return prefs.getString("password_hash", null) != null
    }

    fun setPassword(password: String): Boolean {
        return try {
            val hash = hashPassword(password)
            prefs.edit().putString("password_hash", hash).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString("password_hash", null) ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }

    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        return if (verifyPassword(oldPassword)) {
            setPassword(newPassword)
        } else {
            false
        }
    }

    fun removePassword(password: String): Boolean {
        return if (verifyPassword(password)) {
            prefs.edit().remove("password_hash").apply()
            true
        } else {
            false
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val salt = "mian_security_app_lock_salt_2026"
        val hash = md.digest((password + salt).toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun getLockedApps(): Flow<List<com.chumian.miansecurity.db.AppInfoEntity>> {
        return db.appDao().getLockedApps()
    }

    suspend fun lockApp(packageName: String) = withContext(Dispatchers.IO) {
        db.appDao().setLocked(packageName, true)
    }

    suspend fun unlockApp(packageName: String) = withContext(Dispatchers.IO) {
        db.appDao().setLocked(packageName, false)
    }

    suspend fun isAppLocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = db.appDao().getAllApps()
        false
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt("failed_attempts", 0)
    }

    fun incrementFailedAttempts() {
        val current = getFailedAttempts()
        prefs.edit().putInt("failed_attempts", current + 1).apply()
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt("failed_attempts", 0).apply()
    }

    fun isLockedOut(): Boolean {
        return getFailedAttempts() >= 5
    }

    fun getLockoutTime(): Long {
        return prefs.getLong("lockout_time", 0)
    }

    fun setLockoutTime(time: Long) {
        prefs.edit().putLong("lockout_time", time).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getLockType(): String {
        return prefs.getString("lock_type", "password") ?: "password"
    }

    fun setLockType(type: String) {
        prefs.edit().putString("lock_type", type).apply()
    }

    fun getAutoLockTimeout(): Long {
        return prefs.getLong("auto_lock_timeout", 30000)
    }

    fun setAutoLockTimeout(timeout: Long) {
        prefs.edit().putLong("auto_lock_timeout", timeout).apply()
    }

    fun isHideNotifications(): Boolean {
        return prefs.getBoolean("hide_notifications", true)
    }

    fun setHideNotifications(hide: Boolean) {
        prefs.edit().putBoolean("hide_notifications", hide).apply()
    }

    fun isPreventUninstall(): Boolean {
        return prefs.getBoolean("prevent_uninstall", false)
    }

    fun setPreventUninstall(prevent: Boolean) {
        prefs.edit().putBoolean("prevent_uninstall", prevent).apply()
    }

    fun isIntruderSelfie(): Boolean {
        return prefs.getBoolean("intruder_selfie", false)
    }

    fun setIntruderSelfie(enabled: Boolean) {
        prefs.edit().putBoolean("intruder_selfie", enabled).apply()
    }

    fun getIntruderPhotos(): Set<String> {
        return prefs.getStringSet("intruder_photos", emptySet()) ?: emptySet()
    }

    fun addIntruderPhoto(path: String) {
        val photos = getIntruderPhotos().toMutableSet()
        photos.add(path)
        prefs.edit().putStringSet("intruder_photos", photos).apply()
    }

    fun clearIntruderPhotos() {
        prefs.edit().remove("intruder_photos").apply()
    }
}
