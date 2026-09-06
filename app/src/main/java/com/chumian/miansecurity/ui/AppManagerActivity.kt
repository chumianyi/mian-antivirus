package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityAppManagerBinding
import com.chumian.miansecurity.model.AppScanResult
import com.chumian.miansecurity.ui.adapter.AppListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppManagerBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var apps: List<AppScanResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "应用管理"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadApps()
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val packages = pm.getInstalledPackages(android.content.pm.PackageManager.GET_PERMISSIONS)
                packages.map { pkg ->
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
                    } catch (e: Exception) { pkg.packageName }
                    val isSystem = try {
                        val ai = pm.getApplicationInfo(pkg.packageName, 0)
                        (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    } catch (e: Exception) { false }
                    AppScanResult(
                        packageName = pkg.packageName,
                        appName = appName,
                        versionName = pkg.versionName ?: "",
                        isSystemApp = isSystem,
                        isDangerous = false,
                        dangerousPermissions = emptyList(),
                        allPermissions = pkg.requestedPermissions?.toList() ?: emptyList(),
                        installTime = pkg.firstInstallTime,
                        sourceDir = ""
                    )
                }.sortedBy { it.appName }
            }
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvCount.text = "共 ${apps.size} 个应用"
            val adapter = AppListAdapter(
                onAppClick = { app -> showAppDetail(app) },
                onUninstall = { app -> showAppMenu(app) }
            )
            binding.recyclerView.adapter = adapter
            adapter.submitList(apps)
        }
    }

    private fun showAppDetail(app: AppScanResult) {
        val perms = app.allPermissions.joinToString("\n") { "• $it" }
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setMessage("包名：${app.packageName}\n版本：${app.versionName}\n系统应用：${if (app.isSystemApp) "是" else "否"}\n权限数：${app.allPermissions.size}\n\n权限列表：\n$perms")
            .setPositiveButton("应用信息") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${app.packageName}")
                startActivity(intent)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAppMenu(app: AppScanResult) {
        val options = arrayOf("卸载", if (isFrozen(app.packageName)) "解冻" else "冻结", "应用信息")
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> uninstallApp(app)
                    1 -> toggleFreeze(app)
                    2 -> {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun uninstallApp(app: AppScanResult) {
        AlertDialog.Builder(this)
            .setTitle("确认卸载")
            .setMessage("确定要卸载 ${app.appName} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                if (ShizukuHelper.uninstallApp(app.packageName)) {
                    Toast.makeText(this, "已卸载", Toast.LENGTH_SHORT).show()
                    loadApps()
                } else {
                    Toast.makeText(this, "卸载失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleFreeze(app: AppScanResult) {
        if (isFrozen(app.packageName)) {
            if (ShizukuHelper.unfreezeApp(app.packageName)) {
                Toast.makeText(this, "已解冻", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (ShizukuHelper.freezeApp(app.packageName)) {
                Toast.makeText(this, "已冻结", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isFrozen(pkg: String): Boolean {
        return try {
            val state = packageManager.getApplicationEnabledSetting(pkg)
            state != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
                    state != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } catch (e: Exception) { false }
    }
}
