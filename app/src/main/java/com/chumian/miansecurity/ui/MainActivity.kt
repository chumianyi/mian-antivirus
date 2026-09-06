package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityMainBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.permission.PermissionHelper
import com.chumian.miansecurity.permission.RootHelper
import com.chumian.miansecurity.permission.ShizukuHelper
import com.chumian.miansecurity.service.ProtectService
import com.chumian.miansecurity.update.UpdateChecker
import com.chumian.miansecurity.util.FormatUtil
import com.chumian.miansecurity.util.Prefs
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnRequestPermissionResultListener {
    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        detectCurrentMode()
        checkUpdateOnStart()
        updateStatus()
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runOnUiThread {
                Toast.makeText(this, "Shizuku已授权，已切换到完整模式", Toast.LENGTH_LONG).show()
                Prefs.currentMode = "shizuku"
                Prefs.rootModeEnabled = false
                updateStatus()
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "Shizuku授权被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViews() {
        binding.tvAppName.text = getString(R.string.app_name)

        binding.btnStartScan.setOnClickListener {
            if (!PermissionHelper.hasManageStorage(this)) {
                PermissionHelper.requestManageStorage(this)
                Toast.makeText(this, R.string.grant_storage, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, ScanActivity::class.java))
        }

        binding.cardEmergency.setOnClickListener {
            if (!PermissionHelper.hasAccessibility(this) || !PermissionHelper.hasOverlay(this)) {
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("急救箱需要无障碍权限和悬浮窗权限才能正常工作。是否前往授权？")
                    .setPositiveButton("去授权") { _, _ ->
                        startActivity(Intent(this, PermissionGuideActivity::class.java))
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return@setOnClickListener
            }
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        binding.cardProtect.setOnClickListener {
            if (!PermissionHelper.hasAccessibility(this) || !PermissionHelper.hasOverlay(this)) {
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("安全防护需要无障碍权限和悬浮窗权限才能正常工作。是否前往授权？")
                    .setPositiveButton("去授权") { _, _ ->
                        startActivity(Intent(this, PermissionGuideActivity::class.java))
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return@setOnClickListener
            }
            startActivity(Intent(this, ProtectActivity::class.java))
        }

        binding.cardAppManager.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }

        binding.cardProcessManager.setOnClickListener {
            startActivity(Intent(this, ProcessManagerActivity::class.java))
        }

        binding.cardPermissionManager.setOnClickListener {
            startActivity(Intent(this, PermissionManagerActivity::class.java))
        }

        binding.cardCacheClean.setOnClickListener {
            startActivity(Intent(this, CacheCleanActivity::class.java))
        }

        binding.cardBattery.setOnClickListener {
            startActivity(Intent(this, BatteryActivity::class.java))
        }

        binding.cardNetwork.setOnClickListener {
            startActivity(Intent(this, NetworkMonitorActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnCheckUpdate.setOnClickListener {
            checkUpdate()
        }

        binding.tvMode.setOnClickListener {
            showModeSelector()
        }

        binding.switchProtect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showProtectWarning()
            } else {
                ProtectService.stop(this)
                updateStatus()
            }
        }
    }

    private fun detectCurrentMode() {
        // 按需授权：启动时不强制申请权限，只检测当前可用模式
        val hasShizuku = try {
            ShizukuHelper.isInstalled(this) && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()
        } catch (e: Exception) { false }

        val hasRoot = RootHelper.isRootAvailable()

        // 如果当前模式是basic但Shizuku已授权，自动升级
        if (Prefs.currentMode == "basic" && hasShizuku) {
            Prefs.currentMode = "shizuku"
        }
        // 如果当前模式是force但Shizuku已授权，自动升级到shizuku
        if (Prefs.currentMode == "force" && hasShizuku) {
            Prefs.currentMode = "shizuku"
        }
        // 如果当前模式是root但root不可用，降级
        if (Prefs.currentMode == "root" && !hasRoot) {
            Prefs.currentMode = if (hasShizuku) "shizuku" else "basic"
            Prefs.rootModeEnabled = false
        }
    }

    private fun checkUpdateOnStart() {
        binding.tvUpdateStatus.text = getString(R.string.checking_update)
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                UpdateChecker.checkUpdate(this@MainActivity)
            }
            if (info.hasUpdate) {
                binding.tvUpdateStatus.text = getString(R.string.update_available)
                binding.tvUpdateStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.warning))
                showUpdateDialog(info)
            } else {
                binding.tvUpdateStatus.text = getString(R.string.no_update)
                binding.tvUpdateStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success))
            }
        }
    }

    private fun checkUpdate() {
        binding.tvUpdateStatus.text = getString(R.string.checking_update)
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                UpdateChecker.checkUpdate(this@MainActivity)
            }
            if (info.hasUpdate) {
                binding.tvUpdateStatus.text = getString(R.string.update_available)
                showUpdateDialog(info)
            } else {
                binding.tvUpdateStatus.text = getString(R.string.no_update)
                Toast.makeText(this@MainActivity, R.string.no_update, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(info: com.chumian.miansecurity.model.UpdateInfo) {
        val message = buildString {
            append("最新版本：${info.latestVersion}\n\n")
            append("更新说明：\n${info.changelog}\n")
            if (info.fileSize > 0) {
                append("\n文件大小：${FormatUtil.formatFileSize(info.fileSize)}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.update_available)
            .setMessage(message)
            .setPositiveButton(R.string.update_now) { _, _ ->
                if (info.downloadUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "下载地址不可用", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.update_later, null)
            .setCancelable(!info.forceUpdate)
            .show()
    }

    private fun showModeSelector() {
        val modes = arrayOf(
            "基础模式（无障碍+悬浮窗）",
            "强杀模式（无障碍+悬浮窗+Shizuku）",
            "Shizuku完整模式",
            "顶配模式（Root）"
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.switch_mode)
            .setItems(modes) { _, which ->
                when (which) {
                    0 -> switchToBasicMode()
                    1 -> switchToForceMode()
                    2 -> switchToShizukuMode()
                    3 -> switchToRootMode()
                }
            }
            .show()
    }

    private fun switchToBasicMode() {
        if (!PermissionHelper.hasAccessibility(this) || !PermissionHelper.hasOverlay(this)) {
            Toast.makeText(this, "需要先授予无障碍和悬浮窗权限", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PermissionGuideActivity::class.java))
            return
        }
        Prefs.currentMode = "basic"
        Prefs.rootModeEnabled = false
        updateStatus()
        Toast.makeText(this, "已切换到基础模式", Toast.LENGTH_SHORT).show()
    }

    private fun switchToForceMode() {
        if (!PermissionHelper.hasAccessibility(this) || !PermissionHelper.hasOverlay(this)) {
            Toast.makeText(this, "需要先授予无障碍和悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShizukuHelper.isInstalled(this)) {
            Toast.makeText(this, "请先安装Shizuku", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShizukuHelper.isAvailable()) {
            Toast.makeText(this, "Shizuku未运行，请先启动", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShizukuHelper.isGranted()) {
            ShizukuHelper.requestPermission()
            return
        }
        Prefs.currentMode = "force"
        Prefs.rootModeEnabled = false
        updateStatus()
        Toast.makeText(this, "已切换到强杀模式", Toast.LENGTH_SHORT).show()
    }

    private fun switchToShizukuMode() {
        if (!ShizukuHelper.isInstalled(this)) {
            Toast.makeText(this, "请先安装Shizuku", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShizukuHelper.isAvailable() || !ShizukuHelper.isGranted()) {
            ShizukuHelper.requestPermission()
            return
        }
        Prefs.currentMode = "shizuku"
        Prefs.rootModeEnabled = false
        updateStatus()
        Toast.makeText(this, "已切换到Shizuku完整模式", Toast.LENGTH_SHORT).show()
    }

    private fun switchToRootMode() {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.root_mode_warning)
            .setPositiveButton(R.string.confirm) { _, _ ->
                if (!RootHelper.isRootAvailable()) {
                    Toast.makeText(this, "Root不可用", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Prefs.currentMode = "root"
                Prefs.rootModeEnabled = true
                updateStatus()
                Toast.makeText(this, "已切换到顶配模式(Root)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showProtectWarning() {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.protect_warning_detail)
            .setPositiveButton(R.string.confirm) { _, _ ->
                if (!PermissionHelper.hasAccessibility(this)) {
                    PermissionHelper.requestAccessibility(this)
                    binding.switchProtect.isChecked = false
                    return@setPositiveButton
                }
                ProtectService.start(this)
                updateStatus()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.switchProtect.isChecked = false
            }
            .show()
    }

    private fun updateStatus() {
        val mode = Prefs.currentMode
        binding.tvMode.text = PermissionHelper.getModeName(mode)
        binding.tvModeDesc.text = PermissionHelper.getModeDescription(mode)

        val protectRunning = Prefs.protectEnabled
        binding.switchProtect.isChecked = protectRunning
        binding.tvProtectStatus.text = if (protectRunning) {
            getString(R.string.protect_running)
        } else {
            getString(R.string.protect_stopped)
        }

        if (Prefs.lastScanTime > 0) {
            binding.tvLastScan.text = getString(R.string.last_scan_time, FormatUtil.formatTime(Prefs.lastScanTime))
        } else {
            binding.tvLastScan.text = getString(R.string.never_scanned)
        }

        binding.tvVirusDb.text = "病毒库：${if (Prefs.virusDbLastUpdate > 0) FormatUtil.formatTime(Prefs.virusDbLastUpdate) else "未更新"}"
    }

    override fun onStart() {
        super.onStart()
        try {
            Shizuku.addRequestPermissionResultListener(this)
        } catch (e: Exception) {
            // Shizuku not available
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            Shizuku.removeRequestPermissionResultListener(this)
        } catch (e: Exception) {
            // Shizuku not available
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次返回前台时检测Shizuku授权状态
        detectCurrentMode()
        updateStatus()
    }
}
