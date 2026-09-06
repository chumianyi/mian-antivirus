package com.chumian.miansecurity.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.core.ProcessManager
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityProcessManagerBinding
import com.chumian.miansecurity.model.ProcessInfo
import com.chumian.miansecurity.ui.adapter.ProcessAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProcessManagerBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var processes: List<ProcessInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "进程管理"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.btnRefresh.setOnClickListener { loadProcesses() }
        binding.btnKillAll.setOnClickListener { killAll() }
        loadProcesses()
    }

    private fun loadProcesses() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            processes = withContext(Dispatchers.IO) {
                ProcessManager.getRunningProcesses(this@ProcessManagerActivity)
                    .sortedByDescending { it.memorySize }
            }
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvCount.text = "共 ${processes.size} 个运行进程"
            val adapter = ProcessAdapter(processes) { proc -> killProcess(proc) }
            binding.recyclerView.adapter = adapter
        }
    }

    private fun killProcess(proc: ProcessInfo) {
        if (ShizukuHelper.killProcessByPid(proc.pid) || ShizukuHelper.killProcess(proc.packageName)) {
            Toast.makeText(this, "已结束 ${proc.processName}", Toast.LENGTH_SHORT).show()
            loadProcesses()
        } else {
            ShizukuHelper.killProcess(proc.packageName)
            Toast.makeText(this, "已尝试结束 ${proc.processName}", Toast.LENGTH_SHORT).show()
            loadProcesses()
        }
    }

    private fun killAll() {
        scope.launch {
            val killed = withContext(Dispatchers.IO) {
                ProcessManager.killAllProcesses(this@ProcessManagerActivity)
            }
            Toast.makeText(this@ProcessManagerActivity, "已结束 ${killed.size} 个进程", Toast.LENGTH_SHORT).show()
            loadProcesses()
        }
    }
}
