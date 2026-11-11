package dev.hourflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hourflow.app.ai.CoachContext
import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.ChatMessage
import dev.hourflow.app.data.ChatRole
import dev.hourflow.app.ui.components.AccentLabel
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.TinyDot
import dev.hourflow.app.ui.theme.Coral
import java.time.LocalDateTime

private val prompts = listOf(
    "Plan the rest of today",
    "I'm behind — reset my evening",
    "What should I revise right now?",
    "Review my week"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(
    state: AppState,
    now: LocalDateTime,
    thinking: Boolean,
    onSend: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val hasKey = state.settings.apiKey.isNotBlank()
    val summary = CoachContext.quickTodaySummary(state, now)
    val messages = state.messages
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var pendingPrompt by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || thinking || !hasKey) return
        onSend(trimmed)
        input = ""
        pendingPrompt = null
    }

    val itemCount = messages.size + if (thinking) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem((itemCount - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AI Coach",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SyncBadge(hasKey = hasKey)
        }

        if (!hasKey) {
            AppCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                padding = PaddingValues(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Connect your coach", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Add a Gemini or OpenAI-compatible key in Settings to start chatting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenSettings) { Text("Open") }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    IntroCard(
                        summary = summary,
                        onPrompt = { if (hasKey) submit(it) else onOpenSettings() }
                    )
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
                if (thinking) {
                    item { ThinkingBubble() }
                }
            }
        }

        if (messages.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prompts) { prompt ->
                    PromptChip(prompt) { submit(prompt) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (hasKey) "Ask about today…" else "Add a key to start…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        enabled = hasKey,
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit(input) })
                    )
                    Spacer(Modifier.width(9.dp))
                    FilledIconButton(
                        onClick = { submit(input) },
                        enabled = hasKey && input.isNotBlank() && !thinking,
                        modifier = Modifier.size(46.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Your coach sees your schedule, focus and streak. Verify NEET facts with NCERT.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncBadge(hasKey: Boolean) {
    val color = if (hasKey) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyDot(color = color, size = 5.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (hasKey) "SYNCED" else "SETUP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = color
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntroCard(
    summary: String,
    onPrompt: (String) -> Unit
) {
    AppCard {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Coral.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "A coach that can see your day.",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Every message includes today's blocks, what you completed, focus sessions, streak and 7/30-day trends. No need to explain your schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TinyDot(MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(18.dp))
        AccentLabel("Try asking")
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { prompt ->
                PromptChip(prompt) { onPrompt(prompt) }
            }
        }
    }
}

@Composable
private fun PromptChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val container = when {
        isUser -> MaterialTheme.colorScheme.primary
        message.failed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        message.failed -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Coral.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = container,
            border = if (!isUser && !message.failed) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else null
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = cleanForDisplay(message.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                if (message.failed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Check Settings → AI Coach",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Coral.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.6.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Reading your day…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun cleanForDisplay(text: String): String = text
    .replace("**", "")
    .replace("__", "")
    .replace("###", "")
    .replace("##", "")
    .replace("`", "")
    .trim()
