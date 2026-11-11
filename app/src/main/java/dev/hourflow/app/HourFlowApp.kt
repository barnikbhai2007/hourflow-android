package dev.hourflow.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hourflow.app.remind.ReminderScheduler
import dev.hourflow.app.ui.AppViewModel
import dev.hourflow.app.ui.components.BlockEditorDialog
import dev.hourflow.app.ui.components.BlockEditorRequest
import dev.hourflow.app.ui.components.HourFlowBottomBar
import dev.hourflow.app.ui.components.HourFlowTopBar
import dev.hourflow.app.ui.screens.CoachScreen
import dev.hourflow.app.ui.screens.FocusScreen
import dev.hourflow.app.ui.screens.OnboardingScreen
import dev.hourflow.app.ui.screens.PlanScreen
import dev.hourflow.app.ui.screens.SettingsScreen
import dev.hourflow.app.ui.screens.StatsScreen
import dev.hourflow.app.ui.screens.TodayScreen

@Composable
fun HourFlowApp(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val aiTest by vm.aiTest.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        val message = toast
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeToast()
        }
    }

    LaunchedEffect(state.blocks, state.settings.remindersEnabled, state.settings.onboarded) {
        ReminderScheduler.reschedule(
            context = context,
            blocks = state.blocks,
            enabled = state.settings.remindersEnabled && state.settings.onboarded
        )
    }

    if (!state.settings.onboarded) {
        OnboardingScreen(
            initial = state.settings,
            onStart = { settings, useStarter ->
                vm.completeOnboarding(settings, useStarter)
            }
        )
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editorRequest by remember { mutableStateOf<BlockEditorRequest?>(null) }

    if (showSettings) {
        SettingsScreen(
            settings = state.settings,
            aiTest = aiTest,
            onBack = {
                showSettings = false
                vm.clearAiTest()
            },
            onSave = { settings -> vm.saveSettings(settings) },
            onTestAi = { settings -> vm.testAiConnection(settings) },
            onClearChat = { vm.clearChat() },
            onReset = {
                vm.resetEverything()
                showSettings = false
            }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HourFlowTopBar(
                now = now,
                onSettings = { showSettings = true }
            )
        },
        bottomBar = {
            HourFlowBottomBar(
                selected = tab,
                onSelect = { tab = it }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (tab) {
                0 -> TodayScreen(
                    state = state,
                    now = now,
                    onAddBlock = { hour ->
                        editorRequest = BlockEditorRequest(
                            block = null,
                            date = now.toLocalDate(),
                            planMode = false,
                            initialHour = hour
                        )
                    },
                    onEditBlock = { block ->
                        editorRequest = BlockEditorRequest(
                            block = block,
                            date = now.toLocalDate(),
                            planMode = false
                        )
                    },
                    onToggle = { date, blockId -> vm.toggleComplete(date, blockId) },
                    onApplyStarter = { vm.applyStarterPlan() },
                    onOpenFocus = { tab = 2 },
                    onOpenCoach = { tab = 4 }
                )

                1 -> PlanScreen(
                    state = state,
                    now = now,
                    onAddBlock = { date, hour ->
                        editorRequest = BlockEditorRequest(
                            block = null,
                            date = date,
                            planMode = true,
                            initialHour = hour
                        )
                    },
                    onEditBlock = { block, date ->
                        editorRequest = BlockEditorRequest(
                            block = block,
                            date = date,
                            planMode = true
                        )
                    },
                    onApplyStarter = { vm.applyStarterPlan() }
                )

                2 -> FocusScreen(
                    state = state,
                    focus = focus,
                    now = now,
                    onSetDuration = vm::setFocusDuration,
                    onSetSubject = vm::setFocusSubject,
                    onStart = vm::startFocus,
                    onPause = vm::pauseFocus,
                    onFinish = vm::finishFocus,
                    onReset = vm::resetFocus,
                    onLogManual = vm::logManualFocus
                )

                3 -> StatsScreen(
                    state = state,
                    now = now,
                    onOpenCoach = { tab = 4 }
                )

                else -> CoachScreen(
                    state = state,
                    now = now,
                    thinking = thinking,
                    onSend = vm::sendCoachMessage,
                    onOpenSettings = { showSettings = true }
                )
            }
        }
    }

    val request = editorRequest
    if (request != null) {
        BlockEditorDialog(
            request = request,
            onDismiss = { editorRequest = null },
            onSave = { block ->
                if (request.block == null) {
                    vm.addBlock(block)
                } else {
                    vm.updateBlock(block)
                }
                editorRequest = null
            },
            onDelete = { block ->
                vm.deleteBlock(block.id)
                editorRequest = null
            }
        )
    }
}
