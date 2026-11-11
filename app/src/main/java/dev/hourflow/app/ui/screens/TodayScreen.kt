package dev.hourflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.RoutineBlock
import dev.hourflow.app.data.blocksFor
import dev.hourflow.app.data.completedIdsOn
import dev.hourflow.app.ui.components.AccentLabel
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.CategoryPill
import dev.hourflow.app.ui.components.HourAgenda
import dev.hourflow.app.ui.components.ProgressRing
import dev.hourflow.app.ui.components.SectionHeader
import dev.hourflow.app.ui.components.TinyDot
import dev.hourflow.app.ui.theme.Coral
import dev.hourflow.app.ui.theme.Cream
import dev.hourflow.app.ui.theme.HeroInk
import dev.hourflow.app.ui.theme.HeroInkLight
import dev.hourflow.app.ui.theme.categoryAccent
import dev.hourflow.app.util.dayStat
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.formatRange
import dev.hourflow.app.util.greeting
import dev.hourflow.app.util.percent
import dev.hourflow.app.util.readable
import dev.hourflow.app.util.relativeEnd
import dev.hourflow.app.util.relativeMinutes
import dev.hourflow.app.util.streak
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TodayScreen(
    state: AppState,
    now: LocalDateTime,
    onAddBlock: (Int?) -> Unit,
    onEditBlock: (RoutineBlock) -> Unit,
    onToggle: (LocalDate, String) -> Unit,
    onApplyStarter: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenCoach: () -> Unit
) {
    val today = now.toLocalDate()
    val nowMinute = now.hour * 60 + now.minute
    val blocks = state.blocksFor(today)
    val completed = state.completedIdsOn(today)
    val stat = state.dayStat(today)
    val studyStreak = state.streak(today)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${greeting(now.hour)}, ${state.settings.name.ifBlank { "there" }}.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${today.readable()} · ${blocks.size} blocks planned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (studyStreak > 0) {
                    StreakBadge(studyStreak)
                }
            }
        }

        item {
            TodayHero(
                done = stat.doneBlocks,
                total = blocks.size,
                completion = stat.completion,
                focusMinutes = stat.focusMinutes,
                studyMinutes = stat.studyDoneMinutes,
                streak = studyStreak
            )
        }

        if (blocks.isEmpty()) {
            item {
                AppCard {
                    Text(
                        text = "Start with a plan, not a blank page.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add your first hourly block, or load the ready-made NEET routine. Everything can be edited later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onApplyStarter, shape = RoundedCornerShape(14.dp)) {
                            Text("Use NEET routine")
                        }
                        OutlinedButton(
                            onClick = { onAddBlock(now.hour) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Add a block")
                        }
                    }
                }
            }
        } else {
            item {
                NowCard(
                    blocks = blocks,
                    completed = completed,
                    nowMinute = nowMinute,
                    onToggle = { block -> onToggle(today, block.id) },
                    onOpenFocus = onOpenFocus,
                    onOpenCoach = onOpenCoach,
                    onEdit = onEditBlock
                )
            }
        }

        item {
            SectionHeader(
                title = "Hour by hour",
                trailing = if (blocks.isEmpty()) null
                else "${formatDuration(stat.studyPlannedMinutes)} study planned"
            )
        }

        item {
            HourAgenda(
                blocks = blocks,
                completedIds = completed,
                nowMinute = nowMinute,
                onToggle = { block -> onToggle(today, block.id) },
                onEdit = onEditBlock,
                onAddAt = { minute -> onAddBlock(minute / 60) },
                // Keep the day's useful window in view; the Plan tab has the full 24 hours.
                startHour = (now.hour - 1).coerceIn(0, 23)
            )
        }

        item {
            CoachNudgeCard(onOpenCoach = onOpenCoach)
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Whatshot,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${days}d",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun TodayHero(
    done: Int,
    total: Int,
    completion: Float,
    focusMinutes: Int,
    studyMinutes: Int,
    streak: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(HeroInk, HeroInkLight)))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AccentLabel("Today's flow", Coral)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (total == 0) "Free day" else "$done / $total",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                        color = Cream
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (total == 0) {
                            "Plan a few blocks to give the day shape."
                        } else {
                            "${percent(done, total)}% complete · ${formatDuration(studyMinutes)} study done"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.62f)
                    )
                }
                ProgressRing(
                    progress = if (total == 0) 0f else completion,
                    size = 88.dp,
                    strokeWidth = 8.dp,
                    color = Coral,
                    trackColor = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (total == 0) "—" else "${percent(done, total)}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = Cream
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroMetric("Focus", formatDuration(focusMinutes))
                HeroDivider()
                HeroMetric("Streak", if (streak == 0) "—" else "${streak}d")
                HeroDivider()
                HeroMetric("Planned", "${total} blocks")
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Cream.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Cream.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@Composable
private fun NowCard(
    blocks: List<RoutineBlock>,
    completed: Set<String>,
    nowMinute: Int,
    onToggle: (RoutineBlock) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenCoach: () -> Unit,
    onEdit: (RoutineBlock) -> Unit
) {
    val current = blocks.firstOrNull { nowMinute >= it.startMinute && nowMinute < it.endMinute }
    val next = blocks.firstOrNull { it.startMinute > nowMinute }
    val focusBlock = current ?: next

    AppCard {
        if (focusBlock == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("You're through the plan.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Anything left? Let your coach shape the rest of the evening.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenCoach) { Text("Ask") }
            }
            return@AppCard
        }

        val accent = categoryAccent(focusBlock.category)
        val isCurrent = current != null
        val done = focusBlock.id in completed

        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccentLabel(if (isCurrent) "Happening now" else "Up next")
                    Spacer(Modifier.width(8.dp))
                    TinyDot(accent)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = focusBlock.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatRange(focusBlock.startMinute, focusBlock.endMinute),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                CategoryPill(focusBlock.category)
            }
            IconButton(
                onClick = { onToggle(focusBlock) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (done) Icons.Rounded.CheckCircle
                    else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (done) "Mark as not done" else "Mark as done",
                    tint = if (done) accent else MaterialTheme.colorScheme.outline
                )
            }
        }

        if (isCurrent) {
            Spacer(Modifier.height(12.dp))
            val progress = (nowMinute - focusBlock.startMinute).toFloat() /
                focusBlock.durationMinutes.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = relativeEnd(focusBlock.endMinute - nowMinute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenFocus) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Focus")
                }
                TextButton(onClick = { onEdit(focusBlock) }) { Text("Edit") }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Starts ${relativeMinutes(focusBlock.startMinute - nowMinute)}. Use the gap to set up your desk.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CoachNudgeCard(onOpenCoach: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Need a reset?", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Your coach sees today's blocks, focus minutes and streak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenCoach) { Text("Ask") }
        }
    }
}
