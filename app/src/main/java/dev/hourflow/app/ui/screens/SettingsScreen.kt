package dev.hourflow.app.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.hourflow.app.data.AiProvider
import dev.hourflow.app.data.AppSettings
import dev.hourflow.app.data.ThemeMode
import dev.hourflow.app.remind.ReminderScheduler
import dev.hourflow.app.ui.AiTestUi
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.util.formatDuration
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    aiTest: AiTestUi,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onTestAi: (AppSettings) -> Unit,
    onClearChat: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(settings) { mutableStateOf(settings) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var notificationsAllowed by remember {
        mutableStateOf(ReminderScheduler.notificationsAllowed(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = ReminderScheduler.notificationsAllowed(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val cleaned = draft.copy(
                        name = draft.name.trim(),
                        apiKey = draft.apiKey.trim(),
                        model = draft.model.trim().ifBlank { draft.provider.defaultModel },
                        baseUrl = draft.baseUrl.trim().ifBlank { draft.provider.defaultBaseUrl }
                    )
                    draft = cleaned
                    onSave(cleaned)
                }
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            SettingsLabel("You")
            AppCard {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("Name") },
                    placeholder = { Text("Barnik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily study target", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatDuration(draft.dailyGoalMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = draft.dailyGoalMinutes / 60f,
                    onValueChange = {
                        draft = draft.copy(
                            dailyGoalMinutes = (it * 60).roundToInt().coerceIn(60, 720)
                        )
                    },
                    valueRange = 1f..12f,
                    steps = 21
                )
                Text(
                    text = "The target is used by your coach and shown against logged focus time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsLabel("AI coach")
            AppCard {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = draft.provider == provider,
                            onClick = {
                                if (draft.provider != provider) {
                                    draft = draft.copy(
                                        provider = provider,
                                        model = provider.defaultModel,
                                        baseUrl = provider.defaultBaseUrl
                                    )
                                }
                            },
                            label = { Text(provider.label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { draft = draft.copy(apiKey = it) },
                    label = { Text("API key") },
                    placeholder = { Text("Paste your key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff
                                else Icons.Rounded.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.model,
                    onValueChange = { draft = draft.copy(model = it) },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (draft.provider == AiProvider.OPENAI) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft.baseUrl,
                        onValueChange = { draft = draft.copy(baseUrl = it) },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Any OpenAI-compatible HTTPS endpoint ending at /v1 works.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onTestAi(draft) },
                        enabled = !aiTest.running,
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (aiTest.running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.6.dp
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("Testing")
                        } else {
                            Text("Test connection")
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (draft.apiKey.isBlank()) "Key required" else "Uses one tiny request",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (aiTest.message != null) {
                    Spacer(Modifier.height(10.dp))
                    val statusColor = if (aiTest.ok) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = aiTest.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "The key is stored only on this phone. Every coach message sends your schedule snapshot, focus stats and streak to the selected provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsLabel("Reminders")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Block reminders", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "A nudge 5 minutes before each block starts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = draft.remindersEnabled,
                        onCheckedChange = { draft = draft.copy(remindersEnabled = it) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                if (!notificationsAllowed) {
                    Text(
                        text = "Notifications are off for HourFlow. Reminders cannot appear until you enable them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val ok = ReminderScheduler.sendTest(context)
                            Toast.makeText(
                                context,
                                if (ok) "Test reminder sent" else "Notifications are disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Send test")
                    }
                    if (!notificationsAllowed) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text("Turn on")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsLabel("Appearance")
            AppCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.theme == mode,
                            onClick = { draft = draft.copy(theme = mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tap Save to apply the theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsLabel("Data")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Coach history", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Clear the conversation, keep your schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onClearChat) { Text("Clear") }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reset everything", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Blocks, history, focus sessions, chat and settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showReset = true }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "HourFlow 1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Local-first. No account, no analytics, no ads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset everything?") },
            text = {
                Text("This permanently deletes your blocks, completion history, focus sessions, coach chat and settings. It cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReset = false
                        onReset()
                    }
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
    )
}
