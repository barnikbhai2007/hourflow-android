package dev.hourflow.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.data.Category
import dev.hourflow.app.data.RoutineBlock
import dev.hourflow.app.ui.theme.categoryAccent
import dev.hourflow.app.util.dayFull
import dev.hourflow.app.util.dayInitial
import dev.hourflow.app.util.formatMinute
import dev.hourflow.app.util.readable
import java.time.LocalDate
import java.util.UUID

data class BlockEditorRequest(
    val block: RoutineBlock?,
    val date: LocalDate,
    val planMode: Boolean,
    val initialHour: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockEditorDialog(
    request: BlockEditorRequest,
    onDismiss: () -> Unit,
    onSave: (RoutineBlock) -> Unit,
    onDelete: (RoutineBlock) -> Unit
) {
    val original = request.block
    val defaultStart = ((request.initialHour ?: 9).coerceIn(0, 23)) * 60

    var title by rememberSaveable(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var category by rememberSaveable(original?.id) {
        mutableStateOf(original?.category ?: Category.PHYSICS)
    }
    var startMinute by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.startMinute ?: defaultStart)
    }
    var endMinute by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.endMinute ?: (defaultStart + 60).coerceAtMost(1440))
    }
    var note by rememberSaveable(original?.id) { mutableStateOf(original?.note.orEmpty()) }
    var reminder by rememberSaveable(original?.id) {
        mutableStateOf(original?.reminder ?: true)
    }
    var oneOff by remember(original?.id) {
        mutableStateOf(original?.oneOffDate != null)
    }
    var days by remember(original?.id) {
        mutableStateOf(
            original?.days
                ?: setOf(request.date.dayOfWeek.value)
        )
    }
    var pickerFor by remember { mutableStateOf<String?>(null) }

    val valid = title.isNotBlank() && endMinute > startMinute
    val repeatsText = if (oneOff) {
        "Only on ${request.date.readable()}"
    } else {
        "Repeats every ${dayFull(request.date.dayOfWeek.value)}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (original == null) "Plan a block" else "Edit block",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Physics — thermodynamics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Category.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (category == item) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField(
                        label = "Starts",
                        value = formatMinute(startMinute),
                        modifier = Modifier.weight(1f)
                    ) { pickerFor = "start" }
                    TimeField(
                        label = "Ends",
                        value = formatMinute(endMinute),
                        modifier = Modifier.weight(1f)
                    ) { pickerFor = "end" }
                }

                if (endMinute <= startMinute) {
                    Text(
                        text = "End time must be after the start time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (request.planMode) {
                    Text(
                        text = "Repeats every ${dayFull(request.date.dayOfWeek.value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Repeat weekly", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = repeatsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !oneOff,
                            onCheckedChange = { repeat ->
                                oneOff = !repeat
                                if (repeat && days.isEmpty()) {
                                    days = setOf(request.date.dayOfWeek.value)
                                }
                            }
                        )
                    }
                }

                if (!oneOff) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (day in 1..7) {
                            val selected = day in days
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        days = if (selected) days - day else days + day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayInitial(day),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (days.isEmpty()) {
                        Text(
                            text = "Pick at least one day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("What exactly should happen in this block?") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "A notification 5 minutes before it starts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }

                if (original != null) {
                    TextButton(
                        onClick = { onDelete(original) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Delete this block",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDays = when {
                        oneOff -> emptySet()
                        days.isEmpty() -> setOf(request.date.dayOfWeek.value)
                        else -> days
                    }
                    onSave(
                        RoutineBlock(
                            id = original?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            category = category,
                            startMinute = startMinute,
                            endMinute = endMinute,
                            days = finalDays,
                            oneOffDate = if (oneOff) request.date.toString() else null,
                            note = note.trim(),
                            reminder = reminder
                        )
                    )
                },
                enabled = valid && (oneOff || days.isNotEmpty())
            ) {
                Text("Save block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    val pickerValue = pickerFor
    if (pickerValue != null) {
        val isStart = pickerValue == "start"
        val initial = if (isStart) startMinute else endMinute
        val state = rememberTimePickerState(
            initialHour = (initial / 60).coerceIn(0, 23),
            initialMinute = if (initial >= 1440) 59 else initial % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickerFor = null },
            title = { Text(if (isStart) "Start time" else "End time") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = state.hour * 60 + state.minute
                        if (isStart) {
                            startMinute = picked
                            if (endMinute <= picked) {
                                endMinute = (picked + 60).coerceAtMost(1440)
                            }
                        } else {
                            endMinute = picked.coerceAtLeast(startMinute + 15)
                        }
                        pickerFor = null
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickerFor = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
