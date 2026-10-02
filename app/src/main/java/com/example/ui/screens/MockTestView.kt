package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MockQuestion
import com.example.data.MockTestResultEntity
import com.example.ui.MockTestStatus
import com.example.ui.StudyViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun MockTestView(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.mockTestStatus.collectAsStateWithLifecycle()
    val questions by viewModel.mockQuestions.collectAsStateWithLifecycle()
    val userAnswers by viewModel.userAnswers.collectAsStateWithLifecycle()
    val reviewMarked by viewModel.markedForReview.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsStateWithLifecycle()
    val totalTime by viewModel.totalTestTimeSeconds.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val allHistory by viewModel.allMockTestResults.collectAsStateWithLifecycle()
    val errorMsg by viewModel.mockTestError.collectAsStateWithLifecycle()

    when (status) {
        MockTestStatus.IDLE, MockTestStatus.GENERATING -> {
            MockTestSetupView(
                isGenerating = status == MockTestStatus.GENERATING,
                errorMsg = errorMsg,
                history = allHistory,
                onStartTest = { exam, subject, topic, count, timeMin ->
                    viewModel.startMockTest(exam, subject, topic, count, timeMin)
                },
                onDeleteHistory = { viewModel.deleteMockTestResult(it) },
                modifier = modifier
            )
        }
        MockTestStatus.RUNNING -> {
            ActiveMockTestView(
                questions = questions,
                currentIndex = currentIndex,
                userAnswers = userAnswers,
                reviewMarked = reviewMarked,
                timeRemainingSeconds = timeRemaining,
                totalTimeSeconds = totalTime,
                onSelectOption = { qId, optIndex -> viewModel.selectAnswer(qId, optIndex) },
                onClearOption = { qId -> viewModel.clearAnswer(qId) },
                onToggleReview = { qId -> viewModel.toggleMarkForReview(qId) },
                onSelectQuestion = { idx -> viewModel.setCurrentQuestionIndex(idx) },
                onNext = { viewModel.nextQuestion() },
                onPrevious = { viewModel.previousQuestion() },
                onSubmit = { viewModel.submitMockTest() },
                modifier = modifier
            )
        }
        MockTestStatus.SUBMITTED -> {
            MockTestResultView(
                result = lastResult,
                questions = questions,
                userAnswers = userAnswers,
                onRetake = { viewModel.resetMockTest() },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MockTestSetupView(
    isGenerating: Boolean,
    errorMsg: String?,
    history: List<MockTestResultEntity>,
    onStartTest: (exam: String, subject: String, topic: String, count: Int, timeMin: Int) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val examSubjectsMap = remember {
        mapOf(
            "SSC CGL" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (सभी 4 विषय Mixed)",
                "गणित (Quantitative Aptitude)",
                "तर्कशक्ति (General Intelligence & Reasoning)",
                "सामान्य ज्ञान व सामान्य विज्ञान (GK & Science)",
                "अंग्रेजी (English Language & Comprehension)"
            ),
            "Railway RRB NTPC" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (सभी विषय Mixed)",
                "गणित (Mathematics)",
                "सामान्य बुद्धिमत्ता व तर्कशक्ति (Reasoning)",
                "सामान्य विज्ञान (General Science)",
                "सामान्य जागरूकता (General Awareness)"
            ),
            "UPSC CSE / State PSC" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (GS Mixed)",
                "भारतीय इतिहास व राष्ट्रीय आंदोलन",
                "भारतीय राजव्यवस्था व संविधान",
                "भूगोल व पर्यावरण",
                "सामान्य विज्ञान व प्रौद्योगिकी",
                "भारतीय अर्थव्यवस्था",
                "CSAT तर्कशक्ति व गणित"
            ),
            "JEE Main" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (PCM Mixed)",
                "भौतिक विज्ञान (Physics)",
                "रसायन विज्ञान (Chemistry)",
                "गणित (Mathematics)"
            ),
            "NEET" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (PCB Mixed)",
                "जीव विज्ञान (Biology)",
                "भौतिक विज्ञान (Physics)",
                "रसायन विज्ञान (Chemistry)"
            ),
            "NDA / CDS" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (GAT & Maths Mixed)",
                "गणित (Mathematics)",
                "अंग्रेजी (English)",
                "सामान्य विज्ञान (General Science)",
                "सामान्य अध्ययन (General Studies)"
            ),
            "Banking (IBPS/SBI)" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (सभी विषय Mixed)",
                "संख्यात्मक अभियोग्यता (Quantitative Aptitude)",
                "तर्कशक्ति क्षमता (Reasoning Ability)",
                "अंग्रेजी भाषा (English Language)",
                "बैंकिंग व वित्तीय जागरूकता"
            ),
            "State Police / SI" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (सभी विषय Mixed)",
                "सामान्य ज्ञान व विज्ञान",
                "संख्यात्मक व मानसिक क्षमता (Maths)",
                "तर्कशक्ति (Reasoning)",
                "सामान्य हिंदी (General Hindi)"
            ),
            "सामान्य प्रतियोगिता (General)" to listOf(
                "🌟 पूर्ण मॉक टेस्ट (Mixed All Subjects)",
                "सामान्य विज्ञान (Physics, Chem, Bio)",
                "गणित (Quantitative Maths)",
                "तर्कशक्ति (Reasoning)",
                "सामान्य ज्ञान व समसामयिकी (GK)",
                "अंग्रेजी भाषा (English)"
            )
        )
    }

    val exams = listOf(
        "SSC CGL",
        "Railway RRB NTPC",
        "UPSC CSE / State PSC",
        "JEE Main",
        "NEET",
        "NDA / CDS",
        "Banking (IBPS/SBI)",
        "State Police / SI",
        "सामान्य प्रतियोगिता (General)"
    )

    var selectedExam by remember { mutableStateOf("SSC CGL") }
    val currentSubjects = examSubjectsMap[selectedExam] ?: listOf("🌟 पूर्ण मॉक टेस्ट (सभी विषय Mixed)")
    var selectedSubject by remember { mutableStateOf(currentSubjects.first()) }
    var customTopic by remember { mutableStateOf("") }
    var questionCount by remember { mutableIntStateOf(100) }
    var timerMinutes by remember { mutableIntStateOf(60) }

    val countOptions = listOf(10, 20, 40, 60, 100)
    val timeOptions = listOf(10, 15, 20, 30, 45, 60)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_test_setup_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceCard, SurfaceDark)
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 PYQ MOCK TEST (परीक्षा मोड)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCyan.copy(alpha = 0.2f))
                                .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "हिंदी माध्यम 🇮🇳",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "विगत वर्षों के प्रश्न (PYQs) पर आधारित सटीक परीक्षा पैटर्न मॉक टेस्ट। सभी सेक्शन का अलग टाइमर व स्कोर विश्लेषण उपलब्ध है।",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Exam Selection
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. परीक्षा चुनें (SELECT EXAM):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exams.forEach { exam ->
                        val isSelected = selectedExam == exam
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentCyan else SurfaceCard)
                                .border(1.dp, if (isSelected) AccentCyan else SurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedExam = exam
                                    selectedSubject = examSubjectsMap[exam]?.firstOrNull() ?: "🌟 पूर्ण मॉक टेस्ट"
                                    if (exam == "SSC CGL") {
                                        questionCount = 100
                                        timerMinutes = 60
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = exam,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // SPECIFIC SSC CGL REAL EXAM PRESETS CARD
        if (selectedExam == "SSC CGL") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚡ SSC CGL आधिकारिक परीक्षा पैटर्न चुने (PRESETS):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        letterSpacing = 0.5.sp
                    )

                    // Tier 1 Real 100 Qs Mock Preset
                    val isTier1 = questionCount == 100 && timerMinutes == 60
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTier1) AccentEmerald.copy(alpha = 0.2f) else SurfaceCard)
                            .border(1.dp, if (isTier1) AccentEmerald else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSubject = "🌟 पूर्ण मॉक टेस्ट (सभी 4 विषय Mixed)"
                                questionCount = 100
                                timerMinutes = 60
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏛️ SSC CGL Tier-1 Real Full Mock",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTier1) AccentEmerald else TextPrimary
                                )
                                Text(
                                    text = "100 प्रश्न • 60 मिनट",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "4 सेक्शन (25 तर्कशक्ति + 25 सामान्य ज्ञान + 25 गणित + 25 अंग्रेजी) • कुल 200 अंक (+2, -0.5)",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Tier 2 Section 1 (Math + Reasoning Sectional Timer) Preset
                    val isTier2 = questionCount == 60 && timerMinutes == 60
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTier2) AccentIndigo.copy(alpha = 0.2f) else SurfaceCard)
                            .border(1.dp, if (isTier2) AccentIndigo else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSubject = "🌟 पूर्ण मॉक टेस्ट (सभी 4 विषय Mixed)"
                                questionCount = 60
                                timerMinutes = 60
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱️ SSC CGL Tier-2 Sectional Timer (Section 1)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTier2) AccentIndigo else TextPrimary
                                )
                                Text(
                                    text = "60 प्रश्न • 60 मिनट",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "गणित (30 प्रश्न) + तर्कशक्ति (30 प्रश्न) • सेक्शनल टाइमर 1 घंटा • कुल 180 अंक (+3, -1)",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Quick Speed Sprint (20 Qs, 15 Mins) Preset
                    val isSprint = questionCount == 20 && timerMinutes == 15
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSprint) AccentCyan.copy(alpha = 0.2f) else SurfaceCard)
                            .border(1.dp, if (isSprint) AccentCyan else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSubject = "🌟 पूर्ण मॉक टेस्ट (सभी 4 विषय Mixed)"
                                questionCount = 20
                                timerMinutes = 15
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⚡ SSC CGL Daily Speed Sprint",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSprint) AccentCyan else TextPrimary
                                )
                                Text(
                                    text = "सभी 4 सेक्शन से 5-5 प्रश्न (त्वरित अभ्यास)",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "20 प्रश्न • 15 मिनट",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }
                }
            }
        }

        // Subject Selection (Dynamically mapped based on selectedExam)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. विषय चुनें ($selectedExam):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Scroll ➔",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentSubjects.forEach { sub ->
                        val isSelected = selectedSubject == sub
                        val isFull = sub.contains("पूर्ण मॉक")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected && isFull -> AccentEmerald
                                        isSelected -> AccentIndigo
                                        isFull -> AccentEmerald.copy(alpha = 0.15f)
                                        else -> SurfaceCard
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isSelected -> Color.Transparent
                                        isFull -> AccentEmerald.copy(alpha = 0.5f)
                                        else -> SurfaceBorder
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedSubject = sub }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected && isFull -> Color.Black
                                    isSelected -> Color.White
                                    isFull -> AccentEmerald
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                if (selectedSubject.contains("पूर्ण मॉक")) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentEmerald.copy(alpha = 0.12f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚡ पूर्ण मॉक टेस्ट: इसमें $selectedExam के सभी विषयों से संतुलित सेक्शनल प्रश्न शामिल होंगे।",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentEmerald
                        )
                    }
                }
            }
        }

        // Optional Specific Topic
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "3. विशिष्ट टॉपिक (वैकल्पिक / OPTIONAL TOPIC):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = customTopic,
                    onValueChange = { customTopic = it },
                    placeholder = { Text("उदा. प्रतिशतता, सादृश्यता, प्रकाशिकी, Synonyms") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Question Count Selection
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. प्रश्नों की संख्या (QUESTIONS COUNT):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    countOptions.forEach { count ->
                        val isSelected = questionCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentEmerald else SurfaceCard)
                                .border(1.dp, if (isSelected) AccentEmerald else SurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    questionCount = count
                                    if (count == 10) timerMinutes = 10
                                    if (count == 20) timerMinutes = 15
                                    if (count == 40) timerMinutes = 30
                                    if (count == 60) timerMinutes = 60
                                    if (count == 100) timerMinutes = 60
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count == 100) "100 (Full)" else "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Timer Duration Selection
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "5. समय सीमा (TIMER DURATION):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeOptions.forEach { min ->
                        val isSelected = timerMinutes == min
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentAmber else SurfaceCard)
                                .border(1.dp, if (isSelected) AccentAmber else SurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable { timerMinutes = min }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (isSelected) Color.Black else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$min Min",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message if any
        if (!errorMsg.isNullOrBlank()) {
            item {
                Text(text = errorMsg, color = AccentRose, fontSize = 11.sp)
            }
        }

        // Start Test Button
        item {
            Button(
                onClick = {
                    onStartTest(selectedExam, selectedSubject, customTopic, questionCount, timerMinutes)
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_mock_test_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PYQ मॉक टेस्ट तैयार हो रहा है...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "परीक्षा शुरू करें ($questionCount प्रश्न • $timerMinutes मिनट)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Past Test History & Weakness/Strength Summary
        if (history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "विगत टेस्ट परिणाम व कमजोरियों का रिकॉर्ड:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${history.size} टेस्ट दिए",
                        fontSize = 10.sp,
                        color = AccentCyan
                    )
                }
            }

            items(history, key = { it.id }) { item ->
                PastTestRecordCard(
                    record = item,
                    onDelete = { onDeleteHistory(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActiveMockTestView(
    questions: List<MockQuestion>,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    reviewMarked: Set<Int>,
    timeRemainingSeconds: Int,
    totalTimeSeconds: Int,
    onSelectOption: (qId: Int, optIndex: Int) -> Unit,
    onClearOption: (qId: Int) -> Unit,
    onToggleReview: (qId: Int) -> Unit,
    onSelectQuestion: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSubmitConfirmation by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentIndex) ?: return
    val selectedOption = userAnswers[currentQ.id]
    val isMarked = reviewMarked.contains(currentQ.id)

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val isTimeLow = timeRemainingSeconds < 120

    val answeredCount = userAnswers.size
    val totalCount = questions.size

    // Section group analysis
    val sections = remember(questions) {
        questions.map { it.section.ifBlank { "मुख्य भाग" } }.distinct()
    }
    var activeSection by remember(sections, currentQ.section) {
        mutableStateOf(currentQ.section.ifBlank { sections.firstOrNull() ?: "" })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_mock_test_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Top Exam Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .border(
                    1.dp,
                    if (isTimeLow) AccentRose else SurfaceBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time",
                        tint = if (isTimeLow) AccentRose else AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTimeLow) AccentRose else AccentCyan
                    )
                }

                // Progress Badge
                Text(
                    text = "हल: $answeredCount/$totalCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald
                )

                // Submit Button
                Button(
                    onClick = { showSubmitConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.testTag("submit_test_button")
                ) {
                    Text(
                        text = "सबमिट",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // 2. Real Section Navigation Switcher (जैसे असली TCS iON परीक्षा में होता है)
        if (sections.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { sec ->
                    val secQuestions = questions.filter { (it.section.ifBlank { "मुख्य भाग" }) == sec }
                    val secAnswered = secQuestions.count { userAnswers.containsKey(it.id) }
                    val isSelectedSec = activeSection == sec

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelectedSec) AccentCyan else SurfaceCard)
                            .border(1.dp, if (isSelectedSec) AccentCyan else SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                activeSection = sec
                                val firstQ = secQuestions.firstOrNull()
                                if (firstQ != null) {
                                    val idx = questions.indexOf(firstQ)
                                    if (idx != -1) onSelectQuestion(idx)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${sec.take(12)} ($secAnswered/${secQuestions.size})",
                            fontSize = 10.sp,
                            fontWeight = if (isSelectedSec) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelectedSec) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        // 3. Question Navigation Palette for the Active Section
        val visibleQuestions = if (sections.size > 1) {
            questions.filter { (it.section.ifBlank { "मुख्य भाग" }) == activeSection }
        } else {
            questions
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visibleQuestions.forEach { q ->
                val overallIndex = questions.indexOf(q)
                val isAnswered = userAnswers.containsKey(q.id)
                val isRev = reviewMarked.contains(q.id)
                val isCur = overallIndex == currentIndex

                val boxColor = when {
                    isCur -> AccentCyan
                    isRev -> AccentAmber
                    isAnswered -> AccentEmerald
                    else -> SurfaceDark
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(boxColor)
                        .border(
                            if (isCur) 2.dp else 1.dp,
                            if (isCur) Color.White else SurfaceBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectQuestion(overallIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${overallIndex + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCur || isAnswered || isRev) Color.Black else TextSecondary
                    )
                }
            }
        }

        // 4. Question Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "प्रश्न ${currentIndex + 1} / $totalCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentIndigo.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${currentQ.section.ifBlank { "" }} • ${currentQ.topic}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIndigo
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = currentQ.question,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 21.sp
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 4 Options (A, B, C, D) in Hindi - answers strictly hidden during exam!
                items(currentQ.options.size) { optIdx ->
                    val optText = currentQ.options[optIdx]
                    val isSelected = selectedOption == optIdx

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceDark)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) AccentCyan else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectOption(currentQ.id, optIdx) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) AccentCyan else Color.Transparent)
                                    .border(
                                        2.dp,
                                        if (isSelected) AccentCyan else TextMuted,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 5. Action & Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onPrevious()
                    val prevQ = questions.getOrNull(currentIndex - 1)
                    if (prevQ != null && prevQ.section.isNotBlank()) activeSection = prevQ.section
                },
                enabled = currentIndex > 0,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Prev",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("पिछला", fontSize = 11.sp, color = TextPrimary)
            }

            // Mark for Review
            TextButton(onClick = { onToggleReview(currentQ.id) }) {
                Icon(
                    imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Review",
                    tint = if (isMarked) AccentAmber else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMarked) "समीक्षा में" else "मार्क करें",
                    fontSize = 11.sp,
                    color = if (isMarked) AccentAmber else TextMuted
                )
            }

            // Clear answer
            if (selectedOption != null) {
                TextButton(onClick = { onClearOption(currentQ.id) }) {
                    Text("हटाएं", fontSize = 11.sp, color = TextMuted)
                }
            }

            Button(
                onClick = {
                    onNext()
                    val nextQ = questions.getOrNull(currentIndex + 1)
                    if (nextQ != null && nextQ.section.isNotBlank()) activeSection = nextQ.section
                },
                enabled = currentIndex < questions.size - 1,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("अगला", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitConfirmation) {
        val unans = questions.size - answeredCount
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            containerColor = SurfaceCard,
            title = {
                Text(text = "टेस्ट सबमिट करें?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "कुल प्रश्न: $totalCount",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "हल किए: $answeredCount",
                        fontSize = 13.sp,
                        color = AccentEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    if (unans > 0) {
                        Text(
                            text = "छूटे हुए: $unans",
                            fontSize = 13.sp,
                            color = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "सबमिट करने पर तुरंत सेक्शन-वार परिणाम, कमजोरियों का विश्लेषण और सभी 100 प्रश्नों के विस्तृत हल दिखाई देंगे।",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        onSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Text("हाँ, सबमिट करें", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("जारी रखें", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MockTestResultView(
    result: MockTestResultEntity?,
    questions: List<MockQuestion>,
    userAnswers: Map<Int, Int>,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    val accuracy = if (result.totalQuestions > 0) {
        ((result.correctCount.toFloat() / result.totalQuestions) * 100).toInt()
    } else 0

    val timeMin = result.timeTakenSeconds / 60
    val timeSec = result.timeTakenSeconds % 60

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_test_result_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scorecard Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceCard, SurfaceDark)
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🏆 SSC CGL परीक्षा परिणाम (SCORECARD)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${result.score} / ${result.totalPossibleMarks}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (accuracy >= 60) AccentEmerald else AccentAmber
                    )

                    Text(
                        text = "सटीकता (Accuracy): $accuracy% • समय: ${timeMin}m ${timeSec}s",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreStatPill(
                            label = "सही (Correct)",
                            value = "${result.correctCount}",
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreStatPill(
                            label = "गलत (Wrong)",
                            value = "${result.incorrectCount}",
                            color = AccentRose,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreStatPill(
                            label = "छूटे (Skipped)",
                            value = "${result.unattemptedCount}",
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // SECTION-WISE PERFORMANCE REPORT CARD (सेक्शन अनुसार परिणाम)
        if (result.sectionBreakdown.isNotBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📊 सेक्शन-वार प्रदर्शन रिपोर्ट (SECTION-WISE BREAKDOWN):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        letterSpacing = 0.5.sp
                    )

                    val secItems = result.sectionBreakdown.split(" ; ").filter { it.isNotBlank() }
                    secItems.forEach { itemStr ->
                        val parts = itemStr.split("::")
                        if (parts.size >= 6) {
                            val secName = parts[0]
                            val totalQ = parts[1].toIntOrNull() ?: 0
                            val corrQ = parts[2].toIntOrNull() ?: 0
                            val wrongQ = parts[3].toIntOrNull() ?: 0
                            val skipQ = parts[4].toIntOrNull() ?: 0
                            val mks = parts[5]

                            val secAcc = if (totalQ > 0) ((corrQ.toFloat() / totalQ) * 100).toInt() else 0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = secName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "$mks अंक ($secAcc%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (secAcc >= 60) AccentEmerald else AccentAmber
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "कुल प्रश्न: $totalQ • सही: $corrQ • गलत: $wrongQ • छूटे: $skipQ",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // STRENGTHS & WEAKNESSES ANALYSIS (कमजोरियों और मजबूतियों का विश्लेषण)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "कमजोरी और मजबूती का विश्लेषण (ANALYSIS):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                // Strengths Box
                if (result.strengths.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentEmerald.copy(alpha = 0.1f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(text = "💪 मजबूत टॉपिक्स (Strengths):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.strengths,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Weaknesses Box
                if (result.weaknesses.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentRose.copy(alpha = 0.1f))
                            .border(1.dp, AccentRose.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(text = "⚠️ कमजोर टॉपिक्स (Weaknesses - सुधार करें):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentRose)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.weaknesses,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "सुझाव: इन टॉपिक्स को To-Do में जोड़कर पुनः फॉर्मूला व थ्योरी रिवीजन करें।",
                                fontSize = 10.sp,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Detailed Solutions Review
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "सभी प्रश्नों के संपूर्ण हल व व्याख्या (SOLUTIONS):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("नया टेस्ट", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Question-by-Question Solution Review
        items(questions, key = { it.id }) { q ->
            val userAns = userAnswers[q.id]
            val isCorrect = userAns == q.correctIndex
            val isSkipped = userAns == null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(
                        1.dp,
                        when {
                            isCorrect -> AccentEmerald.copy(alpha = 0.6f)
                            isSkipped -> TextMuted.copy(alpha = 0.4f)
                            else -> AccentRose.copy(alpha = 0.6f)
                        },
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "प्रश्न ${q.id}: ${q.section} • ${q.topic}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isCorrect -> AccentEmerald.copy(alpha = 0.2f)
                                        isSkipped -> TextMuted.copy(alpha = 0.2f)
                                        else -> AccentRose.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    isCorrect -> "सही (+${q.positiveMarks})"
                                    isSkipped -> "छूटा (0)"
                                    else -> "गलत (-${q.negativeMarks})"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCorrect -> AccentEmerald
                                    isSkipped -> TextMuted
                                    else -> AccentRose
                                }
                            )
                        }
                    }

                    Text(
                        text = q.question,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    // Options list
                    q.options.forEachIndexed { optIdx, optText ->
                        val isUserPick = userAns == optIdx
                        val isCorrectOption = optIdx == q.correctIndex

                        val optBg = when {
                            isCorrectOption -> AccentEmerald.copy(alpha = 0.2f)
                            isUserPick -> AccentRose.copy(alpha = 0.2f)
                            else -> SurfaceDark
                        }
                        val optBorder = when {
                            isCorrectOption -> AccentEmerald
                            isUserPick -> AccentRose
                            else -> SurfaceBorder
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(optBg)
                                .border(1.dp, optBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        isCorrectOption -> "✔ "
                                        isUserPick -> "✖ "
                                        else -> "  "
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectOption) AccentEmerald else AccentRose
                                )
                                Text(
                                    text = optText,
                                    fontSize = 11.sp,
                                    color = if (isCorrectOption) TextPrimary else TextSecondary,
                                    fontWeight = if (isCorrectOption) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Explanation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(OledBlack)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "💡 विस्तृत व्याख्या:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = q.explanation,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "New Test",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("नया मॉक टेस्ट शुरू करें", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ScoreStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(OledBlack)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 9.sp, color = TextMuted)
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun PastTestRecordCard(
    record: MockTestResultEntity,
    onDelete: () -> Unit
) {
    val pct = if (record.totalQuestions > 0) {
        ((record.correctCount.toFloat() / record.totalQuestions) * 100).toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${record.examName} • ${record.subjectName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${record.score} अंक ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pct >= 60) AccentEmerald else AccentAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "तारीख: ${record.dateString} • सही: ${record.correctCount}/${record.totalQuestions} • समय: ${record.timeTakenSeconds / 60}m",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )

            if (record.weaknesses.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "कमजोरी: ${record.weaknesses}",
                    fontSize = 10.sp,
                    color = AccentRose,
                    maxLines = 1
                )
            }
        }
    }
}
