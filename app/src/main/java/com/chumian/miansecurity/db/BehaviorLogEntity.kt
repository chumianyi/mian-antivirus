package com.chumian.miansecurity.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_log")
data class BehaviorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val behaviorType: String,
    val description: String,
    val riskLevel: Int,
    val timestamp: Long
)
