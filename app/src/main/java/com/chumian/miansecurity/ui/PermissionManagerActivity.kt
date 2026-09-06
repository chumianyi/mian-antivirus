package com.chumian.miansecurity.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityPermissionManagerBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionManagerBinding
    private val dangerousApps = mutableListOf<AppInfo>()
    private lateinit var adapter: PermissionAppAdapter

    private val dangerousPermissions = listOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.DEVICE_POWER",
        "android.permission.INSTALL_PACKAGES",
        "android.permission.DELETE_PACKAGES"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadApps()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.permission_manager)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PermissionAppAdapter(dangerousApps)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                ProcessManager.getInstalledApps(this@PermissionManagerActivity)
            }
            dangerousApps.clear()
            dangerousApps.addAll(apps.filter { app ->
                app.permissions.any { perm ->
                    dangerousPermissions.any { it.equals(perm, ignoreCase = true) }
                } && !app.isSystemApp
            }.sortedByDescending { app ->
                app.permissions.count { perm ->
                    dangerousPermissions.any { it.equals(perm, ignoreCase = true) }
                }
            })
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvSummary.text = "发现 ${dangerousApps.size} 个应用使用危险权限"
        }
    }
}
