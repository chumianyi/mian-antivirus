package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.core.AppScanner
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.FragmentScanBinding
import com.chumian.miansecurity.model.AppScanResult
import com.chumian.miansecurity.ui.adapter.AppListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main)
    private var scanResults: List<AppScanResult> = emptyList()
    private lateinit var adapter: AppListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        binding.btnStartScan.setOnClickListener { startScan() }
        binding.btnUninstallDanger.setOnClickListener { uninstallDangerApps() }
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onAppClick = { app -> showAppDetail(app) },
            onUninstall = { app -> uninstallApp(app) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun startScan() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvScanProgress.visibility = View.VISIBLE
        binding.btnStartScan.isEnabled = false
        binding.scanResultLayout.visibility = View.GONE

        scope.launch {
            var dangerCount = 0
            var total = 0
            AppScanner.scanApps(requireContext()).collect { value ->
                when (value) {
                    is AppScanner.ScanProgress -> {
                        total = value.total
                        dangerCount = value.dangerCount
                        binding.tvScanProgress.text = "正在扫描：${value.currentApp} (${value.scanned}/${value.total})"
                        binding.progressBar.progress = if (value.total > 0) (value.scanned * 100 / value.total) else 0
                    }
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        scanResults = value as List<AppScanResult>
                        showResults(scanResults)
                    }
                }
            }
        }
    }

    private fun showResults(results: List<AppScanResult>) {
        binding.progressBar.visibility = View.GONE
        binding.tvScanProgress.visibility = View.GONE
        binding.btnStartScan.isEnabled = true
        binding.scanResultLayout.visibility = View.VISIBLE

        val dangerApps = results.filter { it.isDangerous }
        val safeApps = results.filter { !it.isDangerous }

        binding.tvDangerCount.text = dangerApps.size.toString()
        binding.tvSafeCount.text = safeApps.size.toString()
        binding.tvTotalCount.text = results.size.toString()

        // 按危险程度排序，危险的在前
        val sorted = dangerApps + safeApps
        adapter.submitList(sorted)

        if (dangerApps.isNotEmpty()) {
            binding.btnUninstallDanger.visibility = View.VISIBLE
            binding.tvResultTitle.text = "发现 ${dangerApps.size} 个风险应用"
            binding.tvResultTitle.setTextColor(resources.getColor(R.color.danger, null))
        } else {
            binding.btnUninstallDanger.visibility = View.GONE
            binding.tvResultTitle.text = "所有应用均安全"
            binding.tvResultTitle.setTextColor(resources.getColor(R.color.success, null))
        }
    }

    private fun showAppDetail(app: AppScanResult) {
        val perms = app.dangerousPermissions.joinToString("\n") { "• ${AppScanner.getDangerousPermissionLabel(it)} ($it)" }
        val message = buildString {
            append("包名：${app.packageName}\n")
            append("版本：${app.versionName}\n")
            append("系统应用：${if (app.isSystemApp) "是" else "否"}\n")
            append("总权限数：${app.allPermissions.size}\n")
            if (app.dangerousPermissions.isNotEmpty()) {
                append("\n危险权限：\n$perms")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(app.appName)
            .setMessage(message)
            .setPositiveButton("卸载") { _, _ -> uninstallApp(app) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun uninstallApp(app: AppScanResult) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认卸载")
            .setMessage("确定要卸载 ${app.appName} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                if (ShizukuHelper.isGranted()) {
                    val success = ShizukuHelper.uninstallApp(app.packageName)
                    if (success) {
                        Toast.makeText(requireContext(), "已卸载 ${app.appName}", Toast.LENGTH_SHORT).show()
                        scanResults = scanResults.filter { it.packageName != app.packageName }
                        adapter.submitList(scanResults)
                    } else {
                        Toast.makeText(requireContext(), "卸载失败，请手动卸载", Toast.LENGTH_SHORT).show()
                        openAppSettings(app.packageName)
                    }
                } else {
                    openAppSettings(app.packageName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun uninstallDangerApps() {
        val dangerApps = scanResults.filter { it.isDangerous }
        if (dangerApps.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("一键卸载风险应用")
            .setMessage("将卸载 ${dangerApps.size} 个风险应用，确定继续吗？")
            .setPositiveButton("全部卸载") { _, _ ->
                var successCount = 0
                for (app in dangerApps) {
                    if (ShizukuHelper.uninstallApp(app.packageName)) {
                        successCount++
                    }
                }
                Toast.makeText(requireContext(), "成功卸载 $successCount/${dangerApps.size} 个应用", Toast.LENGTH_LONG).show()
                startScan()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openAppSettings(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开应用设置", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
