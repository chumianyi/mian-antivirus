package com.chumian.miansecurity.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ItemAppBinding
import com.chumian.miansecurity.model.AppScanResult

class AppListAdapter(
    private val onAppClick: (AppScanResult) -> Unit,
    private val onUninstall: (AppScanResult) -> Unit
) : ListAdapter<AppScanResult, AppListAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName

        if (app.isDangerous) {
            holder.binding.tvAppInfo.text = "⚠ 危险：${app.dangerousPermissions.joinToString(", ") { it.substringAfterLast(".") }}"
            holder.binding.tvAppInfo.setTextColor(holder.binding.root.context.getColor(R.color.danger))
        } else {
            holder.binding.tvAppInfo.text = if (app.isSystemApp) "✓ 安全（系统应用）" else "✓ 安全"
            holder.binding.tvAppInfo.setTextColor(holder.binding.root.context.getColor(R.color.success))
        }

        holder.binding.root.setOnClickListener { onAppClick(app) }
        holder.binding.ivMore.setOnClickListener { onUninstall(app) }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppScanResult>() {
        override fun areItemsTheSame(oldItem: AppScanResult, newItem: AppScanResult) =
            oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: AppScanResult, newItem: AppScanResult) =
            oldItem == newItem
    }
}
