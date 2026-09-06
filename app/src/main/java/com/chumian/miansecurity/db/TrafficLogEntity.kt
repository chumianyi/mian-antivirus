package com.chumian.miansecurity.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_log")
data class TrafficLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val uploadBytes: Long,
    val downloadBytes: Long,
    val date: String,
    val timestamp: Long
)
