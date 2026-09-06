package com.chumian.miansecurity.features.notification

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import com.chumian.miansecurity.db.AppDatabase
import com.chumian.miansecurity.db.BehaviorLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationInfo(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val postTime: Long,
    val isOngoing: Boolean,
    val priority: Int,
    val category: String?
)

class NotificationManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_manager", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun isNotificationAccessEnabled(): Boolean {
        return try {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabledListeners?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    fun getActiveNotifications(): List<NotificationInfo> {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val activeNotifications = nm.activeNotifications
            val pm = context.packageManager
            activeNotifications.map { sbn ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }
                NotificationInfo(
                    id = sbn.id,
                    packageName = sbn.packageName,
                    appName = appName,
                    title = sbn.notification.extras.getString(Notification.EXTRA_TITLE, ""),
                    content = sbn.notification.extras.getString(Notification.EXTRA_TEXT, ""),
                    postTime = sbn.postTime,
                    isOngoing = sbn.isOngoing,
                    priority = sbn.notification.priority,
                    category = sbn.notification.category
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun cancelNotification(packageName: String, id: Int): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(packageName, id)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cancelAllFromPackage(packageName: String): Int {
        var count = 0
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.activeNotifications.forEach { sbn ->
                if (sbn.packageName == packageName) {
                    nm.cancel(sbn.packageName, sbn.id)
                    count++
                }
            }
        } catch (e: Exception) {
        }
        return count
    }

    fun getBlockedPackages(): Set<String> {
        return prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    }

    fun blockPackage(packageName: String) {
        val blocked = getBlockedPackages().toMutableSet()
        blocked.add(packageName)
        prefs.edit().putStringSet("blocked_packages", blocked).apply()
    }

    fun unblockPackage(packageName: String) {
        val blocked = getBlockedPackages().toMutableSet()
        blocked.remove(packageName)
        prefs.edit().putStringSet("blocked_packages", blocked).apply()
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return getBlockedPackages().contains(packageName)
    }

    fun getSpamKeywords(): Set<String> {
        return prefs.getStringSet("spam_keywords", defaultSpamKeywords()) ?: defaultSpamKeywords()
    }

    fun addSpamKeyword(keyword: String) {
        val keywords = getSpamKeywords().toMutableSet()
        keywords.add(keyword)
        prefs.edit().putStringSet("spam_keywords", keywords).apply()
    }

    fun removeSpamKeyword(keyword: String) {
        val keywords = getSpamKeywords().toMutableSet()
        keywords.remove(keyword)
        prefs.edit().putStringSet("spam_keywords", keywords).apply()
    }

    fun isSpamNotification(title: String, content: String): Boolean {
        val text = (title + " " + content).lowercase()
        return getSpamKeywords().any { keyword ->
            text.contains(keyword.lowercase())
        }
    }

    private fun defaultSpamKeywords(): Set<String> {
        return setOf(
            "中奖", "免费领取", "点击领取", "恭喜你", "限时优惠",
            "立即抢购", "红包", "提现", "赚钱", "兼职",
            "刷单", "贷款", "信用卡", "额度", "借款",
            "win", "free", "gift", "prize", "winner",
            "congratulations", "limited time", "act now", "click here"
        )
    }

    fun getNotificationHistory(): Flow<List<BehaviorLogEntity>> {
        return db.behaviorLogDao().getAll()
    }

    suspend fun logNotification(notification: NotificationInfo) = withContext(Dispatchers.IO) {
        db.behaviorLogDao().insert(
            BehaviorLogEntity(
                packageName = notification.packageName,
                behaviorType = "notification",
                description = "${notification.title}: ${notification.content}",
                riskLevel = if (isSpamNotification(notification.title, notification.content)) 5 else 1,
                timestamp = notification.postTime
            )
        )
    }

    fun isQuietHoursEnabled(): Boolean {
        return prefs.getBoolean("quiet_hours_enabled", false)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("quiet_hours_enabled", enabled).apply()
    }

    fun getQuietHoursStart(): String {
        return prefs.getString("quiet_hours_start", "22:00") ?: "22:00"
    }

    fun setQuietHoursStart(time: String) {
        prefs.edit().putString("quiet_hours_start", time).apply()
    }

    fun getQuietHoursEnd(): String {
        return prefs.getString("quiet_hours_end", "08:00") ?: "08:00"
    }

    fun setQuietHoursEnd(time: String) {
        prefs.edit().putString("quiet_hours_end", time).apply()
    }

    fun isInQuietHours(): Boolean {
        if (!isQuietHoursEnabled()) return false
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val start = getQuietHoursStart()
        val end = getQuietHoursEnd()
        return if (start < end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    fun formatTime(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun getNotificationCountByPackage(): Map<String, Int> {
        return getActiveNotifications().groupBy { it.packageName }.mapValues { it.value.size }
    }

    fun getSpamNotificationCount(): Int {
        return getActiveNotifications().count { isSpamNotification(it.title, it.content) }
    }
}
