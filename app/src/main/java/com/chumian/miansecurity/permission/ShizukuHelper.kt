package com.chumian.miansecurity.permission

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuHelper {
    private const val REQUEST_CODE = 1001

    private fun newProcess(cmd: Array<String>): Process {
        return Shizuku.newProcess(cmd, null)
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun isGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQUEST_CODE)
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun killProcess(packageName: String): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(arrayOf("sh", "-c", "am force-stop $packageName"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun killProcessByPid(pid: Int): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(arrayOf("sh", "-c", "kill -9 $pid"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(
                arrayOf("sh", "-c", "pm uninstall --user 0 $packageName")
            )
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor()
            result.contains("Success")
        } catch (e: Exception) {
            false
        }
    }

    fun freezeApp(packageName: String): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(
                arrayOf("sh", "-c", "pm disable-user --user 0 $packageName")
            )
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun unfreezeApp(packageName: String): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(
                arrayOf("sh", "-c", "pm enable $packageName")
            )
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache(packageName: String): Boolean {
        return try {
            if (!isAvailable() || !isGranted()) return false
            val process = newProcess(
                arrayOf("sh", "-c", "pm trim-caches 999999999999")
            )
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runCommand(command: String): String {
        return try {
            if (!isAvailable() || !isGranted()) return ""
            val process = newProcess(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            output + error
        } catch (e: Exception) {
            ""
        }
    }
}
