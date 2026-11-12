package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingEntity::class, MotionEventEntity::class, PairedCameraEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TwinGuardDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun motionEventDao(): MotionEventDao
    abstract fun pairedCameraDao(): PairedCameraDao

    companion object {
        @Volatile
        private var INSTANCE: TwinGuardDatabase? = null

        fun getDatabase(context: Context): TwinGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TwinGuardDatabase::class.java,
                    "twinguard_cctv.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
