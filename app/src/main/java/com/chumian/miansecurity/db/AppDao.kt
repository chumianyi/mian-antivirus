package com.chumian.miansecurity.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfoEntity>)

    @Update
    suspend fun updateApp(app: AppInfoEntity)

    @Query("SELECT * FROM app_info ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE isSystem = 0 ORDER BY appName ASC")
    fun getUserApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE isLocked = 1")
    fun getLockedApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE riskLevel > 0 ORDER BY riskLevel DESC")
    fun getDangerApps(): Flow<List<AppInfoEntity>>

    @Query("UPDATE app_info SET isLocked = :locked WHERE packageName = :pkg")
    suspend fun setLocked(pkg: String, locked: Boolean)

    @Query("UPDATE app_info SET isFrozen = :frozen WHERE packageName = :pkg")
    suspend fun setFrozen(pkg: String, frozen: Boolean)

    @Query("DELETE FROM app_info WHERE packageName = :pkg")
    suspend fun deleteApp(pkg: String)

    @Query("SELECT COUNT(*) FROM app_info")
    suspend fun getAppCount(): Int
}

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(history: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY scanTime DESC LIMIT 50")
    fun getAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY scanTime DESC LIMIT 1")
    suspend fun getLatest(): ScanHistoryEntity?

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Dao
interface TrafficLogDao {
    @Insert
    suspend fun insert(log: TrafficLogEntity)

    @Insert
    suspend fun insertAll(logs: List<TrafficLogEntity>)

    @Query("SELECT * FROM traffic_log WHERE date = :date ORDER BY (uploadBytes + downloadBytes) DESC")
    fun getByDate(date: String): Flow<List<TrafficLogEntity>>

    @Query("SELECT * FROM traffic_log WHERE timestamp >= :since ORDER BY (uploadBytes + downloadBytes) DESC")
    fun getSince(since: Long): Flow<List<TrafficLogEntity>>

    @Query("SELECT SUM(uploadBytes + downloadBytes) FROM traffic_log WHERE date = :date")
    suspend fun getTotalByDate(date: String): Long
}

@Dao
interface BatteryLogDao {
    @Insert
    suspend fun insert(log: BatteryLogEntity)

    @Query("SELECT * FROM battery_log WHERE date = :date ORDER BY batteryPercent DESC")
    fun getByDate(date: String): Flow<List<BatteryLogEntity>>

    @Query("SELECT * FROM battery_log WHERE timestamp >= :since ORDER BY batteryPercent DESC")
    fun getSince(since: Long): Flow<List<BatteryLogEntity>>
}

@Dao
interface BehaviorLogDao {
    @Insert
    suspend fun insert(log: BehaviorLogEntity)

    @Query("SELECT * FROM behavior_log ORDER BY timestamp DESC LIMIT 200")
    fun getAll(): Flow<List<BehaviorLogEntity>>

    @Query("SELECT * FROM behavior_log WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun getByPackage(pkg: String): Flow<List<BehaviorLogEntity>>

    @Query("DELETE FROM behavior_log")
    suspend fun clearAll()
}
