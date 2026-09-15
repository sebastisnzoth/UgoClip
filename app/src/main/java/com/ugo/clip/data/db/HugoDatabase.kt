package com.ugo.clip.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ugo.clip.data.model.CommandLogEntity
import com.ugo.clip.data.model.GeminiInteractionEntity
import com.ugo.clip.data.model.MemoryFactEntity
import com.ugo.clip.data.model.NoteEntity
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        MemoryFactEntity::class,
        CommandLogEntity::class,
        GeminiInteractionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HugoDatabase : RoomDatabase() {
    abstract fun hugoDao(): HugoDao

    companion object {
        @Volatile
        private var INSTANCE: HugoDatabase? = null

        fun getDatabase(context: Context): HugoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HugoDatabase::class.java,
                    "hugo_clip_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
