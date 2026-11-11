package dev.hourflow.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val readableDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
private val compactDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

fun LocalDate.readable(): String = format(readableDateFormatter)
fun LocalDate.compact(): String = format(compactDateFormatter)
fun LocalDateTime.clock(): String = format(clockFormatter)

fun formatMinute(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 1440)
    val hour = safe / 60
    val minute = safe % 60
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

fun formatRange(startMinute: Int, endMinute: Int): String =
    "${formatMinute(startMinute)} – ${formatMinute(endMinute)}"

fun formatDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun relativeMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    return when {
        safe < 1 -> "now"
        safe < 60 -> "in ${safe}m"
        else -> "in ${safe / 60}h ${safe % 60}m"
    }
}

fun relativeEnd(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    return when {
        safe < 1 -> "ending now"
        safe < 60 -> "${safe}m left"
        else -> "${safe / 60}h ${safe % 60}m left"
    }
}

fun dayShort(day: Int): String = when (day) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    else -> "Sun"
}

fun dayInitial(day: Int): String = dayShort(day).take(1)

fun dayFull(day: Int): String = when (day) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    else -> "Sunday"
}
