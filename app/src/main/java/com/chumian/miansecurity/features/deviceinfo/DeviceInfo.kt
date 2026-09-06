package com.chumian.miansecurity.features.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceInfoItem(
    val category: String,
    val label: String,
    val value: String
)

object DeviceInfo {
    fun getAllInfo(context: Context): List<DeviceInfoItem> {
        val items = mutableListOf<DeviceInfoItem>()

        // 设备基本信息
        items.add(DeviceInfoItem("设备", "品牌", Build.BRAND))
        items.add(DeviceInfoItem("设备", "制造商", Build.MANUFACTURER))
        items.add(DeviceInfoItem("设备", "型号", Build.MODEL))
        items.add(DeviceInfoItem("设备", "设备名", Build.DEVICE))
        items.add(DeviceInfoItem("设备", "产品名", Build.PRODUCT))
        items.add(DeviceInfoItem("设备", "硬件", Build.HARDWARE))
        items.add(DeviceInfoItem("设备", "主板", Build.BOARD))
        items.add(DeviceInfoItem("设备", "Bootloader", Build.BOOTLOADER))
        items.add(DeviceInfoItem("设备", "指纹", Build.FINGERPRINT))

        // 系统信息
        items.add(DeviceInfoItem("系统", "Android版本", Build.VERSION.RELEASE))
        items.add(DeviceInfoItem("系统", "API等级", Build.VERSION.SDK_INT.toString()))
        items.add(DeviceInfoItem("系统", "安全补丁", Build.VERSION.SECURITY_PATCH ?: "未知"))
        items.add(DeviceInfoItem("系统", "构建号", Build.DISPLAY))
        items.add(DeviceInfoItem("系统", "构建类型", Build.TYPE))
        items.add(DeviceInfoItem("系统", "构建标签", Build.TAGS))
        items.add(DeviceInfoItem("系统", "内核版本", getKernelVersion()))
        items.add(DeviceInfoItem("系统", "基带版本", getBasebandVersion()))
        items.add(DeviceInfoItem("系统", "运行时间", getUptime()))

        // CPU信息
        items.add(DeviceInfoItem("CPU", "架构", Build.SUPPORTED_ABIS.joinToString(", ")))
        items.add(DeviceInfoItem("CPU", "核心数", Runtime.getRuntime().availableProcessors().toString()))
        items.add(DeviceInfoItem("CPU", "最大频率", getCpuMaxFreq()))
        items.add(DeviceInfoItem("CPU", "当前频率", getCpuCurrentFreq()))
        items.add(DeviceInfoItem("CPU", "处理器", Build.HARDWARE))

        // 内存信息
        val memInfo = getMemoryInfo(context)
        items.add(DeviceInfoItem("内存", "总内存", formatSize(memInfo.first)))
        items.add(DeviceInfoItem("内存", "可用内存", formatSize(memInfo.second)))
        items.add(DeviceInfoItem("内存", "已用内存", formatSize(memInfo.first - memInfo.second)))
        items.add(DeviceInfoItem("内存", "内存使用率", "${((memInfo.first - memInfo.second) * 100 / memInfo.first)}%"))

        // 存储信息
        val storageInfo = getStorageInfo()
        items.add(DeviceInfoItem("存储", "总存储", formatSize(storageInfo.first)))
        items.add(DeviceInfoItem("存储", "可用存储", formatSize(storageInfo.second)))
        items.add(DeviceInfoItem("存储", "已用存储", formatSize(storageInfo.first - storageInfo.second)))
        items.add(DeviceInfoItem("存储", "存储使用率", "${((storageInfo.first - storageInfo.second) * 100 / storageInfo.first)}%"))

        // 屏幕信息
        val displayInfo = getDisplayInfo(context)
        items.add(DeviceInfoItem("屏幕", "分辨率", "${displayInfo.first}x${displayInfo.second}"))
        items.add(DeviceInfoItem("屏幕", "屏幕密度", "${displayInfo.third}dpi"))
        items.add(DeviceInfoItem("屏幕", "密度等级", getDensityBucket(displayInfo.third)))
        items.add(DeviceInfoItem("屏幕", "刷新率", getRefreshRate(context)))

        // 电池信息
        val batteryInfo = getBatteryInfo(context)
        items.add(DeviceInfoItem("电池", "电量", "${batteryInfo.first}%"))
        items.add(DeviceInfoItem("电池", "充电状态", batteryInfo.second))
        items.add(DeviceInfoItem("电池", "健康状态", batteryInfo.third))
        items.add(DeviceInfoItem("电池", "技术", batteryInfo.fourth))
        items.add(DeviceInfoItem("电池", "电压", "${batteryInfo.fifth}V"))
        items.add(DeviceInfoItem("电池", "温度", "${batteryInfo.sixth}°C"))

        // 网络信息
        items.add(DeviceInfoItem("网络", "网络类型", getNetworkType(context)))
        items.add(DeviceInfoItem("网络", "WiFi状态", getWifiStatus(context)))
        items.add(DeviceInfoItem("网络", "蓝牙状态", getBluetoothStatus()))
        items.add(DeviceInfoItem("网络", "NFC状态", getNfcStatus(context)))
        items.add(DeviceInfoItem("网络", "GPS状态", getGpsStatus(context)))

        // 传感器信息
        items.add(DeviceInfoItem("传感器", "传感器数量", getSensorCount(context).toString()))
        items.add(DeviceInfoItem("传感器", "传感器列表", getSensorList(context)))

        return items
    }

    private fun getKernelVersion(): String {
        return try {
            File("/proc/version").readText().trim()
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getBasebandVersion(): String {
        return try {
            Build.getRadioVersion() ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getUptime(): String {
        return try {
            val uptime = android.os.SystemClock.elapsedRealtime()
            val hours = uptime / (1000 * 60 * 60)
            val minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60)
            "$hours 小时 $minutes 分钟"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getCpuMaxFreq(): String {
        return try {
            val freq = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toInt()
            "${freq / 1000} MHz"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getCpuCurrentFreq(): String {
        return try {
            val freq = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toInt()
            "${freq / 1000} MHz"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getMemoryInfo(context: Context): Pair<Long, Long> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            Pair(memInfo.totalMem, memInfo.availMem)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun getStorageInfo(): Pair<Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val available = stat.availableBytes
            Pair(total, available)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun getDisplayInfo(context: Context): Triple<Int, Int, Int> {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        } catch (e: Exception) {
            Triple(0, 0, 0)
        }
    }

    private fun getDensityBucket(dpi: Int): String = when {
        dpi <= 120 -> "ldpi"
        dpi <= 160 -> "mdpi"
        dpi <= 240 -> "hdpi"
        dpi <= 320 -> "xhdpi"
        dpi <= 480 -> "xxhdpi"
        dpi <= 640 -> "xxxhdpi"
        else -> "unknown"
    }

    private fun getRefreshRate(context: Context): String {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            "${wm.defaultDisplay.refreshRate} Hz"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getBatteryInfo(context: Context): SixTuple<Int, String, String, String, Float, Float> {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = when (batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知"
            }
            val health = "良好"
            val technology = "Li-ion"
            val voltage = 0f
            val temperature = 0f
            SixTuple(level, status, health, technology, voltage, temperature)
        } catch (e: Exception) {
            SixTuple(0, "未知", "未知", "未知", 0f, 0f)
        }
    }

    data class SixTuple<A, B, C, D, E, F>(
        val first: A, val second: B, val third: C,
        val fourth: D, val fifth: E, val sixth: F
    )

    private fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动数据"
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
                else -> "未连接"
            }
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getWifiStatus(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            if (wifiManager.isWifiEnabled) "已开启" else "已关闭"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getBluetoothStatus(): String {
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == true) "已开启" else "已关闭"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getNfcStatus(context: Context): String {
        return try {
            val nfcManager = context.getSystemService(Context.NFC_SERVICE) as android.nfc.NfcManager
            val nfcAdapter = nfcManager.defaultAdapter
            if (nfcAdapter?.isEnabled == true) "已开启" else "已关闭或不支持"
        } catch (e: Exception) {
            "不支持"
        }
    }

    private fun getGpsStatus(context: Context): String {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) "已开启" else "已关闭"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getSensorCount(context: Context): Int {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
            sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL).size
        } catch (e: Exception) {
            0
        }
    }

    private fun getSensorList(context: Context): String {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val sensors = sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)
            sensors.joinToString(", ") { it.name }
        } catch (e: Exception) {
            "未知"
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }

    fun getDeviceSummary(context: Context): String {
        return "${Build.BRAND} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${getMemoryInfo(context).first.let { formatSize(it) }} RAM"
    }
}
