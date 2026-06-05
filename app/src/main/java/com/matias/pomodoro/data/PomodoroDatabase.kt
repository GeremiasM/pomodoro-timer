package com.matias.pomodoro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PomodoroSession::class],
    version = 1,
    exportSchema = false
)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    companion object {
        @Volatile
        private var instance: PomodoroDatabase? = null

        fun getInstance(context: Context): PomodoroDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    "pomodoro.db"
                ).build().also { instance = it }
            }
        }
    }
}
