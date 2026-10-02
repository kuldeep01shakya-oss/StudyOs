package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String = "General", // Physics, Maths, Revision, Aptitude, General, Chemistry, etc.
    val priority: String = "medium", // high, medium, low
    val reminderTime: String = "", // e.g. "18:30"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val durationMinutes: Int,
    val mode: String = "pomodoro", // "pomodoro", "stopwatch"
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val subject: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String // "yyyy-MM-dd"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "model", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_visuals")
data class GeneratedVisualEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val size: String = "1K", // "1K", "2K", "4K"
    val base64Data: String? = null,
    val filePath: String? = null,
    val mimeType: String = "image/jpeg",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#06B6D4",
    val isCustom: Boolean = true
)

@Entity(tableName = "subject_records")
data class SubjectRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectName: String,
    val title: String, // e.g. "4 Lectures complete kiye" ya "Calculus Qs"
    val lectureCount: Int = 1,
    val category: String = "Lecture", // "Lecture", "Practice", "Revision", "Task Completed"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String // "yyyy-MM-dd"
)

@Entity(tableName = "mock_test_results")
data class MockTestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examName: String,
    val subjectName: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val score: Int,
    val totalPossibleMarks: Int,
    val timeTakenSeconds: Int,
    val weaknesses: String = "", // Comma-separated or JSON list of weak topics
    val strengths: String = "", // Comma-separated or JSON list of strong topics
    val sectionBreakdown: String = "", // Section-wise performance report
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MockQuestion(
    val id: Int,
    val question: String, // हिंदी में प्रश्न
    val options: List<String>, // [A, B, C, D]
    val correctIndex: Int, // 0 to 3
    val topic: String, // Topic name for strength/weakness analysis
    val explanation: String, // हिंदी में व्याख्या
    val section: String = "", // e.g. "तर्कशक्ति (Reasoning)", "सामान्य जागरूकता", "गणित (Quantitative Aptitude)", "अंग्रेजी"
    val positiveMarks: Float = 2.0f,
    val negativeMarks: Float = 0.5f
)




