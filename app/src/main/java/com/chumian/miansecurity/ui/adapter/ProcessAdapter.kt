package com.chumian.miansecurity.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemProcessBinding
import com.chumian.miansecurity.model.ProcessInfo
import java.text.DecimalFormat

class ProcessAdapter(
    private val processes: List<ProcessInfo>,
    private val onKill: (ProcessInfo) -> Unit
) : RecyclerView.Adapter<ProcessAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProcessBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProcessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proc = processes[position]
        val systemTag = if (proc.isSystem) " [系统]" else ""
        holder.binding.tvProcessName.text = "${proc.processName}$systemTag"
        holder.binding.tvPid.text = "PID: ${proc.pid}"
        holder.binding.tvMemory.text = formatSize(proc.memorySize)
        holder.binding.btnKill.setOnClickListener { onKill(proc) }
    }

    override fun getItemCount() = processes.size

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = minOf(digitGroups, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }
}
