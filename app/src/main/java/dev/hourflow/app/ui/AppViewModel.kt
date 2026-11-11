package dev.hourflow.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hourflow.app.ai.AiClient
import dev.hourflow.app.ai.CoachContext
import dev.hourflow.app.data.AppRepository
import dev.hourflow.app.data.AppSettings
import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.ChatMessage
import dev.hourflow.app.data.ChatRole
import dev.hourflow.app.data.FocusSession
import dev.hourflow.app.data.RoutineBlock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class FocusUiState(
    val durationMinutes: Int = 25,
    val secondsLeft: Int = 25 * 60,
    val running: Boolean = false,
    val subject: String = "Physics",
    val endAt: Long = 0L,
    val justFinished: Boolean = false
) {
    val totalSeconds: Int get() = durationMinutes * 60
    val elapsedSeconds: Int get() = (totalSeconds - secondsLeft).coerceAtLeast(0)
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

data class AiTestUi(
    val running: Boolean = false,
    val message: String? = null,
    val ok: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    val state: StateFlow<AppState> = repository.state

    private val _now = MutableStateFlow(LocalDateTime.now())
    val now: StateFlow<LocalDateTime> = _now.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private val _focus = MutableStateFlow(FocusUiState())
    val focus: StateFlow<FocusUiState> = _focus.asStateFlow()

    private val _aiTest = MutableStateFlow(AiTestUi())
    val aiTest: StateFlow<AiTestUi> = _aiTest.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private var focusJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                _now.value = LocalDateTime.now()
            }
        }
    }

    // ---------- Blocks ----------

    fun addBlock(block: RoutineBlock) {
        repository.addBlock(block)
        notify("Block added")
    }

    fun updateBlock(block: RoutineBlock) {
        repository.updateBlock(block)
        notify("Block updated")
    }

    fun deleteBlock(blockId: String) {
        repository.deleteBlock(blockId)
        notify("Block deleted")
    }

    fun toggleComplete(date: LocalDate, blockId: String) {
        repository.toggleComplete(date, blockId)
    }

    fun applyStarterPlan() {
        repository.applyStarterPlan(replaceExisting = true)
        notify("NEET routine applied")
    }

    // ---------- Focus ----------

    fun setFocusDuration(minutes: Int) {
        val current = _focus.value
        if (current.running) return
        _focus.value = current.copy(
            durationMinutes = minutes,
            secondsLeft = minutes * 60,
            justFinished = false
        )
    }

    fun setFocusSubject(subject: String) {
        _focus.value = _focus.value.copy(subject = subject)
    }

    fun startFocus() {
        val current = _focus.value
        if (current.running) return
        val seconds = if (current.secondsLeft <= 0) current.durationMinutes * 60 else current.secondsLeft
        val target = System.currentTimeMillis() + seconds * 1000L
        _focus.value = current.copy(
            running = true,
            secondsLeft = seconds,
            endAt = target,
            justFinished = false
        )

        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            while (isActive) {
                val left = ((target - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                if (left != _focus.value.secondsLeft) {
                    _focus.update { it.copy(secondsLeft = left) }
                }
                if (left <= 0) {
                    finishAndLog(completedFully = true)
                    break
                }
                delay(250)
            }
        }
    }

    fun pauseFocus() {
        val current = _focus.value
        if (!current.running) return
        focusJob?.cancel()
        focusJob = null
        val left = ((current.endAt - System.currentTimeMillis()) / 1000L)
            .toInt()
            .coerceIn(0, current.durationMinutes * 60)
        _focus.value = current.copy(running = false, secondsLeft = left, endAt = 0L)
    }

    fun finishFocus() {
        val current = _focus.value
        if (current.justFinished) {
            resetFocus()
            return
        }
        if (current.elapsedSeconds < 30) {
            notify("Focus for at least 30 seconds to log it")
            return
        }
        focusJob?.cancel()
        focusJob = null
        finishAndLog(completedFully = false)
    }

    fun resetFocus() {
        focusJob?.cancel()
        focusJob = null
        _focus.update {
            it.copy(
                running = false,
                secondsLeft = it.durationMinutes * 60,
                endAt = 0L,
                justFinished = false
            )
        }
    }

    fun logManualFocus(subject: String, minutes: Int) {
        if (minutes <= 0) return
        repository.addFocusSession(
            FocusSession(
                startedAt = System.currentTimeMillis(),
                minutes = minutes,
                subject = subject
            )
        )
        notify("Study time logged")
    }

    private fun finishAndLog(completedFully: Boolean) {
        val current = _focus.value
        val minutes = if (completedFully) {
            current.durationMinutes
        } else {
            (current.elapsedSeconds / 60).coerceAtLeast(1)
        }
        repository.addFocusSession(
            FocusSession(
                startedAt = System.currentTimeMillis(),
                minutes = minutes,
                subject = current.subject
            )
        )
        _focus.value = current.copy(
            running = false,
            secondsLeft = 0,
            endAt = 0L,
            justFinished = true
        )
        if (completedFully) {
            notify("Focus session complete · ${minutes}m logged")
        } else {
            notify("${minutes}m logged")
        }
    }

    // ---------- Settings ----------

    fun saveSettings(settings: AppSettings) {
        repository.saveSettings(settings)
        notify("Settings saved")
    }

    fun completeOnboarding(settings: AppSettings, useStarter: Boolean) {
        repository.saveSettings(settings.copy(onboarded = true))
        if (useStarter && repository.state.value.blocks.isEmpty()) {
            repository.applyStarterPlan(replaceExisting = true)
        }
    }

    fun resetEverything() {
        focusJob?.cancel()
        focusJob = null
        _focus.value = FocusUiState()
        _thinking.value = false
        repository.resetEverything()
    }

    fun clearChat() {
        repository.clearChat()
        notify("Coach chat cleared")
    }

    // ---------- AI coach ----------

    fun sendCoachMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _thinking.value) return

        repository.appendMessage(ChatMessage(role = ChatRole.USER, text = text))
        val key = repository.state.value.settings.apiKey.trim()
        if (key.isEmpty()) {
            repository.appendMessage(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    text = "I need an AI key before I can think with you. Open Settings → AI Coach, paste a Google Gemini key (or an OpenAI-compatible key), and tap Save. I'll instantly get your live schedule, focus stats and streak as context.",
                    failed = true
                )
            )
            return
        }

        _thinking.value = true
        viewModelScope.launch {
            val snapshotState = repository.state.value
            val snapshot = CoachContext.snapshot(snapshotState, LocalDateTime.now())
            val system = CoachContext.systemPrompt(snapshotState.settings.name) +
                "\n\nHere is the live data you must use:\n" + snapshot
            val history = snapshotState.messages.takeLast(18)

            AiClient.chat(snapshotState.settings, system, history)
                .onSuccess { reply ->
                    repository.appendMessage(
                        ChatMessage(role = ChatRole.ASSISTANT, text = reply.trim())
                    )
                }
                .onFailure { error ->
                    repository.appendMessage(
                        ChatMessage(
                            role = ChatRole.ASSISTANT,
                            text = "I couldn't reach the model. ${error.message ?: "Unknown error"}",
                            failed = true
                        )
                    )
                }
            _thinking.value = false
        }
    }

    fun testAiConnection(settings: AppSettings) {
        if (_aiTest.value.running) return
        if (settings.apiKey.isBlank()) {
            _aiTest.value = AiTestUi(message = "Add an API key first.", ok = false)
            return
        }
        _aiTest.value = AiTestUi(running = true)
        viewModelScope.launch {
            val result = AiClient.chat(
                settings = settings,
                systemPrompt = "You are a connection test. Reply with a very short confirmation.",
                history = listOf(
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "Connection test. Reply with exactly: Connection OK."
                    )
                )
            )
            _aiTest.value = result.fold(
                onSuccess = { reply ->
                    AiTestUi(
                        message = "Connected · ${settings.model} replied: ${reply.take(80)}",
                        ok = true
                    )
                },
                onFailure = { error ->
                    AiTestUi(message = error.message ?: "Connection failed", ok = false)
                }
            )
        }
    }

    fun clearAiTest() {
        _aiTest.value = AiTestUi()
    }

    // ---------- Small helpers ----------

    fun notify(message: String) {
        _toast.value = message
    }

    fun consumeToast() {
        _toast.value = null
    }
}
