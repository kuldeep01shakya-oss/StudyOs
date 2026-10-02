package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StudyTask
import com.example.ui.StudyViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AlertManager

@Composable
fun FocusTimerScreen(
    viewModel: StudyViewModel,
    tasks: List<StudyTask>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val selectedPresetMins by viewModel.selectedPresetMins.collectAsStateWithLifecycle()
    val secondsRemaining by viewModel.secondsRemaining.collectAsStateWithLifecycle()
    val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val taggedTaskId by viewModel.taggedTaskId.collectAsStateWithLifecycle()

    val taggedTask = tasks.firstOrNull { it.id == taggedTaskId }
    var taskDropdownExpanded by remember { mutableStateOf(false) }

    // Format display string
    val totalSec = if (timerMode == "pomodoro") secondsRemaining else stopwatchSeconds
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val clockString = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val glowingBorderColor by animateColorAsState(
        targetValue = if (isTimerRunning) AccentCyan else SurfaceBorder,
        label = "clockBorder"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_timer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Focus Engine Hero Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceCard, SurfaceDark)
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("focus_engine_card")
            ) {
                Column {
                    // Header & Mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Focus Engine",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Pomodoro / Stopwatch segmented pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(OledBlack)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (timerMode == "pomodoro") AccentIndigo else Color.Transparent)
                                    .clickable { viewModel.setTimerMode("pomodoro") }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("mode_pomodoro")
                            ) {
                                Text(
                                    text = "Pomodoro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerMode == "pomodoro") Color.White else TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (timerMode == "stopwatch") AccentIndigo else Color.Transparent)
                                    .clickable { viewModel.setTimerMode("stopwatch") }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("mode_stopwatch")
                            ) {
                                Text(
                                    text = "Stopwatch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerMode == "stopwatch") Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pomodoro Presets Row (Only visible in Pomodoro mode)
                    if (timerMode == "pomodoro") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf(25, 50, 90).forEach { mins ->
                                val isSelected = selectedPresetMins == mins
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else OledBlack)
                                        .border(
                                            1.dp,
                                            if (isSelected) AccentCyan else SurfaceBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setPomodoroPreset(mins) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("preset_${mins}m")
                                ) {
                                    Text(
                                        text = "$mins min",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AccentCyan else TextSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Giant Timer Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(OledBlack.copy(alpha = 0.9f))
                            .border(1.5.dp, glowingBorderColor, RoundedCornerShape(20.dp))
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = clockString,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isTimerRunning) AccentCyan else TextPrimary,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.testTag("timer_clock_display")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isTimerRunning) "Deep work sprint in progress" else "Ready for deep work sprint",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isTimerRunning) AccentEmerald else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Timer Controls (Start/Pause & Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Main Start / Pause button
                        Button(
                            onClick = { viewModel.toggleTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) AccentIndigo else AccentCyan
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(52.dp)
                                .testTag("timer_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Start",
                                tint = if (isTimerRunning) Color.White else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTimerRunning) "Pause Focus" else "Start Focus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerRunning) Color.White else Color.Black
                            )
                        }

                        // Reset Button
                        Button(
                            onClick = { viewModel.resetTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = OledBlack),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                                .testTag("timer_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 2. Tag Current Session to Task
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "TAG CURRENT SESSION TO TARGET:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OledBlack)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { taskDropdownExpanded = true }
                            .padding(12.dp)
                            .testTag("tag_task_dropdown")
                    ) {
                        Text(
                            text = taggedTask?.title ?: "-- General Study (No Specific Target) --",
                            fontSize = 12.sp,
                            color = if (taggedTask != null) AccentCyan else TextSecondary
                        )

                        DropdownMenu(
                            expanded = taskDropdownExpanded,
                            onDismissRequest = { taskDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("-- General Study --") },
                                onClick = {
                                    viewModel.setTaggedTask(null)
                                    taskDropdownExpanded = false
                                }
                            )
                            tasks.filter { !it.isCompleted }.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text("${task.subject}: ${task.title}") },
                                    onClick = {
                                        viewModel.setTaggedTask(task.id)
                                        taskDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Audio & Alarm Test Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceCard.copy(alpha = 0.7f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Audio Alert & Haptic",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Timer khatam hone par alert bajega",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val alert = AlertManager(context)
                            alert.playSessionAlert("Test Alert", "Sound aur vibration activate ho gaye!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("test_sound_button")
                    ) {
                        Text(text = "Test", fontSize = 11.sp, color = AccentCyan)
                    }
                }
            }
        }
    }
}
