package com.chumian.miansecurity.features.battery

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.chumian.miansecurity.db.AppDatabase
import com.chumian.miansecurity.db.BatteryLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BatteryInfo(
    val packageName: String,
    val appName: String,
    val batteryPercent: Float,
    val foregroundTimeMs: Long,
    val wakeLockCount: Int
)

class BatteryMonitor(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale) else -1
    }

    fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getBatteryHealth(): String {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "未知故障"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }
    }

    fun getBatteryTechnology(): String {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "未知"
    }

    fun getBatteryTemperature(): Float {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return temp / 10.0f
    }

    fun getBatteryVoltage(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    }

    fun getTodayBatteryUsage(): Flow<List<BatteryLogEntity>> {
        val today = dateFormat.format(Date())
        return db.batteryLogDao().getByDate(today)
    }

    fun getBatteryUsageSince(since: Long): Flow<List<BatteryLogEntity>> {
        return db.batteryLogDao().getSince(since)
    }

    suspend fun collectBatteryStats(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager
            val now = System.currentTimeMillis()
            val startTime = now - TimeUnit.DAYS.toMillis(1)
            val today = dateFormat.format(Date())

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, now)
            stats?.forEach { usageStats ->
                try {
                    val appInfo = pm.getApplicationInfo(usageStats.packageName, 0)
                    val foregroundTime = usageStats.totalTimeInForeground
                    if (foregroundTime > 60000) {
                        val estimatedBattery = (foregroundTime / 3600000.0f * 5).coerceAtMost(50f)
                        db.batteryLogDao().insert(
                            BatteryLogEntity(
                                packageName = usageStats.packageName,
                                appName = pm.getApplicationLabel(appInfo).toString(),
                                batteryPercent = estimatedBattery,
                                foregroundTimeMs = foregroundTime,
                                wakeLockCount = 0,
                                date = today,
                                timestamp = now
                            )
                        )
                        count++
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        count
    }

    fun formatDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return if (hours > 0) "${hours}小时${minutes}分钟" else "${minutes}分钟"
    }
}
