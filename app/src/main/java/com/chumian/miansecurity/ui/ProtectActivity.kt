package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.core.Prefs
import com.chumian.miansecurity.core.ProcessManager
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityProtectBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProtectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProtectBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProtectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "安全防护"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        updateUI()
        setupClicks()
    }

    private fun updateUI() {
        binding.switchProtect.isChecked = Prefs.protectEnabled
        binding.tvProtectStatus.text = if (Prefs.protectEnabled) "实时守护：运行中" else "实时守护：已关闭"

        when (Prefs.protectMethod) {
            "notification" -> binding.radioNotification.isChecked = true
            "volume" -> binding.radioVolume.isChecked = true
            "shake" -> binding.radioShake.isChecked = true
        }

        binding.switchVolumeProtect.isChecked = Prefs.volumeProtectEnabled
    }

    private fun setupClicks() {
        binding.switchProtect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("实时守护功能日常情况下不建议开启，可能会影响系统稳定性。确定开启吗？")
                    .setPositiveButton("确定开启") { _, _ ->
                        Prefs.protectEnabled = true
                        startProtectService()
                        updateUI()
                        Toast.makeText(this, "实时守护已开启", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        binding.switchProtect.isChecked = false
                    }
                    .show()
            } else {
                Prefs.protectEnabled = false
                stopProtectService()
                updateUI()
                Toast.makeText(this, "实时守护已关闭", Toast.LENGTH_SHORT).show()
            }
        }

        binding.radioGroupMethod.setOnCheckedChangeListener { _, checkedId ->
            Prefs.protectMethod = when (checkedId) {
                binding.radioVolume.id -> "volume"
                binding.radioShake.id -> "shake"
                else -> "notification"
            }
        }

        binding.switchVolumeProtect.setOnCheckedChangeListener { _, isChecked ->
            Prefs.volumeProtectEnabled = isChecked
            Toast.makeText(this, if (isChecked) "音量保护已开启" else "音量保护已关闭", Toast.LENGTH_SHORT).show()
        }

        binding.btnKillAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("禁止所有进程")
                .setMessage("将强制停止所有非系统进程，确定继续吗？")
                .setPositiveButton("确定") { _, _ ->
                    scope.launch {
                        val killed = ProcessManager.killAllProcesses(this@ProtectActivity)
                        Toast.makeText(this@ProtectActivity, "已禁止 ${killed.size} 个进程", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun startProtectService() {
        // 启动前台服务
        try {
            val intent = android.content.Intent(this, com.chumian.miansecurity.service.ProtectService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopProtectService() {
        try {
            stopService(android.content.Intent(this, com.chumian.miansecurity.service.ProtectService::class.java))
        } catch (e: Exception) {}
    }
}
