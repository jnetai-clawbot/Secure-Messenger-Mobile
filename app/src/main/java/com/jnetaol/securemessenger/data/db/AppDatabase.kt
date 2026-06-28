package com.jnetaol.securemessenger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jnetaol.securemessenger.data.model.Contact
import com.jnetaol.securemessenger.data.model.Message
import com.jnetaol.securemessenger.logger.DebugLogger

@Database(entities = [Contact::class, Message::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                DebugLogger.d("AppDatabase", "getInstance", "SM-DB-001", "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_messenger_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                DebugLogger.i("AppDatabase", "getInstance", "SM-DB-002", "Database instance created")
                instance
            }
        }
    }
}
