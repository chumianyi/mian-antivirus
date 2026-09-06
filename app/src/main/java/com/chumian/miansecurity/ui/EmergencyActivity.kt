package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityEmergencyBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.permission.PermissionHelper
import com.chumian.miansecurity.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyBinding
    private val killedProcesses = mutableListOf<String>()
    private lateinit var adapter: KilledProcessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.emergency_box)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = KilledProcessAdapter(killedProcesses)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnStartEmergency.setOnClickListener {
            showEmergencyConfirm()
        }
    }

    private fun showEmergencyConfirm() {
        if (!PermissionHelper.hasAccessibility(this) || !PermissionHelper.hasOverlay(this)) {
            Toast.makeText(this, "需要无障碍和悬浮窗权限", Toast.LENGTH_SHORT).show()
            startActivity(android.content.Intent(this, PermissionGuideActivity::class.java))
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.emergency_warning)
            .setPositiveButton(R.string.confirm) { _, _ ->
                startEmergency()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startEmergency() {
        killedProcesses.clear()
        adapter.notifyDataSetChanged()
        binding.progressLayout.visibility = View.VISIBLE
        binding.resultLayout.visibility = View.GONE
        binding.btnStartEmergency.isEnabled = false
        binding.btnStartEmergency.text = getString(R.string.emergency_running)

        lifecycleScope.launch {
            binding.tvStatus.text = getString(R.string.force_scan)
            // 快速扫描
            val scanResults = withContext(Dispatchers.IO) {
                try {
                    val apps = ProcessManager.getInstalledApps(this@EmergencyActivity)
                    apps.filter { !it.isSystemApp }.take(20)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            binding.tvStatus.text = getString(R.string.clear_background)
            val killed = withContext(Dispatchers.IO) {
                ProcessManager.killAllProcesses(this@EmergencyActivity)
            }

            killedProcesses.addAll(killed)
            adapter.notifyDataSetChanged()

            binding.progressLayout.visibility = View.GONE
            binding.resultLayout.visibility = View.VISIBLE
            binding.btnStartEmergency.isEnabled = true
            binding.btnStartEmergency.text = getString(R.string.start_emergency)
            binding.tvKilledCount.text = "已禁止 ${killedProcesses.size} 个进程"

            Toast.makeText(this@EmergencyActivity, R.string.emergency_complete, Toast.LENGTH_SHORT).show()
        }
    }
}
