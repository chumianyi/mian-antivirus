package com.chumian.miansecurity.scan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import com.chumian.miansecurity.model.ScanResult
import com.chumian.miansecurity.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class ScanProgress(
    val currentFile: String,
    val filesScanned: Int,
    val totalFiles: Int,
    val threatsFound: Int,
    val isComplete: Boolean = false
)

object ScanEngine {
    private val SKIP_DIRS = setOf(
        "/proc", "/sys", "/dev", "/sbin", "/system",
        "/vendor", "/apex", "/data/dalvik-cache",
        "/data/resource-cache", "/cache"
    )

    private val SCAN_EXTENSIONS = setOf(
        ".apk", ".dex", ".so", ".jar", ".zip", ".rar",
        ".7z", ".exe", ".bat", ".sh", ".bin", ".dat",
        ".db", ".sqlite", ".xml", ".json", ".cfg",
        ".txt", ".log", ".html", ".js", ".py"
    )

    fun startScan(context: Context, scanType: String = "full"): Flow<ScanProgress> = flow {
        val results = mutableListOf<ScanResult>()
        var filesScanned = 0
        var threatsFound = 0

        val filesToScan = mutableListOf<File>()

        if (scanType == "apps" || scanType == "full") {
            scanInstalledApps(context, filesToScan)
        }

        if (scanType == "full" || scanType == "files") {
            scanStorage(filesToScan)
        }

        val totalFiles = filesToScan.size

        for (file in filesToScan) {
            emit(
                ScanProgress(
                    currentFile = file.absolutePath,
                    filesScanned = filesScanned,
                    totalFiles = totalFiles,
                    threatsFound = threatsFound
                )
            )

            val threats = scanFile(context, file)
            if (threats.isNotEmpty()) {
                threatsFound += threats.size
                for (threat in threats) {
                    results.add(
                        ScanResult(
                            fileName = file.name,
                            filePath = file.absolutePath,
                            virusType = threat,
                            riskLevel = VirusDatabase.getRiskLevel(threat),
                            fileSize = file.length()
                        )
                    )
                }
            }
            filesScanned++
        }

        Prefs.lastScanTime = System.currentTimeMillis()
        Prefs.lastScanThreats = threatsFound

        emit(
            ScanProgress(
                currentFile = "",
                filesScanned = filesScanned,
                totalFiles = totalFiles,
                threatsFound = threatsFound,
                isComplete = true
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun scanInstalledApps(context: Context, filesToScan: MutableList<File>) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            for (pkg in packages) {
                if (!Prefs.scanSystemApps && isSystemPackage(pkg.packageName)) continue
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    val apkFile = File(appInfo.sourceDir)
                    if (apkFile.exists()) {
                        filesToScan.add(apkFile)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isSystemPackage(pkg: String): Boolean {
        return pkg.startsWith("com.android.") ||
                pkg.startsWith("android.") ||
                pkg.startsWith("com.google.android.") ||
                pkg == "android"
    }

    private fun scanStorage(filesToScan: MutableList<File>) {
        try {
            val storageDirs = listOf(
                Environment.getExternalStorageDirectory(),
                File("/sdcard"),
                File("/storage/emulated/0")
            ).distinct()

            for (dir in storageDirs) {
                if (dir.exists() && dir.isDirectory) {
                    walkDirectory(dir, filesToScan, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun walkDirectory(dir: File, filesToScan: MutableList<File>, depth: Int) {
        if (depth > 10) return
        if (SKIP_DIRS.any { dir.absolutePath.startsWith(it) }) return

        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    if (!file.name.startsWith(".")) {
                        walkDirectory(file, filesToScan, depth + 1)
                    }
                } else {
                    val ext = file.extension.lowercase()
                    if (SCAN_EXTENSIONS.contains(".$ext") || file.length() < 10 * 1024 * 1024) {
                        if (file.length() < 100 * 1024 * 1024) {
                            filesToScan.add(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun scanFile(context: Context, file: File): List<String> {
        val threats = mutableListOf<String>()
        try {
            if (file.name.endsWith(".apk")) {
                threats.addAll(scanApkFile(context, file))
            } else {
                threats.addAll(VirusDatabase.scanFileContent(file))
            }
        } catch (e: Exception) {
            // Ignore
        }
        return threats
    }

    private fun scanApkFile(context: Context, file: File): List<String> {
        val threats = mutableListOf<String>()
        try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS)
            if (pkgInfo != null) {
                val permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
                threats.addAll(VirusDatabase.scanPermissions(permissions))
            }
        } catch (e: Exception) {
            // Ignore
        }
        return threats
    }

    fun deleteThreat(result: ScanResult): Boolean {
        return try {
            val file = File(result.filePath)
            if (file.exists()) {
                file.delete()
            } else true
        } catch (e: Exception) {
            false
        }
    }

    fun quarantineThreat(context: Context, result: ScanResult): Boolean {
        return try {
            val quarantineDir = File(context.filesDir, "quarantine")
            if (!quarantineDir.exists()) quarantineDir.mkdirs()
            val file = File(result.filePath)
            if (file.exists()) {
                val dest = File(quarantineDir, file.name + ".quarantine")
                file.copyTo(dest, overwrite = true)
                file.delete()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
