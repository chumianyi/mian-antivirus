package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityScanBinding
import com.chumian.miansecurity.model.ScanResult
import com.chumian.miansecurity.scan.ScanEngine
import com.chumian.miansecurity.scan.VirusDatabase
import com.chumian.miansecurity.util.FormatUtil
import com.chumian.miansecurity.util.Prefs
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private val results = mutableListOf<ScanResult>()
    private lateinit var adapter: ScanResultAdapter
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        updateVirusDbInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.scan)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ScanResultAdapter(results) { position, isChecked ->
            results[position] = results[position].copy(isSelected = isChecked)
            updateDeleteButtons()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnStartScan.setOnClickListener {
            if (!isScanning) {
                startScan()
            }
        }

        binding.btnUpdateDb.setOnClickListener {
            updateVirusDatabase()
        }

        binding.btnDeleteSelected.setOnClickListener {
            deleteSelected()
        }

        binding.btnDeleteAll.setOnClickListener {
            deleteAll()
        }

        binding.btnSelectAll.setOnClickListener {
            selectAll()
        }
    }

    private fun updateVirusDbInfo() {
        binding.tvDbVersion.text = "病毒库版本：${VirusDatabase.getDatabaseVersion()}"
        binding.tvSignatureCount.text = "特征码数量：${VirusDatabase.getSignatureCount()}"
        binding.tvLastUpdate.text = "上次更新：${if (Prefs.virusDbLastUpdate > 0) FormatUtil.formatTime(Prefs.virusDbLastUpdate) else "从未"}"
    }

    private fun updateVirusDatabase() {
        binding.btnUpdateDb.isEnabled = false
        binding.btnUpdateDb.text = getString(R.string.virus_db_downloading)
        lifecycleScope.launch {
            val success = VirusDatabase.updateDatabase(this@ScanActivity)
            binding.btnUpdateDb.isEnabled = true
            binding.btnUpdateDb.text = getString(R.string.virus_db_update)
            if (success) {
                updateVirusDbInfo()
                Toast.makeText(this@ScanActivity, R.string.virus_db_updated, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ScanActivity, R.string.virus_db_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScan() {
        isScanning = true
        results.clear()
        adapter.notifyDataSetChanged()
        binding.progressLayout.visibility = View.VISIBLE
        binding.resultLayout.visibility = View.GONE
        binding.btnStartScan.isEnabled = false
        binding.btnStartScan.text = getString(R.string.scanning)

        lifecycleScope.launch {
            ScanEngine.startScan(this@ScanActivity, "full").collect { progress ->
                binding.progressBar.progress = if (progress.totalFiles > 0) {
                    (progress.filesScanned * 100 / progress.totalFiles)
                } else 0
                binding.tvProgress.text = "${binding.progressBar.progress}%"
                binding.tvCurrentFile.text = progress.currentFile
                binding.tvFilesScanned.text = "已扫描：${progress.filesScanned}/${progress.totalFiles}"
                binding.tvThreatsFound.text = "威胁：${progress.threatsFound}"

                if (progress.isComplete) {
                    isScanning = false
                    binding.btnStartScan.isEnabled = true
                    binding.btnStartScan.text = getString(R.string.start_scan)
                    binding.progressLayout.visibility = View.GONE
                    showResults(progress.threatsFound)
                }
            }
        }
    }

    private fun showResults(threatCount: Int) {
        binding.resultLayout.visibility = View.VISIBLE
        if (threatCount > 0) {
            binding.tvResultTitle.text = getString(R.string.threat_found)
            binding.tvResultTitle.setTextColor(getColor(R.color.danger))
            binding.tvResultDesc.text = getString(R.string.recommend_delete)
            binding.deleteButtonsLayout.visibility = View.VISIBLE
        } else {
            binding.tvResultTitle.text = getString(R.string.safe_device)
            binding.tvResultTitle.setTextColor(getColor(R.color.success))
            binding.tvResultDesc.text = getString(R.string.no_threat)
            binding.deleteButtonsLayout.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
        updateDeleteButtons()
    }

    private fun selectAll() {
        val allSelected = results.all { it.isSelected }
        for (i in results.indices) {
            results[i] = results[i].copy(isSelected = !allSelected)
        }
        adapter.notifyDataSetChanged()
        updateDeleteButtons()
        binding.btnSelectAll.text = if (allSelected) getString(R.string.select_all) else getString(R.string.deselect_all)
    }

    private fun updateDeleteButtons() {
        val selectedCount = results.count { it.isSelected }
        binding.btnDeleteSelected.isEnabled = selectedCount > 0
        binding.btnDeleteSelected.text = "${getString(R.string.delete_selected)}($selectedCount)"
        binding.btnDeleteAll.isEnabled = results.isNotEmpty()
    }

    private fun deleteSelected() {
        val selected = results.filter { it.isSelected }
        if (selected.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(R.string.confirm) { _, _ ->
                var deleted = 0
                for (result in selected) {
                    if (Prefs.quarantineThreats) {
                        if (ScanEngine.quarantineThreat(this, result)) deleted++
                    } else {
                        if (ScanEngine.deleteThreat(result)) deleted++
                    }
                }
                results.removeAll(selected.toSet())
                adapter.notifyDataSetChanged()
                updateDeleteButtons()
                Toast.makeText(this, "已处理 $deleted 个威胁", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteAll() {
        if (results.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(getString(R.string.delete_all_confirm))
            .setPositiveButton(R.string.confirm) { _, _ ->
                var deleted = 0
                for (result in results) {
                    if (Prefs.quarantineThreats) {
                        if (ScanEngine.quarantineThreat(this, result)) deleted++
                    } else {
                        if (ScanEngine.deleteThreat(result)) deleted++
                    }
                }
                results.clear()
                adapter.notifyDataSetChanged()
                updateDeleteButtons()
                Toast.makeText(this, "已处理 $deleted 个威胁", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
