package com.chumian.miansecurity.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemScanResultBinding
import com.chumian.miansecurity.model.ScanResult
import com.chumian.miansecurity.util.FormatUtil

class ScanResultAdapter(
    private val results: List<ScanResult>,
    private val onCheckedChange: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScanResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.binding.tvFileName.text = result.fileName
        holder.binding.tvFilePath.text = FormatUtil.truncatePath(result.filePath)
        holder.binding.tvVirusType.text = result.virusType
        holder.binding.tvRiskLevel.text = result.riskLevel
        holder.binding.tvRiskLevel.setTextColor(Color.parseColor(FormatUtil.getRiskColor(result.riskLevel)))
        holder.binding.tvFileSize.text = FormatUtil.formatFileSize(result.fileSize)
        holder.binding.checkbox.isChecked = result.isSelected
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(holder.adapterPosition, isChecked)
        }
    }

    override fun getItemCount() = results.size
}
