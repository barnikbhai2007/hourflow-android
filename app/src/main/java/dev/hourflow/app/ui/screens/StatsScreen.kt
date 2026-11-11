package dev.hourflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.data.AppState
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.SectionHeader
import dev.hourflow.app.ui.theme.categoryAccent
import dev.hourflow.app.util.DayStat
import dev.hourflow.app.util.completedStudyByCategory
import dev.hourflow.app.util.dayInitial
import dev.hourflow.app.util.dayShort
import dev.hourflow.app.util.dayStat
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.percent
import dev.hourflow.app.util.streak
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun StatsScreen(
    state: AppState,
    now: LocalDateTime,
    onOpenCoach: () -> Unit
) {
    val today = now.toLocalDate()
    val stats = (6 downTo 0).map { offset ->
        state.dayStat(today.minusDays(offset.toLong()))
    }
    val weekPlanned = stats.sumOf { it.plannedBlocks }
    val weekDone = stats.sumOf { it.doneBlocks }
    val weekFocus = stats.sumOf { it.focusMinutes }
    val weekStudy = stats.sumOf { it.studyDoneMinutes }
    val completion = percent(weekDone, weekPlanned)
    val studyStreak = state.streak(today)
    val breakdown = state.completedStudyByCategory(today, 7)
        .entries
        .sortedByDescending { it.value }
    val bestDay = stats.maxByOrNull { it.doneBlocks * 10 + it.focusMinutes }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Progress, not perfection.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Last 7 days · ${stats.first().date.compactLabel()} to ${today.compactLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CheckCircle,
                    label = "Completion",
                    value = "$completion%",
                    detail = "$weekDone of $weekPlanned blocks"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Timer,
                    label = "Focus",
                    value = formatDuration(weekFocus),
                    detail = "this week"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Whatshot,
                    label = "Streak",
                    value = if (studyStreak == 0) "—" else "$studyStreak days",
                    detail = "active days in a row"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.MenuBook,
                    label = "Study logged",
                    value = formatDuration(weekStudy),
                    detail = "completed study blocks"
                )
            }
        }

        item {
            AppCard {
                SectionHeader(title = "Focus this week", trailing = formatDuration(weekFocus))
                Spacer(Modifier.height(16.dp))
                FocusBars(stats = stats, today = today)
            }
        }

        item {
            AppCard {
                SectionHeader(title = "Consistency", trailing = "last 4 weeks")
                Spacer(Modifier.height(14.dp))
                ConsistencyGrid(state = state, today = today)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    listOf(0.12f, 0.3f, 0.55f, 0.85f).forEach { alpha ->
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .size(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "More completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AppCard {
                SectionHeader(
                    title = "Where study time goes",
                    trailing = if (breakdown.isEmpty()) null else "7 days"
                )
                Spacer(Modifier.height(12.dp))
                if (breakdown.isEmpty()) {
                    Text(
                        text = "Complete a study block to see your subject split.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val maxValue = breakdown.first().value.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        breakdown.take(6).forEach { (category, minutes) ->
                            val accent = categoryAccent(category)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatDuration(minutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { minutes.toFloat() / maxValue },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = accent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            CoachInsightCard(
                completion = completion,
                weekFocus = weekFocus,
                weekDone = weekDone,
                bestDay = bestDay,
                topCategory = breakdown.firstOrNull()?.key?.label,
                onOpenCoach = onOpenCoach
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    detail: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusBars(stats: List<DayStat>, today: LocalDate) {
    val maxFocus = stats.maxOfOrNull { it.focusMinutes }?.coerceAtLeast(60) ?: 60
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        stats.forEach { stat ->
            val fraction = stat.focusMinutes.toFloat() / maxFocus
            Column(
                modifier = Modifier.weight(1f).height(140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (stat.focusMinutes > 0) "${stat.focusMinutes}m" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((96 * fraction).coerceAtLeast(3f).dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (stat.date == today) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)
                        )
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = dayInitial(stat.date.dayOfWeek.value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stat.date == today) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConsistencyGrid(state: AppState, today: LocalDate) {
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (day in 1..7) {
                Text(
                    text = dayInitial(day),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        for (week in 3 downTo 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (day in 0..6) {
                    val date = monday.minusWeeks(week.toLong()).plusDays(day.toLong())
                    val stat = state.dayStat(date)
                    val isFuture = date.isAfter(today)
                    val ratio = stat.completion
                    val color = when {
                        isFuture -> Color.Transparent
                        stat.doneBlocks == 0 && stat.focusMinutes == 0 ->
                            MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.secondary.copy(
                            alpha = (0.22f + ratio * 0.72f).coerceIn(0.22f, 0.94f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachInsightCard(
    completion: Int,
    weekFocus: Int,
    weekDone: Int,
    bestDay: DayStat?,
    topCategory: String?,
    onOpenCoach: () -> Unit
) {
    val insight = when {
        weekDone == 0 && weekFocus == 0 ->
            "Nothing is logged in the last 7 days. Do not redesign your whole routine — just finish one small block today. Momentum comes before motivation."
        completion < 45 ->
            "You completed $completion% of planned blocks. Your plan may be heavier than your real day. Trim one block tomorrow and protect the first study session."
        weekFocus == 0 ->
            "$completion% of blocks are done, but no focus sessions are logged. Start a 25-minute timer for the next study block so your effort becomes visible."
        else ->
            "You completed $completion% of blocks and logged ${formatDuration(weekFocus)} of focus. " +
                (bestDay?.let { "${dayShort(it.date.dayOfWeek.value)} was your strongest day. " } ?: "") +
                (topCategory?.let { "$it received the most completed study time. " } ?: "") +
                "Keep the next block small and specific."
    }

    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
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
                Text("Coach read", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenCoach,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Talk it through", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun LocalDate.compactLabel(): String =
    "${dayOfMonth} ${month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
