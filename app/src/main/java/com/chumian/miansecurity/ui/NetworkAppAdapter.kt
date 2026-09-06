package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemNetworkAppBinding
import com.chumian.miansecurity.model.AppInfo

class NetworkAppAdapter(
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<NetworkAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNetworkAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNetworkAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        val status = if (app.isRunning) "运行中" else "未运行"
        holder.binding.tvNetworkInfo.text = "${app.packageName} | $status"
        try {
            holder.binding.ivIcon.setImageDrawable(app.icon)
        } catch (e: Exception) {
            holder.binding.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    override fun getItemCount() = apps.size
}
