package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayHeatmap(
    val dateString: String,
    val dayLabel: String, // "Mon", "Tue", etc.
    val minutes: Int,
    val intensity: Int // 0 (none), 1 (light), 2 (medium), 3 (high)
)

data class StudyStats(
    val streakDays: Int,
    val todayMinutes: Int,
    val lifetimeMinutes: Int,
    val completedTasksCount: Int,
    val weeklyHeatmap: List<DayHeatmap>
)

class StudyRepository(private val database: AppDatabase) {
    private val taskDao = database.taskDao()
    private val sessionDao = database.sessionDao()
    private val chatDao = database.chatDao()
    private val visualDao = database.visualDao()
    private val subjectDao = database.subjectDao()
    private val subjectRecordDao = database.subjectRecordDao()
    private val mockTestDao = database.mockTestDao()

    val allTasks: Flow<List<StudyTask>> = taskDao.getAllTasks()
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    val allChatMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allVisuals: Flow<List<GeneratedVisualEntity>> = visualDao.getAllVisuals()
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    val allRecords: Flow<List<SubjectRecordEntity>> = subjectRecordDao.getAllRecords()
    val allMockTestResults: Flow<List<MockTestResultEntity>> = mockTestDao.getAllTestResults()
    val totalMinutes: Flow<Int?> = sessionDao.getTotalMinutes()

    suspend fun insertMockTestResult(result: MockTestResultEntity) {
        mockTestDao.insertTestResult(result)
    }

    suspend fun deleteMockTestResult(id: Long) {
        mockTestDao.deleteTestResult(id)
    }

    fun getRecordsForSubject(subjectName: String): Flow<List<SubjectRecordEntity>> {
        return subjectRecordDao.getRecordsForSubject(subjectName)
    }

    suspend fun insertSubjectRecord(
        subjectName: String,
        title: String,
        lectureCount: Int = 1,
        category: String = "Lecture",
        notes: String = ""
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date())
        subjectRecordDao.insertRecord(
            SubjectRecordEntity(
                subjectName = subjectName.trim(),
                title = title.trim(),
                lectureCount = lectureCount,
                category = category,
                notes = notes.trim(),
                dateString = dateStr
            )
        )
    }

    suspend fun deleteSubjectRecord(id: Long) {
        subjectRecordDao.deleteRecordById(id)
    }

    suspend fun insertSubject(name: String, colorHex: String = "#06B6D4") {
        subjectDao.insertSubject(SubjectEntity(name = name.trim(), colorHex = colorHex, isCustom = true))
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        subjectDao.deleteSubject(subject)
    }

    suspend fun deleteSubjectByName(name: String) {
        subjectDao.deleteSubjectByName(name)
    }

    suspend fun getSubjectCount(): Int = subjectDao.getSubjectCount()

    suspend fun insertTask(task: StudyTask) = taskDao.insertTask(task)
    suspend fun updateTask(task: StudyTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: StudyTask) = taskDao.deleteTask(task)
    suspend fun clearCompletedTasks() = taskDao.clearCompletedTasks()
    suspend fun clearAllTasks() = taskDao.clearAllTasks()

    suspend fun logSession(
        durationMinutes: Int,
        mode: String,
        taskId: Long? = null,
        taskTitle: String? = null,
        subject: String? = null
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val session = StudySession(
            durationMinutes = durationMinutes,
            mode = mode,
            taskId = taskId,
            taskTitle = taskTitle,
            subject = subject,
            dateString = dateStr
        )
        sessionDao.insertSession(session)
    }

    suspend fun insertChatMessage(role: String, text: String) {
        chatDao.insertMessage(ChatMessageEntity(role = role, text = text))
    }

    suspend fun clearChat() = chatDao.clearHistory()

    suspend fun insertVisual(visual: GeneratedVisualEntity) = visualDao.insertVisual(visual)
    suspend fun deleteVisual(id: Long) = visualDao.deleteVisual(id)

    fun computeStats(sessions: List<StudySession>, tasks: List<StudyTask>): StudyStats {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val sessionsByDate = sessions.groupBy { it.dateString }
        val todayMins = sessionsByDate[todayStr]?.sumOf { it.durationMinutes } ?: 0
        val lifetimeMins = sessions.sumOf { it.durationMinutes }
        val completedCount = tasks.count { it.isCompleted }

        // Compute 7-day grid
        val calendar = Calendar.getInstance()
        val heatmap = mutableListOf<DayHeatmap>()
        val activeDatesSet = mutableSetOf<String>()

        // Collect past 7 days (ending today)
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val date = cal.time
            val dStr = sdf.format(date)
            val dLabel = dayFormat.format(date)
            val mins = sessionsByDate[dStr]?.sumOf { it.durationMinutes } ?: 0
            if (mins > 0) activeDatesSet.add(dStr)

            val intensity = when {
                mins == 0 -> 0
                mins < 25 -> 1
                mins < 60 -> 2
                else -> 3
            }
            heatmap.add(DayHeatmap(dateString = dStr, dayLabel = dLabel, minutes = mins, intensity = intensity))
        }

        // Streak calculation
        var streak = 0
        val checkCal = Calendar.getInstance()
        // If studied today, start from today, else start checking from yesterday
        val studiedToday = activeDatesSet.contains(todayStr)
        if (!studiedToday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dStr = sdf.format(checkCal.time)
            val hasSession = sessionsByDate.containsKey(dStr) && (sessionsByDate[dStr]?.sumOf { it.durationMinutes } ?: 0) > 0
            if (hasSession) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return StudyStats(
            streakDays = streak,
            todayMinutes = todayMins,
            lifetimeMinutes = lifetimeMins,
            completedTasksCount = completedCount,
            weeklyHeatmap = heatmap
        )
    }

    fun exportAsJson(tasks: List<StudyTask>, sessions: List<StudySession>): String {
        val root = JSONObject()
        val taskArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("subject", t.subject)
            obj.put("priority", t.priority)
            obj.put("reminderTime", t.reminderTime)
            obj.put("isCompleted", t.isCompleted)
            taskArray.put(obj)
        }
        root.put("tasks", taskArray)

        val sessionArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("durationMinutes", s.durationMinutes)
            obj.put("mode", s.mode)
            obj.put("taskTitle", s.taskTitle ?: "")
            obj.put("subject", s.subject ?: "")
            obj.put("date", s.dateString)
            sessionArray.put(obj)
        }
        root.put("sessions", sessionArray)
        root.put("exportTime", System.currentTimeMillis())
        return root.toString(2)
    }
}
