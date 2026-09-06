package com.chumian.miansecurity.features.traffic

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.chumian.miansecurity.db.AppDatabase
import com.chumian.miansecurity.db.TrafficLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrafficInfo(
    val packageName: String,
    val appName: String,
    val uploadBytes: Long,
    val downloadBytes: Long,
    val totalBytes: Long
)

class TrafficMonitor(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayTraffic(): Flow<List<TrafficLogEntity>> {
        val today = dateFormat.format(Date())
        return db.trafficLogDao().getByDate(today)
    }

    fun getTrafficSince(since: Long): Flow<List<TrafficLogEntity>> {
        return db.trafficLogDao().getSince(since)
    }

    suspend fun getTodayTotal(): Long = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        db.trafficLogDao().getTotalByDate(today)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun collectCurrentTraffic(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val pm = context.packageManager
            val now = System.currentTimeMillis()
            val todayStart = now - 24 * 60 * 60 * 1000

            val apps = pm.getInstalledApplications(0)
            val today = dateFormat.format(Date())

            apps.forEach { appInfo ->
                try {
                    val uid = appInfo.uid
                    val stats = nsm.queryDetailsForUid(
                        ConnectivityManager.TYPE_WIFI,
                        null,
                        todayStart,
                        now,
                        uid
                    )
                    var upload = 0L
                    var download = 0L
                    val bucket = android.app.usage.NetworkStats.Bucket()
                    while (stats.hasNextBucket()) {
                        stats.getNextBucket(bucket)
                        upload += bucket.txBytes
                        download += bucket.rxBytes
                    }
                    stats.close()

                    if (upload > 0 || download > 0) {
                        db.trafficLogDao().insert(
                            TrafficLogEntity(
                                packageName = appInfo.packageName,
                                appName = pm.getApplicationLabel(appInfo).toString(),
                                uploadBytes = upload,
                                downloadBytes = download,
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

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return "无网络"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "未知"
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                else -> "未知"
            }
        } else {
            @Suppress("DEPRECATION")
            when (cm.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "移动数据"
                ConnectivityManager.TYPE_ETHERNET -> "以太网"
                else -> "未知"
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = minOf(digitGroups, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format("%.2f %s", value, units[index])
    }
}
