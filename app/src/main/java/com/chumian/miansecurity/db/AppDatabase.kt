package com.chumian.miansecurity.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppInfoEntity::class,
        ScanHistoryEntity::class,
        TrafficLogEntity::class,
        BatteryLogEntity::class,
        BehaviorLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun trafficLogDao(): TrafficLogDao
    abstract fun batteryLogDao(): BatteryLogDao
    abstract fun behaviorLogDao(): BehaviorLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mian_security.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
