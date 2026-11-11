package dev.hourflow.app.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class Category(val label: String) {
    PHYSICS("Physics"),
    CHEMISTRY("Chemistry"),
    BIOLOGY("Biology"),
    REVISION("Revision"),
    MOCK("Mock test"),
    CLASS("Class"),
    HEALTH("Health"),
    BREAK("Break"),
    SLEEP("Sleep"),
    PERSONAL("Personal");

    val isStudy: Boolean
        get() = when (this) {
            PHYSICS, CHEMISTRY, BIOLOGY, REVISION, MOCK, CLASS -> true
            else -> false
        }
}

data class RoutineBlock(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: Category,
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<Int> = emptySet(),
    val oneOffDate: String? = null,
    val note: String = "",
    val reminder: Boolean = true
) {
    val durationMinutes: Int get() = (endMinute - startMinute).coerceAtLeast(0)

    fun occursOn(date: LocalDate): Boolean {
        val dateText = date.toString()
        return if (oneOffDate != null) {
            oneOffDate == dateText
        } else {
            date.dayOfWeek.value in days
        }
    }
}

data class DayLog(
    val date: String,
    val completed: Set<String> = emptySet()
)

enum class AiProvider(
    val label: String,
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    GEMINI(
        label = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash"
    ),
    OPENAI(
        label = "OpenAI-compatible",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini"
    )
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppSettings(
    val name: String = "",
    val dailyGoalMinutes: Int = 480,
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = AiProvider.GEMINI.defaultModel,
    val baseUrl: String = AiProvider.GEMINI.defaultBaseUrl,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val remindersEnabled: Boolean = true
)

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val at: Long = System.currentTimeMillis(),
    val failed: Boolean = false
)

data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    val minutes: Int,
    val subject: String
) {
    val localDate: LocalDate
        get() = java.time.Instant.ofEpochMilli(startedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
}

data class AppState(
    val blocks: List<RoutineBlock> = emptyList(),
    val logs: Map<String, DayLog> = emptyMap(),
    val sessions: List<FocusSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val settings: AppSettings = AppSettings()
)

fun AppState.blocksFor(date: LocalDate): List<RoutineBlock> =
    blocks.filter { it.occursOn(date) }.sortedBy { it.startMinute }

fun AppState.completedIdsOn(date: LocalDate): Set<String> =
    logs[date.toString()]?.completed ?: emptySet()

fun AppState.isDone(date: LocalDate, blockId: String): Boolean =
    blockId in completedIdsOn(date)

fun AppState.isFocusConfigured(): Boolean =
    settings.apiKey.isNotBlank()
