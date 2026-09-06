package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.core.AppScanner
import com.chumian.miansecurity.databinding.ActivityPermissionManagerBinding
import com.chumian.miansecurity.model.AppScanResult
import com.chumian.miansecurity.ui.adapter.AppListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionManagerBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var apps: List<AppScanResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "权限管理"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadPermissions()
    }

    private fun loadPermissions() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val packages = pm.getInstalledPackages(android.content.pm.PackageManager.GET_PERMISSIONS)
                packages.map { pkg ->
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
                    } catch (e: Exception) { pkg.packageName }
                    val allPerms = pkg.requestedPermissions?.toList() ?: emptyList()
                    val dangerousPerms = allPerms.filter {
                        it == "android.permission.BIND_ACCESSIBILITY_SERVICE" ||
                        it == "android.permission.SYSTEM_ALERT_WINDOW"
                    }
                    AppScanResult(
                        packageName = pkg.packageName,
                        appName = appName,
                        versionName = pkg.versionName ?: "",
                        isSystemApp = false,
                        isDangerous = dangerousPerms.isNotEmpty(),
                        dangerousPermissions = dangerousPerms,
                        allPermissions = allPerms,
                        installTime = pkg.firstInstallTime,
                        sourceDir = ""
                    )
                }.filter { it.allPermissions.isNotEmpty() }
                    .sortedByDescending { it.dangerousPermissions.size }
            }
            binding.progressBar.visibility = android.view.View.GONE
            val dangerCount = apps.count { it.isDangerous }
            binding.tvSummary.text = "共 ${apps.size} 个应用有权限，其中 $dangerCount 个申请了危险权限（无障碍/悬浮窗）"
            val adapter = AppListAdapter(
                onAppClick = { app -> showPermissions(app) },
                onUninstall = { app -> showPermissions(app) }
            )
            binding.recyclerView.adapter = adapter
            adapter.submitList(apps)
        }
    }

    private fun showPermissions(app: AppScanResult) {
        val dangerous = app.dangerousPermissions.joinToString("\n") { "⚠ ${AppScanner.getDangerousPermissionLabel(it)}" }
        val normal = app.allPermissions.filter { it !in app.dangerousPermissions }.joinToString("\n") { "• $it" }
        val message = buildString {
            append("危险权限（${app.dangerousPermissions.size}）：\n")
            append(if (dangerous.isEmpty()) "无\n" else "$dangerous\n")
            append("\n其他权限（${app.allPermissions.size - app.dangerousPermissions.size}）：\n")
            append(if (normal.isEmpty()) "无" else normal)
        }
        AlertDialog.Builder(this)
            .setTitle("${app.appName} 权限详情")
            .setMessage(message)
            .setPositiveButton("关闭", null)
            .show()
    }
}
