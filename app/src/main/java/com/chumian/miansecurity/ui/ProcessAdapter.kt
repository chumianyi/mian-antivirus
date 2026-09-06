package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemProcessBinding
import com.chumian.miansecurity.model.ProcessInfo
import com.chumian.miansecurity.util.FormatUtil

class ProcessAdapter(
    private val processes: List<ProcessInfo>,
    private val onKill: (ProcessInfo) -> Unit
) : RecyclerView.Adapter<ProcessAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProcessBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProcessBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proc = processes[position]
        val systemTag = if (proc.isSystem) " [系统]" else ""
        holder.binding.tvProcessName.text = "${proc.processName}$systemTag"
        holder.binding.tvPid.text = "PID: ${proc.pid}"
        holder.binding.tvMemory.text = FormatUtil.formatFileSize(proc.memorySize)
        holder.binding.btnKill.setOnClickListener { onKill(proc) }
    }

    override fun getItemCount() = processes.size
}
