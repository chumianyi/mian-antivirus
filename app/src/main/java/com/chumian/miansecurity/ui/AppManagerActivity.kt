package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityAppManagerBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppManagerBinding
    private val allApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter
    private var filterType = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadApps()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.app_manager)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(filteredApps) { app, action ->
            handleAppAction(app, action)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                ProcessManager.getInstalledApps(this@AppManagerActivity)
            }
            allApps.clear()
            allApps.addAll(apps)
            applyFilter()
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvCount.text = "共 ${filteredApps.size} 个应用"
        }
    }

    private fun applyFilter() {
        filteredApps.clear()
        filteredApps.addAll(when (filterType) {
            "user" -> allApps.filter { !it.isSystemApp }
            "system" -> allApps.filter { it.isSystemApp }
            "running" -> allApps.filter { it.isRunning }
            "frozen" -> allApps.filter { it.isFrozen }
            else -> allApps
        })
        adapter.notifyDataSetChanged()
        binding.tvCount.text = "共 ${filteredApps.size} 个应用"
    }

    private fun handleAppAction(app: AppInfo, action: String) {
        when (action) {
            "uninstall" -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.uninstall)
                    .setMessage("确定卸载 ${app.appName}？")
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        if (ProcessManager.uninstallApp(this, app.packageName)) {
                            Toast.makeText(this, "卸载成功", Toast.LENGTH_SHORT).show()
                            loadApps()
                        } else {
                            val intent = Intent(Intent.ACTION_DELETE)
                            intent.data = Uri.parse("package:${app.packageName}")
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            "freeze" -> {
                if (app.isFrozen) {
                    if (ProcessManager.unfreezeApp(this, app.packageName)) {
                        Toast.makeText(this, "已解冻", Toast.LENGTH_SHORT).show()
                        loadApps()
                    }
                } else {
                    if (ProcessManager.freezeApp(this, app.packageName)) {
                        Toast.makeText(this, "已冻结", Toast.LENGTH_SHORT).show()
                        loadApps()
                    } else {
                        Toast.makeText(this, "需要Shizuku或Root权限", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "info" -> {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${app.packageName}")
                startActivity(intent)
            }
            "permissions" -> {
                showAppPermissions(app)
            }
        }
    }

    private fun showAppPermissions(app: AppInfo) {
        val perms = app.permissions.joinToString("\n") { "• $it" }
        AlertDialog.Builder(this)
            .setTitle("${app.appName} 权限")
            .setMessage(if (perms.isEmpty()) "无权限" else perms)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_manager_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.lowercase() ?: ""
                filteredApps.clear()
                filteredApps.addAll(allApps.filter {
                    it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
                })
                adapter.notifyDataSetChanged()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        filterType = when (item.itemId) {
            R.id.filter_all -> "all"
            R.id.filter_user -> "user"
            R.id.filter_system -> "system"
            R.id.filter_running -> "running"
            R.id.filter_frozen -> "frozen"
            else -> return super.onOptionsItemSelected(item)
        }
        applyFilter()
        return true
    }
}
