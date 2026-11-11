package dev.hourflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.RoutineBlock
import dev.hourflow.app.ui.components.AccentLabel
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.HourAgenda
import dev.hourflow.app.ui.components.SectionHeader
import dev.hourflow.app.ui.components.TinyDot
import dev.hourflow.app.util.dayFull
import dev.hourflow.app.util.dayInitial
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.formatMinute
import dev.hourflow.app.util.readable
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun PlanScreen(
    state: AppState,
    now: LocalDateTime,
    onAddBlock: (LocalDate, Int?) -> Unit,
    onEditBlock: (RoutineBlock, LocalDate) -> Unit,
    onApplyStarter: () -> Unit
) {
    val today = now.toLocalDate()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    var selectedDay by rememberSaveable { mutableIntStateOf(today.dayOfWeek.value) }
    var showStarterConfirm by rememberSaveable { mutableStateOf(false) }
    val selectedDate = monday.plusDays((selectedDay - 1).toLong())

    val blocks = state.blocks
        .filter { it.oneOffDate == null && selectedDay in it.days }
        .sortedBy { it.startMinute }
    val studyMinutes = blocks.filter { it.category.isStudy }.sumOf { it.durationMinutes }
    val nowMinute = if (selectedDate == today) now.hour * 60 + now.minute else null

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Weekly plan",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The template for every ${dayFull(selectedDay)}. Edit once — it repeats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (day in 1..7) {
                    val date = monday.plusDays((day - 1).toLong())
                    val selected = day == selectedDay
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayInitial(day),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        if (date == today) {
                            TinyDot(
                                color = if (selected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.secondary,
                                size = 4.dp
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        AccentLabel("Your ${dayFull(selectedDay)}", MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (blocks.isEmpty()) "Nothing planned yet" else "${blocks.size} blocks",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (blocks.isEmpty()) {
                                "This is a free template day. Add a block or start with NEET."
                            } else {
                                "${formatDuration(studyMinutes)} of study · first block ${formatMinute(blocks.first().startMinute)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = { onAddBlock(selectedDate, null) },
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Add block")
                    }
                    OutlinedButton(
                        onClick = {
                            if (state.blocks.isEmpty()) onApplyStarter()
                            else showStarterConfirm = true
                        },
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("NEET routine")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Hour by hour",
                trailing = if (blocks.isEmpty()) null else formatDuration(studyMinutes)
            )
        }

        item {
            HourAgenda(
                blocks = blocks,
                completedIds = emptySet(),
                nowMinute = nowMinute,
                onToggle = null,
                onEdit = { block -> onEditBlock(block, selectedDate) },
                onAddAt = { minute -> onAddBlock(selectedDate, minute / 60) },
                showAddHints = true
            )
        }

        item {
            Text(
                text = "One-off blocks planned from Today only appear on that date. Weekly blocks show up every week.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showStarterConfirm) {
        AlertDialog(
            onDismissRequest = { showStarterConfirm = false },
            title = { Text("Replace your weekly plan?") },
            text = {
                Text(
                    "This removes all ${state.blocks.size} existing blocks and loads the NEET starter routine. Completed history stays intact."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStarterConfirm = false
                        onApplyStarter()
                    }
                ) { Text("Load NEET routine") }
            },
            dismissButton = {
                TextButton(onClick = { showStarterConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
