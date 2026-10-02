package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.data.GeneratedVisualEntity
import com.example.data.StudyRepository
import com.example.data.StudySession
import com.example.data.StudyStats
import com.example.data.StudyTask
import com.example.network.ChatTurn
import com.example.network.GeminiClient
import com.example.network.GeminiResult
import com.example.util.AlertManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    TASKS,
    FOCUS,
    STATS,
    AI_TUTOR,
    RECORDS
}

enum class MockTestStatus {
    IDLE,
    GENERATING,
    RUNNING,
    SUBMITTED
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = StudyRepository(database)
    private val geminiClient = GeminiClient(application)
    private val alertManager = AlertManager(application)

    init {
        viewModelScope.launch {
            // Seed default subjects if empty
            if (repository.getSubjectCount() == 0) {
                repository.insertSubject("Physics", "#06B6D4")
                repository.insertSubject("Maths", "#6366F1")
                repository.insertSubject("Revision", "#F59E0B")
                repository.insertSubject("Aptitude", "#F43F5E")
                repository.insertSubject("General", "#10B981")
                repository.insertSubject("Chemistry", "#A855F7")

                // Initial sample records
                repository.insertSubjectRecord(
                    subjectName = "Maths",
                    title = "Calculus & Limits (4 Lectures)",
                    lectureCount = 4,
                    category = "Lecture",
                    notes = "4 video lectures attend kiye aur practice questions solve kiye"
                )
                repository.insertSubjectRecord(
                    subjectName = "Physics",
                    title = "Ray Optics & Prism Derivation (2 Lectures)",
                    lectureCount = 2,
                    category = "Lecture",
                    notes = "2 lectures complete kiye aur notes banaye"
                )
            }

            // Seed sample targets if empty on first startup
            delay(200)
            if (allTasks.value.isEmpty()) {
                repository.insertTask(
                    StudyTask(
                        title = "Optics 20 Numericals ya Ray Diagrams",
                        subject = "Physics",
                        priority = "high",
                        reminderTime = "18:30"
                    )
                )
                repository.insertTask(
                    StudyTask(
                        title = "Trigonometry & Calculus Problems",
                        subject = "Maths",
                        priority = "medium",
                        reminderTime = "20:00"
                    )
                )
                repository.insertTask(
                    StudyTask(
                        title = "Chemical Bonding & High-Yield Formulas",
                        subject = "Revision",
                        priority = "high",
                        reminderTime = "21:30"
                    )
                )
                repository.insertTask(
                    StudyTask(
                        title = "Quantitative Aptitude Speed Test",
                        subject = "Aptitude",
                        priority = "low",
                        reminderTime = ""
                    )
                )
                // Seed an initial focus session to kickstart streak
                repository.logSession(
                    durationMinutes = 25,
                    mode = "pomodoro",
                    taskTitle = "Optics 20 Numericals ya Ray Diagrams",
                    subject = "Physics"
                )
            }
        }
    }

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.TASKS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // ---------------- SUBJECTS ----------------
    val allSubjects: StateFlow<List<com.example.data.SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String, colorHex: String = "#06B6D4") {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                val exists = allSubjects.value.any { it.name.equals(trimmed, ignoreCase = true) }
                if (!exists) {
                    repository.insertSubject(trimmed, colorHex)
                }
            }
        }
    }

    fun deleteSubject(subject: com.example.data.SubjectEntity) {
        viewModelScope.launch {
            if (_selectedFilter.value.equals(subject.name, ignoreCase = true)) {
                _selectedFilter.value = "all"
            }
            repository.deleteSubject(subject)
        }
    }

    // ---------------- SUBJECT STUDY RECORDS ----------------
    val allRecords: StateFlow<List<com.example.data.SubjectRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubjectRecord(
        subjectName: String,
        title: String,
        lectureCount: Int = 1,
        category: String = "Lecture",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertSubjectRecord(
                subjectName = subjectName,
                title = title,
                lectureCount = lectureCount,
                category = category,
                notes = notes
            )
        }
    }

    fun deleteSubjectRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteSubjectRecord(id)
        }
    }

    // ---------------- TASKS ----------------
    val allTasks: StateFlow<List<StudyTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow("all")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    private val _editingTask = MutableStateFlow<StudyTask?>(null)
    val editingTask: StateFlow<StudyTask?> = _editingTask.asStateFlow()

    private val _showTaskDialog = MutableStateFlow(false)
    val showTaskDialog: StateFlow<Boolean> = _showTaskDialog.asStateFlow()

    fun openAddTaskDialog(task: StudyTask? = null) {
        _editingTask.value = task
        _showTaskDialog.value = true
    }

    fun closeTaskDialog() {
        _showTaskDialog.value = false
        _editingTask.value = null
    }

    fun saveTask(title: String, subject: String, priority: String, reminderTime: String) {
        viewModelScope.launch {
            val current = _editingTask.value
            if (current != null) {
                repository.updateTask(
                    current.copy(
                        title = title.trim(),
                        subject = subject,
                        priority = priority,
                        reminderTime = reminderTime
                    )
                )
            } else {
                repository.insertTask(
                    StudyTask(
                        title = title.trim(),
                        subject = subject,
                        priority = priority,
                        reminderTime = reminderTime
                    )
                )
            }
            closeTaskDialog()
        }
    }

    fun toggleTaskCompletion(task: StudyTask) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.updateTask(
                task.copy(
                    isCompleted = newCompleted,
                    completedAt = if (newCompleted) System.currentTimeMillis() else null
                )
            )
            // When user checks off a task, automatically record it in that subject's history!
            if (newCompleted) {
                repository.insertSubjectRecord(
                    subjectName = task.subject,
                    title = "${task.title} (Target Complete)",
                    lectureCount = 1,
                    category = "Task Completed"
                )
            }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    // ---------------- FOCUS TIMER ----------------
    private val _timerMode = MutableStateFlow("pomodoro") // "pomodoro" or "stopwatch"
    val timerMode: StateFlow<String> = _timerMode.asStateFlow()

    private val _selectedPresetMins = MutableStateFlow(25)
    val selectedPresetMins: StateFlow<Int> = _selectedPresetMins.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(25 * 60)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _stopwatchSeconds = MutableStateFlow(0)
    val stopwatchSeconds: StateFlow<Int> = _stopwatchSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _taggedTaskId = MutableStateFlow<Long?>(null)
    val taggedTaskId: StateFlow<Long?> = _taggedTaskId.asStateFlow()

    private var timerJob: Job? = null

    fun setTimerMode(mode: String) {
        if (_isTimerRunning.value) pauseTimer()
        _timerMode.value = mode
        if (mode == "pomodoro") {
            _secondsRemaining.value = _selectedPresetMins.value * 60
        } else {
            _stopwatchSeconds.value = 0
        }
    }

    fun setPomodoroPreset(minutes: Int) {
        if (_isTimerRunning.value) pauseTimer()
        _selectedPresetMins.value = minutes
        _secondsRemaining.value = minutes * 60
    }

    fun setTaggedTask(taskId: Long?) {
        _taggedTaskId.value = taskId
    }

    fun toggleTimer() {
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000L)
                if (_timerMode.value == "pomodoro") {
                    if (_secondsRemaining.value > 1) {
                        _secondsRemaining.value -= 1
                    } else {
                        // Finished pomodoro!
                        _secondsRemaining.value = 0
                        _isTimerRunning.value = false
                        onTimerFinished()
                        break
                    }
                } else {
                    // Stopwatch mode
                    _stopwatchSeconds.value += 1
                }
            }
        }
    }

    private fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        if (_timerMode.value == "pomodoro") {
            _secondsRemaining.value = _selectedPresetMins.value * 60
        } else {
            if (_stopwatchSeconds.value > 60) {
                // If stopped after a minute, log the session!
                val mins = (_stopwatchSeconds.value / 60).coerceAtLeast(1)
                logStudySession(mins, "stopwatch")
            }
            _stopwatchSeconds.value = 0
        }
    }

    private fun onTimerFinished() {
        val mins = _selectedPresetMins.value
        logStudySession(mins, "pomodoro")
        alertManager.playSessionAlert(
            title = "Focus Complete! ⚡",
            message = "Shabash! $mins min deep work logged to your consistency streak."
        )
        // Reset timer display
        _secondsRemaining.value = _selectedPresetMins.value * 60
    }

    private fun logStudySession(minutes: Int, mode: String) {
        viewModelScope.launch {
            val task = allTasks.value.firstOrNull { it.id == _taggedTaskId.value }
            repository.logSession(
                durationMinutes = minutes,
                mode = mode,
                taskId = task?.id,
                taskTitle = task?.title,
                subject = task?.subject
            )
        }
    }

    // ---------------- STATS & CONSISTENCY ----------------
    val allSessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyStats: StateFlow<StudyStats> = combine(allSessions, allTasks) { sessions, tasks ->
        repository.computeStats(sessions, tasks)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StudyStats(0, 0, 0, 0, emptyList())
    )

    fun exportDataJson(): String {
        return repository.exportAsJson(allTasks.value, allSessions.value)
    }

    // ---------------- AI TUTOR (CHATBOT) ----------------
    // Supports gemini-3.5-flash, gemini-3.1-flash-lite-preview, gemini-3.1-pro-preview
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedRole = MutableStateFlow("Study Coach & Mentor")
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    fun setChatModel(model: String) {
        _selectedModel.value = model
    }

    fun setModel(model: String) = setChatModel(model)

    fun setChatRole(role: String) {
        _selectedRole.value = role
    }

    fun setRole(role: String) = setChatRole(role)

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isChatLoading.value) return

        viewModelScope.launch {
            _chatError.value = null
            // 1. Save user message locally
            repository.insertChatMessage(role = "user", text = trimmed)

            // 2. Prepare conversation history
            val currentMsgs = chatMessages.value.takeLast(10).map {
                ChatTurn(role = if (it.role == "user") "user" else "model", text = it.text)
            } + ChatTurn(role = "user", text = trimmed)

            val systemPrompt = when (_selectedRole.value) {
                "Study Coach & Mentor" ->
                    "You are StudyOS Coach, a disciplined, encouraging, high-performance study mentor. Help students stay focused, overcome procrastination, break down tough syllabi, and practice spaced repetition. Keep tone energetic and supportive."
                "Doubt Solver (Socratic STEM)" ->
                    "You are a master STEM professor in Physics, Chemistry, and Mathematics. Solve doubts by guiding the student step-by-step with formulas, derivations, and conceptual insights. Format equations cleanly."
                "Quick Concept Explainer" ->
                    "You are an expert tutor who explains complex academic concepts like I'm 15. Use simple real-world analogies, bullet points, and high-yield summary points."
                "Exam Strategist & Formula Guide" ->
                    "You are an exam strategy expert. Provide high-yield formulas, common pitfalls, speed tricks, and revision mnemonic shortcuts for competitive and board exams."
                else ->
                    "You are an AI study tutor helping students learn efficiently and master their subjects."
            }

            _isChatLoading.value = true
            when (val res = geminiClient.sendChatMessage(_selectedModel.value, systemPrompt, currentMsgs)) {
                is GeminiResult.Success -> {
                    repository.insertChatMessage(role = "model", text = res.data)
                }
                is GeminiResult.Error -> {
                    _chatError.value = res.message
                    repository.insertChatMessage(
                        role = "model",
                        text = "⚠️ [Error]: ${res.message}"
                    )
                }
            }
            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // ---------------- PYQ MOCK TEST & QUIZ ----------------
    private val _mockTestStatus = MutableStateFlow(MockTestStatus.IDLE)
    val mockTestStatus: StateFlow<MockTestStatus> = _mockTestStatus.asStateFlow()

    private val _mockTestError = MutableStateFlow<String?>(null)
    val mockTestError: StateFlow<String?> = _mockTestError.asStateFlow()

    private val _currentExam = MutableStateFlow("SSC CGL")
    val currentExam: StateFlow<String> = _currentExam.asStateFlow()

    private val _currentTestSubject = MutableStateFlow("सामान्य अध्ययन एवं विज्ञान")
    val currentTestSubject: StateFlow<String> = _currentTestSubject.asStateFlow()

    private val _mockQuestions = MutableStateFlow<List<com.example.data.MockQuestion>>(emptyList())
    val mockQuestions: StateFlow<List<com.example.data.MockQuestion>> = _mockQuestions.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap()) // questionId -> optionIndex (0..3)
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
    val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _totalTestTimeSeconds = MutableStateFlow(0)
    val totalTestTimeSeconds: StateFlow<Int> = _totalTestTimeSeconds.asStateFlow()

    private val _lastResult = MutableStateFlow<com.example.data.MockTestResultEntity?>(null)
    val lastResult: StateFlow<com.example.data.MockTestResultEntity?> = _lastResult.asStateFlow()

    val allMockTestResults: StateFlow<List<com.example.data.MockTestResultEntity>> = repository.allMockTestResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var mockTimerJob: Job? = null

    fun startMockTest(
        exam: String,
        subject: String,
        topic: String,
        count: Int,
        durationMinutes: Int
    ) {
        _mockTestStatus.value = MockTestStatus.GENERATING
        _mockTestError.value = null
        _currentExam.value = exam
        _currentTestSubject.value = subject

        viewModelScope.launch {
            when (val res = geminiClient.generateMockTest(exam, subject, topic, count)) {
                is GeminiResult.Success -> {
                    val questions = res.data
                    if (questions.isNotEmpty()) {
                        _mockQuestions.value = questions
                        _userAnswers.value = emptyMap()
                        _markedForReview.value = emptySet()
                        _currentQuestionIndex.value = 0
                        val totalSecs = durationMinutes * 60
                        _totalTestTimeSeconds.value = totalSecs
                        _timeRemainingSeconds.value = totalSecs
                        _mockTestStatus.value = MockTestStatus.RUNNING
                        startMockCountdown()
                    } else {
                        _mockTestError.value = "प्रश्न लोड करने में समस्या आई। पुनः प्रयास करें।"
                        _mockTestStatus.value = MockTestStatus.IDLE
                    }
                }
                is GeminiResult.Error -> {
                    _mockTestError.value = res.message
                    _mockTestStatus.value = MockTestStatus.IDLE
                }
            }
        }
    }

    private fun startMockCountdown() {
        mockTimerJob?.cancel()
        mockTimerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0) {
                delay(1000)
                _timeRemainingSeconds.value -= 1
            }
            // Timer expired -> auto submit test
            submitMockTest()
        }
    }

    fun selectAnswer(questionId: Int, optionIndex: Int) {
        val current = _userAnswers.value.toMutableMap()
        current[questionId] = optionIndex
        _userAnswers.value = current
    }

    fun clearAnswer(questionId: Int) {
        val current = _userAnswers.value.toMutableMap()
        current.remove(questionId)
        _userAnswers.value = current
    }

    fun toggleMarkForReview(questionId: Int) {
        val current = _markedForReview.value.toMutableSet()
        if (current.contains(questionId)) {
            current.remove(questionId)
        } else {
            current.add(questionId)
        }
        _markedForReview.value = current
    }

    fun setCurrentQuestionIndex(index: Int) {
        if (index in _mockQuestions.value.indices) {
            _currentQuestionIndex.value = index
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _mockQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun submitMockTest() {
        mockTimerJob?.cancel()
        val questions = _mockQuestions.value
        val answers = _userAnswers.value

        var correct = 0
        var incorrect = 0
        var unattempted = 0

        // Topic performance maps for strength/weakness analysis
        val topicCorrect = mutableMapOf<String, Int>()
        val topicTotal = mutableMapOf<String, Int>()

        // Section performance breakdown
        class SecSummary(
            var total: Int = 0,
            var correct: Int = 0,
            var incorrect: Int = 0,
            var unattempted: Int = 0,
            var marks: Float = 0f
        )
        val sectionMap = mutableMapOf<String, SecSummary>()

        var totalNetMarks = 0f
        var totalPossible = 0

        questions.forEach { q ->
            val secName = q.section.ifBlank { "मुख्य भाग" }
            val summary = sectionMap.getOrPut(secName) { SecSummary() }
            summary.total++
            totalPossible += q.positiveMarks.toInt()

            val userAns = answers[q.id]
            val topic = q.topic.ifBlank { _currentTestSubject.value }
            topicTotal[topic] = (topicTotal[topic] ?: 0) + 1

            if (userAns == null) {
                unattempted++
                summary.unattempted++
            } else if (userAns == q.correctIndex) {
                correct++
                summary.correct++
                summary.marks += q.positiveMarks
                totalNetMarks += q.positiveMarks
                topicCorrect[topic] = (topicCorrect[topic] ?: 0) + 1
            } else {
                incorrect++
                summary.incorrect++
                summary.marks -= q.negativeMarks
                totalNetMarks -= q.negativeMarks
            }
        }

        val strongTopics = mutableListOf<String>()
        val weakTopics = mutableListOf<String>()

        topicTotal.forEach { (topic, total) ->
            val got = topicCorrect[topic] ?: 0
            val pct = (got.toFloat() / total) * 100
            if (pct >= 80) {
                strongTopics.add("$topic (${got}/$total सही)")
            } else {
                weakTopics.add("$topic (${total - got} गलत/छूटे)")
            }
        }

        val sectionReport = sectionMap.entries.joinToString(" ; ") { (sec, s) ->
            "$sec::${s.total}::${s.correct}::${s.incorrect}::${s.unattempted}::${String.format(Locale.getDefault(), "%.1f", s.marks)}"
        }

        val timeTaken = (_totalTestTimeSeconds.value - _timeRemainingSeconds.value).coerceAtLeast(1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val resultEntity = com.example.data.MockTestResultEntity(
            examName = _currentExam.value,
            subjectName = _currentTestSubject.value,
            totalQuestions = questions.size,
            correctCount = correct,
            incorrectCount = incorrect,
            unattemptedCount = unattempted,
            score = totalNetMarks.toInt(),
            totalPossibleMarks = totalPossible.coerceAtLeast(1),
            timeTakenSeconds = timeTaken,
            weaknesses = weakTopics.joinToString(" • "),
            strengths = strongTopics.joinToString(" • "),
            sectionBreakdown = sectionReport,
            dateString = dateStr
        )

        _lastResult.value = resultEntity
        _mockTestStatus.value = MockTestStatus.SUBMITTED

        viewModelScope.launch {
            repository.insertMockTestResult(resultEntity)
        }
    }

    fun resetMockTest() {
        mockTimerJob?.cancel()
        _mockTestStatus.value = MockTestStatus.IDLE
        _mockQuestions.value = emptyList()
        _userAnswers.value = emptyMap()
        _markedForReview.value = emptySet()
        _currentQuestionIndex.value = 0
    }

    fun deleteMockTestResult(id: Long) {
        viewModelScope.launch {
            repository.deleteMockTestResult(id)
        }
    }
}
