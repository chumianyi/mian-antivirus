package com.chumian.miansecurity.model

data class AppScanResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isDangerous: Boolean,
    val dangerousPermissions: List<String>,
    val allPermissions: List<String>,
    val installTime: Long,
    val sourceDir: String
)

data class ProcessInfo(
    val pid: Int,
    val processName: String,
    val packageName: String,
    val memorySize: Long,
    val isSystem: Boolean
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val latestVersionCode: Int,
    val changelog: String,
    val downloadUrl: String,
    val fileSize: Long,
    val forceUpdate: Boolean
)
