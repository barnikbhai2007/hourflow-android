package dev.hourflow.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.data.RoutineBlock
import dev.hourflow.app.ui.theme.categoryAccent
import dev.hourflow.app.ui.theme.categoryContainer
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.formatMinute
import dev.hourflow.app.util.formatRange

@Composable
fun HourAgenda(
    blocks: List<RoutineBlock>,
    completedIds: Set<String>,
    nowMinute: Int?,
    onToggle: ((RoutineBlock) -> Unit)?,
    onEdit: (RoutineBlock) -> Unit,
    onAddAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showAddHints: Boolean = true,
    startHour: Int = 0,
    endHour: Int = 23
) {
    val byHour = blocks.groupBy { it.startMinute / 60 }
    val currentHour = nowMinute?.div(60)
    val firstHour = startHour.coerceIn(0, 23)
    val lastHour = endHour.coerceIn(firstHour, 23)

    Column(modifier = modifier.fillMaxWidth()) {
        for (hour in firstHour..lastHour) {
            val hourBlocks = byHour[hour].orEmpty().sortedBy { it.startMinute }
            val current = currentHour == hour

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Column(
                    modifier = Modifier.width(48.dp).padding(top = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatMinute(hour * 60),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (current) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    )
                    if (current) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    if (current) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                            )
                        }
                    }

                    if (hourBlocks.isEmpty()) {
                        val canTap = showAddHints && hour in 5..23
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = canTap) { onAddAt(hour * 60) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = if (canTap) "Open · tap to plan" else "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            hourBlocks.forEach { block ->
                                AgendaBlockCard(
                                    block = block,
                                    done = block.id in completedIds,
                                    current = nowMinute != null && nowMinute in block.startMinute until block.endMinute,
                                    onToggle = onToggle,
                                    onEdit = onEdit
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaBlockCard(
    block: RoutineBlock,
    done: Boolean,
    current: Boolean,
    onToggle: ((RoutineBlock) -> Unit)?,
    onEdit: (RoutineBlock) -> Unit
) {
    val accent = categoryAccent(block.category)
    val container = if (done) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        categoryContainer(block.category)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit(block) },
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(
            width = if (current) 1.5.dp else 1.dp,
            color = if (current) accent.copy(alpha = 0.65f) else accent.copy(alpha = 0.20f)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (done) MaterialTheme.colorScheme.onSurfaceVariant else accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = block.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else accent
                    )
                    if (current) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${block.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatRange(block.startMinute, block.endMinute),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
                if (block.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = block.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onToggle != null) {
                IconButton(
                    onClick = { onToggle(block) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (done) "Mark as not done" else "Mark as done",
                        tint = if (done) accent else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
