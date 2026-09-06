package com.chumian.miansecurity.model

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val changelog: String = "",
    val downloadUrl: String = "",
    val fileSize: Long = 0,
    val forceUpdate: Boolean = false
)

data class ScanResult(
    val fileName: String,
    val filePath: String,
    val virusType: String,
    val riskLevel: String,
    val fileSize: Long,
    val isSelected: Boolean = false
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val isSystemApp: Boolean,
    val isFrozen: Boolean,
    val isRunning: Boolean,
    val appSize: Long,
    val dataSize: Long,
    val cacheSize: Long,
    val installTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val permissions: List<String>,
    val icon: android.graphics.drawable.Drawable? = null
)

data class ProcessInfo(
    val pid: Int,
    val processName: String,
    val packageName: String,
    val memorySize: Long,
    val cpuUsage: Float,
    val isSystem: Boolean,
    val isSelected: Boolean = false
)

data class ThreatInfo(
    val name: String,
    val type: String,
    val risk: String,
    val description: String,
    val signatures: List<String>
)
