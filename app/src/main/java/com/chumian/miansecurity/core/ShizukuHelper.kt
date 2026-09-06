package com.chumian.miansecurity.core

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuHelper {
    private const val REQUEST_CODE = 1001

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isGranted(): Boolean {
        return try {
            if (!isRunning()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun isReady(context: Context): Boolean {
        return isInstalled(context) && isRunning() && isGranted()
    }

    fun requestPermission() {
        try {
            if (!isRunning()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun runCommand(command: String): String {
        return try {
            if (!isGranted()) return ""
            val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }

    fun killProcess(packageName: String): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "am force-stop $packageName").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun killProcessByPid(pid: Int): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "kill -9 $pid").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "pm uninstall --user 0 $packageName")
                .redirectErrorStream(true).start()
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor()
            result.contains("Success")
        } catch (e: Exception) {
            false
        }
    }

    fun freezeApp(packageName: String): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "pm disable-user --user 0 $packageName").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun unfreezeApp(packageName: String): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "pm enable $packageName").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache(packageName: String): Boolean {
        return try {
            if (!isGranted()) return false
            val process = ProcessBuilder("sh", "-c", "pm trim-caches 999999999999").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
