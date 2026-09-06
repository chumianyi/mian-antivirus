package com.chumian.miansecurity.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanTime: Long,
    val totalApps: Int,
    val dangerApps: Int,
    val safeApps: Int,
    val durationMs: Long,
    val dangerPackageNames: String
)
