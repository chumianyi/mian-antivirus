package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemPermissionAppBinding
import com.chumian.miansecurity.model.AppInfo

class PermissionAppAdapter(
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<PermissionAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPermissionAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName
        holder.binding.ivIcon.setImageDrawable(app.icon)

        val dangerousCount = app.permissions.count { perm ->
            listOf(
                "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CONTACTS",
                "READ_CALL_LOG", "RECORD_AUDIO", "CAMERA", "ACCESS_FINE_LOCATION",
                "SYSTEM_ALERT_WINDOW", "INSTALL_PACKAGES"
            ).any { perm.contains(it, ignoreCase = true) }
        }
        holder.binding.tvPermissions.text = "$dangerousCount 项危险权限 / 共 ${app.permissions.size} 项"

        holder.binding.root.setOnClickListener {
            val perms = app.permissions.joinToString("\n") { "• $it" }
            AlertDialog.Builder(holder.binding.root.context)
                .setTitle("${app.appName} 权限列表")
                .setMessage(if (perms.isEmpty()) "无权限" else perms)
                .setPositiveButton("应用信息") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${app.packageName}")
                    holder.binding.root.context.startActivity(intent)
                }
                .setNegativeButton("关闭", null)
                .show()
        }
    }

    override fun getItemCount() = apps.size
}
