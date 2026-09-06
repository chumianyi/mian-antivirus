package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityProcessManagerBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.ProcessInfo
import com.chumian.miansecurity.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProcessManagerBinding
    private val processes = mutableListOf<ProcessInfo>()
    private lateinit var adapter: ProcessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        loadProcesses()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.process_manager)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ProcessAdapter(processes) { proc ->
            killProcess(proc)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnRefresh.setOnClickListener { loadProcesses() }
        binding.btnKillAll.setOnClickListener { killAll() }
    }

    private fun loadProcesses() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val procs = withContext(Dispatchers.IO) {
                ProcessManager.getRunningProcesses(this@ProcessManagerActivity)
            }
            processes.clear()
            processes.addAll(procs)
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = android.view.View.GONE
            val totalMem = processes.sumOf { it.memorySize }
            binding.tvSummary.text = "共 ${processes.size} 个进程，占用 ${FormatUtil.formatFileSize(totalMem)}"
        }
    }

    private fun killProcess(proc: ProcessInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.kill_process)
            .setMessage("确定结束进程 ${proc.processName}？")
            .setPositiveButton(R.string.confirm) { _, _ ->
                if (ProcessManager.forceStopProcess(this, proc.packageName)) {
                    Toast.makeText(this, "进程已结束", Toast.LENGTH_SHORT).show()
                    loadProcesses()
                } else {
                    Toast.makeText(this, "结束失败，需要更高权限", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun killAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage("确定结束所有非系统进程？")
            .setPositiveButton(R.string.confirm) { _, _ ->
                val killed = ProcessManager.killAllProcesses(this)
                Toast.makeText(this, "已结束 ${killed.size} 个进程", Toast.LENGTH_SHORT).show()
                loadProcesses()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
