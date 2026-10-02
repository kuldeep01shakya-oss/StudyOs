package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudyTask::class,
        StudySession::class,
        ChatMessageEntity::class,
        GeneratedVisualEntity::class,
        SubjectEntity::class,
        SubjectRecordEntity::class,
        MockTestResultEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): StudyTaskDao
    abstract fun sessionDao(): StudySessionDao
    abstract fun chatDao(): ChatMessageDao
    abstract fun visualDao(): VisualDao
    abstract fun subjectDao(): SubjectDao
    abstract fun subjectRecordDao(): SubjectRecordDao
    abstract fun mockTestDao(): MockTestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyos_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
