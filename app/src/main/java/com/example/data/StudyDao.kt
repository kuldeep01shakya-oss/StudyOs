package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("DELETE FROM study_tasks WHERE isCompleted = 1")
    suspend fun clearCompletedTasks()

    @Query("DELETE FROM study_tasks")
    suspend fun clearAllTasks()
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Query("SELECT SUM(durationMinutes) FROM study_sessions")
    fun getTotalMinutes(): Flow<Int?>

    @Query("SELECT * FROM study_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<StudySession>>

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface VisualDao {
    @Query("SELECT * FROM generated_visuals ORDER BY timestamp DESC")
    fun getAllVisuals(): Flow<List<GeneratedVisualEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisual(visual: GeneratedVisualEntity): Long

    @Query("DELETE FROM generated_visuals WHERE id = :id")
    suspend fun deleteVisual(id: Long)

    @Query("DELETE FROM generated_visuals")
    suspend fun clearAllVisuals()
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE name = :name")
    suspend fun deleteSubjectByName(name: String)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int
}

@Dao
interface SubjectRecordDao {
    @Query("SELECT * FROM subject_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SubjectRecordEntity>>

    @Query("SELECT * FROM subject_records WHERE subjectName = :subjectName ORDER BY timestamp DESC")
    fun getRecordsForSubject(subjectName: String): Flow<List<SubjectRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SubjectRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: SubjectRecordEntity)

    @Query("DELETE FROM subject_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM subject_records WHERE subjectName = :subjectName")
    suspend fun deleteRecordsForSubject(subjectName: String)
}

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_test_results ORDER BY timestamp DESC")
    fun getAllTestResults(): Flow<List<MockTestResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResult(result: MockTestResultEntity): Long

    @Query("DELETE FROM mock_test_results WHERE id = :id")
    suspend fun deleteTestResult(id: Long)
}



