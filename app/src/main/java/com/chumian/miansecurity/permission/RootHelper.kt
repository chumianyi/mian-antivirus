package com.chumian.miansecurity.permission

import com.topjohnwu.superuser.Shell
import java.io.File

object RootHelper {
    init {
        try {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isRootAvailable(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    fun requestRoot(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    fun exec(command: String): Shell.Result {
        return Shell.cmd(command).exec()
    }

    fun execWithOutput(command: String): String {
        return try {
            val result = Shell.cmd(command).exec()
            result.out.joinToString("\n") + result.err.joinToString("\n")
        } catch (e: Exception) {
            ""
        }
    }

    fun killProcess(packageName: String): Boolean {
        return try {
            val result = Shell.cmd("am force-stop $packageName").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun killProcessByPid(pid: Int): Boolean {
        return try {
            val result = Shell.cmd("kill -9 $pid").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return try {
            val result = Shell.cmd("pm uninstall --user 0 $packageName").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun freezeApp(packageName: String): Boolean {
        return try {
            val result = Shell.cmd("pm disable-user --user 0 $packageName").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun unfreezeApp(packageName: String): Boolean {
        return try {
            val result = Shell.cmd("pm enable $packageName").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache(packageName: String): Boolean {
        return try {
            val result = Shell.cmd("pm trim-caches 999999999999").exec()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val result = Shell.cmd("rm -rf '$path'").exec()
            result.isSuccess
        } catch (e: Exception) {
            File(path).delete()
        }
    }

    fun listProcesses(): List<String> {
        return try {
            val result = Shell.cmd("ps -A").exec()
            result.out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getForegroundApp(): String {
        return try {
            val result = Shell.cmd("dumpsys activity activities | grep mResumedActivity").exec()
            val line = result.out.firstOrNull() ?: return ""
            val regex = Regex("([a-zA-Z0-9_.]+)/")
            regex.find(line)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
