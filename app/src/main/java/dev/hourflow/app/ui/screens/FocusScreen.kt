package dev.hourflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.data.AppState
import dev.hourflow.app.ui.AppViewModel
import dev.hourflow.app.ui.FocusUiState
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.CategoryPill
import dev.hourflow.app.ui.components.EmptyState
import dev.hourflow.app.ui.components.ProgressRing
import dev.hourflow.app.ui.components.SectionHeader
import dev.hourflow.app.ui.components.TinyDot
import dev.hourflow.app.ui.theme.Coral
import dev.hourflow.app.ui.theme.Cream
import dev.hourflow.app.ui.theme.HeroInk
import dev.hourflow.app.ui.theme.HeroInkLight
import dev.hourflow.app.util.formatClock
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.greeting
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val subjects = listOf("Physics", "Chemistry", "Biology", "Revision", "Mock")
private val durations = listOf(25, 45, 60, 90)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    state: AppState,
    focus: FocusUiState,
    now: LocalDateTime,
    onSetDuration: (Int) -> Unit,
    onSetSubject: (String) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onLogManual: (String, Int) -> Unit
) {
    val today = now.toLocalDate()
    val todaySessions = state.sessions.filter { it.localDate == today }.sortedByDescending { it.startedAt }
    val todayMinutes = todaySessions.sumOf { it.minutes }
    val goal = state.settings.dailyGoalMinutes.coerceAtLeast(1)
    val goalProgress = (todayMinutes.toFloat() / goal).coerceIn(0f, 1f)

    var showManual by rememberSaveable { mutableStateOf(false) }
    var manualMinutes by rememberSaveable { mutableStateOf("30") }
    var manualSubject by rememberSaveable { mutableStateOf("Physics") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "${greeting(now.hour)}. Ready to focus?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "One subject. One block. No tab switching.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                text = "SUBJECT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = focus.subject == subject,
                        onClick = { onSetSubject(subject) },
                        label = { Text(subject) },
                        enabled = !focus.running
                    )
                }
            }
        }

        item {
            FocusTimerCard(
                focus = focus,
                onStart = onStart,
                onPause = onPause,
                onFinish = onFinish,
                onReset = onReset
            )
        }

        item {
            if (!focus.running && focus.elapsedSeconds == 0 && !focus.justFinished) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durations.forEach { minutes ->
                        FilterChip(
                            selected = focus.durationMinutes == minutes,
                            onClick = { onSetDuration(minutes) },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionHeader(title = "Today's focus")
                    }
                    TextButton(onClick = { showManual = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Log")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatDuration(todayMinutes),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "of ${formatDuration(goal)} goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                if (todaySessions.isEmpty()) {
                    Text(
                        text = "No sessions logged yet. A 25-minute sprint is enough to start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        todaySessions.take(8).forEach { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TinyDot(color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = session.subject,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = Instant.ofEpochMilli(session.startedAt)
                                            .atZone(ZoneId.systemDefault())
                                            .format(timeFormatter),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatDuration(session.minutes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("The 25-minute rule", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Stuck? Set 25 minutes, close everything else, and only open the first page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showManual) {
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Log study time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        subjects.forEach { subject ->
                            FilterChip(
                                selected = manualSubject == subject,
                                onClick = { manualSubject = subject },
                                label = { Text(subject, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = manualMinutes,
                        onValueChange = { text ->
                            manualMinutes = text.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = manualMinutes.toIntOrNull() ?: 0
                        if (minutes > 0) {
                            onLogManual(manualSubject, minutes)
                            showManual = false
                            manualMinutes = "30"
                        }
                    },
                    enabled = (manualMinutes.toIntOrNull() ?: 0) > 0
                ) { Text("Log session") }
            },
            dismissButton = {
                TextButton(onClick = { showManual = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FocusTimerCard(
    focus: FocusUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit
) {
    val status = when {
        focus.justFinished -> "SESSION LOGGED"
        focus.running -> "FOCUSING"
        focus.elapsedSeconds > 0 -> "PAUSED"
        else -> "READY"
    }
    val progress = if (focus.justFinished) 1f else focus.progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(HeroInk, HeroInkLight)))
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TinyDot(Coral)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = focus.subject.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Coral
                )
            }
            Spacer(Modifier.height(16.dp))

            ProgressRing(
                progress = progress,
                size = 226.dp,
                strokeWidth = 10.dp,
                color = Coral,
                trackColor = Color.White.copy(alpha = 0.10f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatClock(if (focus.justFinished) 0 else focus.secondsLeft),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 46.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Cream
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Cream.copy(alpha = 0.48f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        when {
                            focus.running -> onPause()
                            focus.justFinished -> {
                                onReset()
                                onStart()
                            }
                            else -> onStart()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral,
                        contentColor = Color(0xFF2A0C05)
                    )
                ) {
                    Icon(
                        imageVector = when {
                            focus.running -> Icons.Rounded.Pause
                            focus.justFinished -> Icons.Rounded.Refresh
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            focus.running -> "Pause"
                            focus.justFinished -> "Go again"
                            focus.elapsedSeconds > 0 -> "Resume"
                            else -> "Start focus"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!focus.justFinished && focus.elapsedSeconds > 0) {
                    OutlinedButton(
                        onClick = onFinish,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cream)
                    ) {
                        Text("Finish & log")
                    }
                }
            }

            if (focus.justFinished) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${formatDuration(focus.durationMinutes)} logged. Take a real break.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.7f)
                    )
                }
            } else if (focus.elapsedSeconds > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatDuration(focus.elapsedSeconds / 60)} in this block",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onReset) {
                        Text(
                            "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            color = Cream.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}
