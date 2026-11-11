package dev.hourflow.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.hourflow.app.data.AiProvider
import dev.hourflow.app.data.AppSettings
import dev.hourflow.app.ui.components.AccentLabel
import dev.hourflow.app.ui.components.AppCard
import dev.hourflow.app.ui.components.BrandMark
import dev.hourflow.app.ui.theme.Coral
import dev.hourflow.app.util.formatDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initial: AppSettings,
    onStart: (AppSettings, Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var goalMinutes by rememberSaveable { mutableIntStateOf(initial.dailyGoalMinutes) }
    var providerName by rememberSaveable { mutableStateOf(initial.provider.name) }
    var apiKey by rememberSaveable { mutableStateOf(initial.apiKey) }
    var model by rememberSaveable { mutableStateOf(initial.model) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var useStarter by rememberSaveable { mutableStateOf(true) }
    val provider = AiProvider.valueOf(providerName)
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "HourFlow",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "NEET edition",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(42.dp))
            Text(
                text = "Own your\nhours.",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "A calm, hourly routine tracker for NEET prep. Plan the day, protect focus, and talk to a coach that can see your real schedule.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Hourly timeline", "Focus timer", "Schedule-aware AI", "Stats").forEach {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            AppCard {
                AccentLabel("First, the basics")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    placeholder = { Text("Barnik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Only used for greetings and as context for your coach.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            AppCard {
                AccentLabel("Daily study target")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A target, not a guilt meter. You can change it anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(240, 360, 480, 600).forEach { option ->
                        FilterChip(
                            selected = goalMinutes == option,
                            onClick = { goalMinutes = option },
                            label = { Text("${option / 60}h") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Currently ${formatDuration(goalMinutes)} of focused study per day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Coral.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .padding(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Coral
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Your AI coach", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Reads your live schedule + stats before every reply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProvider.entries.forEach { item ->
                        FilterChip(
                            selected = provider == item,
                            onClick = {
                                providerName = item.name
                                model = item.defaultModel
                            },
                            label = { Text(item.label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    placeholder = { Text("Paste your key — optional for now") },
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

                Spacer(Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("The key stays on this phone and is sent only to the provider you choose. ")
                        if (provider == AiProvider.GEMINI) {
                            append("Need one? ")
                            pushStyle(SpanStyle(color = Coral, fontWeight = FontWeight.SemiBold))
                            append("Get a Gemini API key")
                            pop()
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                if (provider == AiProvider.GEMINI) {
                    TextButton(
                        onClick = {
                            runCatching {
                                uriHandler.openUri("https://aistudio.google.com/app/apikey")
                            }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Open Google AI Studio")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Start with a NEET routine", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Mon–Sat study blocks plus a Sunday mock and analysis. Every block is editable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useStarter, onCheckedChange = { useStarter = it })
                }
                if (!useStarter) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "You'll start with an empty day and can add blocks yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    onStart(
                        initial.copy(
                            name = name.trim(),
                            dailyGoalMinutes = goalMinutes,
                            provider = provider,
                            apiKey = apiKey.trim(),
                            model = model.trim().ifBlank { provider.defaultModel },
                            baseUrl = provider.defaultBaseUrl,
                            onboarded = true
                        ),
                        useStarter
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Start my day", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "No account. No cloud database. Your routine stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
