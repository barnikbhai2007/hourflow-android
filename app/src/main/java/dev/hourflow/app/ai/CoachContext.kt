package dev.hourflow.app.ai

import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.Category
import dev.hourflow.app.data.blocksFor
import dev.hourflow.app.data.completedIdsOn
import dev.hourflow.app.util.clock
import dev.hourflow.app.util.dayShort
import dev.hourflow.app.util.dayStat
import dev.hourflow.app.util.focusMinutesOn
import dev.hourflow.app.util.formatDuration
import dev.hourflow.app.util.formatMinute
import dev.hourflow.app.util.formatRange
import dev.hourflow.app.util.percent
import dev.hourflow.app.util.streak
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CoachContext {

    fun systemPrompt(settingsName: String): String {
        val student = settingsName.ifBlank { "the student" }
        return """
            You are HourFlow, a calm and sharp daily-routine coach for $student, a NEET aspirant in India.
            You can see a live snapshot of their schedule, completion, focus sessions and weekly stats.
            Your job is to help them study consistently, protect sleep and breaks, and prepare for NEET.

            Rules:
            - Be specific and practical. Reference real block names and clock times from the snapshot.
            - Give one clear next action first, then at most 3 short supporting points.
            - Keep normal replies under 180 words. If asked for a full plan, use a compact time-blocked list.
            - NEET guidance: NCERT first, active recall, spaced revision, PYQs, timed mocks, and error analysis.
            - Do not invent blocks, scores, marks or stats that are not in the snapshot.
            - If they are behind, help them reset the rest of the day instead of guilt-tripping them.
            - Never suggest skipping sleep, meals, or exercise to study more.
            - Use 24-hour times. Use simple language. No markdown tables, no long preambles.
            - If a question needs a doctor or mental-health professional, say so briefly and kindly.
        """.trimIndent()
    }

    fun snapshot(state: AppState, now: LocalDateTime): String {
        val today = now.toLocalDate()
        val nowMinute = now.hour * 60 + now.minute
        val todayBlocks = state.blocksFor(today)
        val done = state.completedIdsOn(today)
        val settings = state.settings
        val zone = ZoneId.systemDefault().id
        val timeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm")

        val studyBlocks = todayBlocks.filter { it.category.isStudy }
        val studyDoneMinutes = studyBlocks.filter { it.id in done }.sumOf { it.durationMinutes }
        val studyPlannedMinutes = studyBlocks.sumOf { it.durationMinutes }
        val focusToday = state.focusMinutesOn(today)
        val todayFocus = state.sessions.filter { it.localDate == today }

        val current = todayBlocks.firstOrNull { nowMinute >= it.startMinute && nowMinute < it.endMinute }
        val next = todayBlocks.firstOrNull { it.startMinute > nowMinute }

        val builder = StringBuilder()
        builder.appendLine("LIVE SNAPSHOT")
        builder.appendLine("Student: ${settings.name.ifBlank { "Unknown" }}")
        builder.appendLine("Now: ${now.format(timeFormatter)} ($zone)")
        builder.appendLine("Daily study target: ${formatDuration(settings.dailyGoalMinutes)}")
        builder.appendLine("Current streak: ${state.streak(today)} day(s)")
        builder.appendLine()

        builder.appendLine("TODAY — ${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} ${today}")
        if (todayBlocks.isEmpty()) {
            builder.appendLine("No blocks planned today.")
        } else {
            todayBlocks.forEach { block ->
                val status = if (block.id in done) "[done]" else "[ ]"
                builder.appendLine(
                    "$status ${formatRange(block.startMinute, block.endMinute)} · ${block.title} " +
                        "(${block.category.label})" +
                        if (block.note.isNotBlank()) " — note: ${block.note}" else ""
                )
            }
        }
        builder.appendLine()
        builder.appendLine(
            "Today so far: ${done.count { id -> todayBlocks.any { it.id == id } }}/${todayBlocks.size} blocks done " +
                "(${percent(done.count { id -> todayBlocks.any { it.id == id } }, todayBlocks.size)}%). " +
                "Study blocks completed: ${formatDuration(studyDoneMinutes)} of ${formatDuration(studyPlannedMinutes)} planned. " +
                "Focus timer: ${formatDuration(focusToday)} today."
        )
        if (todayFocus.isNotEmpty()) {
            val bySubject = todayFocus.groupBy { it.subject }.mapValues { entry -> entry.value.sumOf { it.minutes } }
            builder.appendLine("Focus sessions today: " + bySubject.entries.joinToString { "${it.key} ${formatDuration(it.value)}" })
        }
        builder.appendLine(
            when {
                current != null -> "Current block: ${current.title} until ${formatMinute(current.endMinute)}."
                next != null -> "Next block: ${next.title} at ${formatMinute(next.startMinute)}."
                else -> "No more blocks today."
            }
        )
        builder.appendLine()

        val tomorrow = today.plusDays(1)
        val tomorrowBlocks = state.blocksFor(tomorrow)
        if (tomorrowBlocks.isNotEmpty()) {
            builder.appendLine("TOMORROW (${dayShort(tomorrow.dayOfWeek.value)})")
            builder.appendLine(tomorrowBlocks.take(14).joinToString("; ") {
                "${formatMinute(it.startMinute)} ${it.title} (${it.category.label})"
            } + if (tomorrowBlocks.size > 14) "; …" else "")
            builder.appendLine()
        }

        builder.appendLine("LAST 7 DAYS")
        var weekPlanned = 0
        var weekDone = 0
        var weekFocus = 0
        for (offset in 6 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val stat = state.dayStat(date)
            weekPlanned += stat.plannedBlocks
            weekDone += stat.doneBlocks
            weekFocus += stat.focusMinutes
            builder.appendLine(
                "- ${dayShort(date.dayOfWeek.value)} ${date}: ${stat.doneBlocks}/${stat.plannedBlocks} blocks " +
                    "(${percent(stat.doneBlocks, stat.plannedBlocks)}%), study ${formatDuration(stat.studyDoneMinutes)}, " +
                    "focus ${formatDuration(stat.focusMinutes)}"
            )
        }
        builder.appendLine(
            "7-day totals: $weekDone/$weekPlanned blocks (${percent(weekDone, weekPlanned)}%), " +
                "${formatDuration(weekFocus)} of focus timer."
        )

        // A quick 30-day read so the coach can spot trends without a huge prompt.
        var monthFocus = 0
        var monthStudy = 0
        for (offset in 0 until 30) {
            val date = today.minusDays(offset.toLong())
            monthFocus += state.focusMinutesOn(date)
            monthStudy += state.dayStat(date).studyDoneMinutes
        }
        builder.appendLine(
            "30-day totals: ${formatDuration(monthFocus)} focus, ${formatDuration(monthStudy)} completed study blocks."
        )

        val studyByCategory = todayBlocks
            .filter { it.category.isStudy }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
        if (studyByCategory.isNotEmpty()) {
            builder.appendLine(
                "Today's study split: " + studyByCategory.entries.joinToString { "${it.key.label} ${formatDuration(it.value)}" }
            )
        }

        builder.appendLine()
        builder.appendLine("Use this snapshot as ground truth. Do not ask for information that is already above.")
        return builder.toString()
    }

    fun quickTodaySummary(state: AppState, now: LocalDateTime): String {
        val today = now.toLocalDate()
        val blocks = state.blocksFor(today)
        val done = state.completedIdsOn(today)
        if (blocks.isEmpty()) return "No blocks planned today"
        val count = blocks.count { it.id in done }
        return "$count of ${blocks.size} blocks done · ${formatDuration(state.focusMinutesOn(today))} focus"
    }

    fun categoryLabel(category: Category): String = category.label
}
