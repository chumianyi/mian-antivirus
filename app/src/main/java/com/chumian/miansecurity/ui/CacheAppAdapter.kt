package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemCacheAppBinding
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil

class CacheAppAdapter(
    private val apps: List<AppInfo>,
    private val selected: Set<String>,
    private val onCheckedChange: (String, Boolean) -> Unit
) : RecyclerView.Adapter<CacheAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCacheAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCacheAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvCacheSize.text = "${app.packageName} | 缓存: ${FormatUtil.formatFileSize(app.cacheSize)}"
        try {
            holder.binding.ivIcon.setImageDrawable(app.icon)
        } catch (e: Exception) {
            holder.binding.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        holder.binding.checkBox.isChecked = selected.contains(app.packageName)
        holder.binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(app.packageName, isChecked)
        }
    }

    override fun getItemCount() = apps.size
}
