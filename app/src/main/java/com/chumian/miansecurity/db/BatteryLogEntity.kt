package com.chumian.miansecurity.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_log")
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val batteryPercent: Float,
    val foregroundTimeMs: Long,
    val wakeLockCount: Int,
    val date: String,
    val timestamp: Long
)
