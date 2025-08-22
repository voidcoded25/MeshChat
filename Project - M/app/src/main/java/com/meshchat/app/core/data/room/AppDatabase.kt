package com.meshchat.app.core.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meshchat.app.core.data.ChatMessage

@Database(
    entities = [ChatMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(UUIDConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meshchat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
