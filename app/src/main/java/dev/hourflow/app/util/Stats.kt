package dev.hourflow.app.util

import dev.hourflow.app.data.AppState
import dev.hourflow.app.data.Category
import dev.hourflow.app.data.blocksFor
import dev.hourflow.app.data.completedIdsOn
import java.time.LocalDate

data class DayStat(
    val date: LocalDate,
    val plannedBlocks: Int,
    val doneBlocks: Int,
    val studyPlannedMinutes: Int,
    val studyDoneMinutes: Int,
    val focusMinutes: Int
) {
    val completion: Float
        get() = if (plannedBlocks == 0) 0f else doneBlocks.toFloat() / plannedBlocks
}

fun AppState.focusMinutesOn(date: LocalDate): Int =
    sessions.filter { it.localDate == date }.sumOf { it.minutes }

fun AppState.dayStat(date: LocalDate): DayStat {
    val blocks = blocksFor(date)
    val done = completedIdsOn(date)
    val studyBlocks = blocks.filter { it.category.isStudy }
    return DayStat(
        date = date,
        plannedBlocks = blocks.size,
        doneBlocks = blocks.count { it.id in done },
        studyPlannedMinutes = studyBlocks.sumOf { it.durationMinutes },
        studyDoneMinutes = studyBlocks.filter { it.id in done }.sumOf { it.durationMinutes },
        focusMinutes = focusMinutesOn(date)
    )
}

fun AppState.streak(today: LocalDate): Int {
    var day = today
    val todayStat = dayStat(today)
    if (todayStat.doneBlocks == 0 && todayStat.focusMinutes < 15) {
        day = today.minusDays(1)
    }

    var count = 0
    while (count < 3650) {
        val stat = dayStat(day)
        val active = stat.doneBlocks > 0 || stat.focusMinutes >= 15
        if (!active) break
        count += 1
        day = day.minusDays(1)
    }
    return count
}

/** Completed study minutes in the last [days] days, grouped by category. */
fun AppState.completedStudyByCategory(today: LocalDate, days: Int): Map<Category, Int> {
    val result = mutableMapOf<Category, Int>()
    for (offset in 0 until days) {
        val date = today.minusDays(offset.toLong())
        val done = completedIdsOn(date)
        blocksFor(date)
            .filter { it.category.isStudy && it.id in done }
            .forEach { block ->
                result[block.category] = (result[block.category] ?: 0) + block.durationMinutes
            }
    }
    return result
}

fun percent(done: Int, total: Int): Int =
    if (total <= 0) 0 else ((done.toFloat() / total) * 100f).toInt().coerceIn(0, 100)
