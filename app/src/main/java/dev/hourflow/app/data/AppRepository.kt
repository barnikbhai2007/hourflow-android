package dev.hourflow.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Single source of truth for HourFlow. Everything lives on-device in
 * SharedPreferences as JSON — no account, no backend, no hidden sync.
 */
class AppRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun addBlock(block: RoutineBlock) = mutate { current ->
        current.copy(blocks = current.blocks + block)
    }

    fun updateBlock(block: RoutineBlock) = mutate { current ->
        current.copy(blocks = current.blocks.map { if (it.id == block.id) block else it })
    }

    fun deleteBlock(blockId: String) = mutate { current ->
        current.copy(blocks = current.blocks.filterNot { it.id == blockId })
    }

    fun toggleComplete(date: LocalDate, blockId: String) = mutate { current ->
        val key = date.toString()
        val log = current.logs[key] ?: DayLog(key)
        val newCompleted = if (blockId in log.completed) {
            log.completed - blockId
        } else {
            log.completed + blockId
        }
        current.copy(logs = current.logs + (key to log.copy(completed = newCompleted)))
    }

    fun addFocusSession(session: FocusSession) = mutate { current ->
        current.copy(sessions = (current.sessions + session).takeLast(MAX_SESSIONS))
    }

    fun appendMessage(message: ChatMessage) = mutate { current ->
        current.copy(messages = (current.messages + message).takeLast(MAX_MESSAGES))
    }

    fun clearChat() = mutate { current -> current.copy(messages = emptyList()) }

    fun saveSettings(settings: AppSettings) = mutate { current ->
        current.copy(settings = settings)
    }

    fun applyStarterPlan(replaceExisting: Boolean = true) = mutate { current ->
        val starter = StarterPlan.blocks()
        current.copy(
            blocks = if (replaceExisting) starter else current.blocks + starter
        )
    }

    fun resetEverything() {
        prefs.edit().clear().apply()
        _state.value = AppState()
    }

    private fun mutate(transform: (AppState) -> AppState) {
        val next = transform(_state.value)
        _state.value = next
        persist(next)
    }

    private fun persist(state: AppState) {
        val blocksJson = JSONArray()
        state.blocks.forEach { blocksJson.put(it.toJson()) }

        val logsJson = JSONObject()
        state.logs.forEach { (date, log) ->
            logsJson.put(date, JSONArray().apply { log.completed.forEach { put(it) } })
        }

        val sessionsJson = JSONArray()
        state.sessions.forEach { sessionsJson.put(it.toJson()) }

        val messagesJson = JSONArray()
        state.messages.forEach { messagesJson.put(it.toJson()) }

        prefs.edit()
            .putString(KEY_BLOCKS, blocksJson.toString())
            .putString(KEY_LOGS, logsJson.toString())
            .putString(KEY_SESSIONS, sessionsJson.toString())
            .putString(KEY_MESSAGES, messagesJson.toString())
            .putString(KEY_SETTINGS, state.settings.toJson().toString())
            .apply()
    }

    private fun read(): AppState {
        val blocks = runCatching {
            val array = JSONArray(prefs.getString(KEY_BLOCKS, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getJSONObject(index).toBlock() }.getOrNull()
            }
        }.getOrDefault(emptyList())

        val logs = runCatching {
            val obj = JSONObject(prefs.getString(KEY_LOGS, "{}") ?: "{}")
            val result = mutableMapOf<String, DayLog>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val date = keys.next()
                val completed = mutableSetOf<String>()
                val array = obj.optJSONArray(date) ?: JSONArray()
                for (index in 0 until array.length()) {
                    completed += array.optString(index)
                }
                result[date] = DayLog(date = date, completed = completed)
            }
            result
        }.getOrDefault(emptyMap())

        val sessions = runCatching {
            val array = JSONArray(prefs.getString(KEY_SESSIONS, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getJSONObject(index).toFocusSession() }.getOrNull()
            }
        }.getOrDefault(emptyList())

        val messages = runCatching {
            val array = JSONArray(prefs.getString(KEY_MESSAGES, "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getJSONObject(index).toMessage() }.getOrNull()
            }
        }.getOrDefault(emptyList())

        val settings = runCatching {
            val obj = prefs.getString(KEY_SETTINGS, null)
                ?.let { JSONObject(it) }
                ?: JSONObject()
            obj.toSettings()
        }.getOrDefault(AppSettings())

        return AppState(
            blocks = blocks,
            logs = logs,
            sessions = sessions,
            messages = messages,
            settings = settings
        )
    }

    companion object {
        private const val PREFS_NAME = "hourflow_state_v1"
        private const val KEY_BLOCKS = "blocks"
        private const val KEY_LOGS = "logs"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_MESSAGES = "messages"
        private const val KEY_SETTINGS = "settings"
        private const val MAX_SESSIONS = 5000
        private const val MAX_MESSAGES = 200
    }
}

private fun RoutineBlock.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("category", category.name)
    put("startMinute", startMinute)
    put("endMinute", endMinute)
    put("days", JSONArray().apply { days.sorted().forEach { put(it) } })
    put("oneOffDate", oneOffDate ?: JSONObject.NULL)
    put("note", note)
    put("reminder", reminder)
}

private fun JSONObject.toBlock(): RoutineBlock {
    val days = mutableSetOf<Int>()
    val daysArray = optJSONArray("days") ?: JSONArray()
    for (index in 0 until daysArray.length()) {
        val day = daysArray.optInt(index)
        if (day in 1..7) days += day
    }
    val category = runCatching {
        Category.valueOf(optString("category", Category.PERSONAL.name))
    }.getOrDefault(Category.PERSONAL)
    val oneOff = optString("oneOffDate")
        .takeIf { it.isNotBlank() && it != "null" }

    return RoutineBlock(
        id = optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
        title = optString("title", "Untitled block"),
        category = category,
        startMinute = optInt("startMinute", 9 * 60).coerceIn(0, 1440),
        endMinute = optInt("endMinute", 10 * 60).coerceIn(0, 1440),
        days = days,
        oneOffDate = oneOff,
        note = optString("note"),
        reminder = optBoolean("reminder", true)
    )
}

private fun FocusSession.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("startedAt", startedAt)
    put("minutes", minutes)
    put("subject", subject)
}

private fun JSONObject.toFocusSession(): FocusSession = FocusSession(
    id = optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
    startedAt = optLong("startedAt", System.currentTimeMillis()),
    minutes = optInt("minutes", 0).coerceAtLeast(0),
    subject = optString("subject", "Study")
)

private fun ChatMessage.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("role", role.name)
    put("text", text)
    put("at", at)
    put("failed", failed)
}

private fun JSONObject.toMessage(): ChatMessage {
    val role = runCatching {
        ChatRole.valueOf(optString("role", ChatRole.ASSISTANT.name))
    }.getOrDefault(ChatRole.ASSISTANT)
    return ChatMessage(
        id = optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
        role = role,
        text = optString("text"),
        at = optLong("at", System.currentTimeMillis()),
        failed = optBoolean("failed", false)
    )
}

private fun AppSettings.toJson(): JSONObject = JSONObject().apply {
    put("name", name)
    put("dailyGoalMinutes", dailyGoalMinutes)
    put("provider", provider.name)
    put("apiKey", apiKey)
    put("model", model)
    put("baseUrl", baseUrl)
    put("theme", theme.name)
    put("onboarded", onboarded)
    put("remindersEnabled", remindersEnabled)
}

private fun JSONObject.toSettings(): AppSettings {
    val provider = runCatching {
        AiProvider.valueOf(optString("provider", AiProvider.GEMINI.name))
    }.getOrDefault(AiProvider.GEMINI)
    val theme = runCatching {
        ThemeMode.valueOf(optString("theme", ThemeMode.SYSTEM.name))
    }.getOrDefault(ThemeMode.SYSTEM)
    val model = optString("model").takeIf { it.isNotBlank() } ?: provider.defaultModel
    val baseUrl = optString("baseUrl").takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl

    return AppSettings(
        name = optString("name"),
        dailyGoalMinutes = optInt("dailyGoalMinutes", 480).coerceIn(30, 900),
        provider = provider,
        apiKey = optString("apiKey"),
        model = model,
        baseUrl = baseUrl,
        theme = theme,
        onboarded = optBoolean("onboarded", false),
        remindersEnabled = optBoolean("remindersEnabled", true)
    )
}
