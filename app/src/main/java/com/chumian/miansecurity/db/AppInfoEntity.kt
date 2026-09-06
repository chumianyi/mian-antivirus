package com.chumian.miansecurity.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val isSystem: Boolean,
    val installTime: Long,
    val lastUpdateTime: Long,
    val apkSize: Long,
    val targetSdk: Int,
    val permissions: String,
    val isLocked: Boolean = false,
    val isFrozen: Boolean = false,
    val riskLevel: Int = 0
)
