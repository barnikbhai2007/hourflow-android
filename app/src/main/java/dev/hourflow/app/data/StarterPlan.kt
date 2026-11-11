package dev.hourflow.app.data

/**
 * A realistic NEET study routine. Monday–Saturday is a full study day;
 * Sunday is reserved for a mock test, analysis and a weekly reset.
 */
object StarterPlan {

    fun blocks(): List<RoutineBlock> {
        val list = mutableListOf<RoutineBlock>()

        fun add(
            title: String,
            category: Category,
            startMinute: Int,
            endMinute: Int,
            days: Set<Int>,
            note: String = ""
        ) {
            list += RoutineBlock(
                title = title,
                category = category,
                startMinute = startMinute,
                endMinute = endMinute,
                days = days,
                note = note
            )
        }

        val weekdays = (1..6).toSet()
        val sunday = setOf(7)

        // Monday – Saturday
        add("Wake up & hydrate", Category.HEALTH, 6 * 60, 6 * 60 + 30, weekdays, "Water first, phone later.")
        add("Physics — concepts", Category.PHYSICS, 6 * 60 + 30, 7 * 60 + 30, weekdays, "NCERT line by line, then 5 PYQs.")
        add("Breakfast", Category.BREAK, 7 * 60 + 30, 8 * 60, weekdays)
        add("Chemistry — concepts", Category.CHEMISTRY, 8 * 60, 9 * 60 + 30, weekdays, "Reaction mechanisms + exceptions.")
        add("Short break", Category.BREAK, 9 * 60 + 30, 9 * 60 + 45, weekdays)
        add("Biology — NCERT", Category.BIOLOGY, 9 * 60 + 45, 11 * 60 + 15, weekdays, "Diagrams + active recall.")
        add("Break", Category.BREAK, 11 * 60 + 15, 11 * 60 + 30, weekdays)
        add("Revision — flashcards", Category.REVISION, 11 * 60 + 30, 12 * 60 + 15, weekdays, "Yesterday's weak topics only.")
        add("Lunch", Category.BREAK, 12 * 60 + 15, 13 * 60 + 15, weekdays)
        add("Physics — practice", Category.PHYSICS, 13 * 60 + 15, 14 * 60 + 45, weekdays, "Timed questions, no notes.")
        add("Break", Category.BREAK, 14 * 60 + 45, 15 * 60, weekdays)
        add("Chemistry — practice", Category.CHEMISTRY, 15 * 60, 16 * 60 + 15, weekdays, "Mix easy + previous-year questions.")
        add("Move & reset", Category.HEALTH, 16 * 60 + 15, 17 * 60, weekdays, "Walk, stretch, sunlight. No screens.")
        add("Biology — practice", Category.BIOLOGY, 17 * 60, 18 * 60, weekdays)
        add("Free / buffer", Category.PERSONAL, 18 * 60, 19 * 60, weekdays, "Catch up or rest — protect this gap.")
        add("Dinner", Category.BREAK, 19 * 60, 19 * 60 + 30, weekdays)
        add("Mock or mixed practice", Category.MOCK, 19 * 60 + 30, 20 * 60 + 30, weekdays)
        add("Active recall + plan tomorrow", Category.REVISION, 20 * 60 + 30, 21 * 60, weekdays, "Write 3 weak points.")
        add("Wind down — no screens", Category.PERSONAL, 22 * 60 + 30, 23 * 60, weekdays)
        add("Sleep", Category.SLEEP, 23 * 60, 24 * 60, weekdays)

        // Sunday
        add("Slow morning", Category.BREAK, 7 * 60 + 30, 8 * 60 + 30, sunday)
        add("Full mock test", Category.MOCK, 8 * 60 + 30, 11 * 60, sunday, "Strict timing. One OMR sheet.")
        add("Break", Category.BREAK, 11 * 60, 11 * 60 + 30, sunday)
        add("Mock analysis", Category.REVISION, 11 * 60 + 30, 13 * 60, sunday, "Classify every mistake: silly / concept / speed.")
        add("Lunch", Category.BREAK, 13 * 60, 14 * 60, sunday)
        add("Weak-topic repair", Category.REVISION, 14 * 60, 15 * 60, sunday, "Only the three weakest topics.")
        add("Rest & recharge", Category.PERSONAL, 15 * 60, 18 * 60, sunday, "Guilt-free rest. It is part of the plan.")
        add("Weekly review + next week plan", Category.REVISION, 18 * 60, 19 * 60, sunday)
        add("Dinner", Category.BREAK, 19 * 60, 19 * 60 + 30, sunday)
        add("Wind down — no screens", Category.PERSONAL, 22 * 60 + 30, 23 * 60, sunday)
        add("Sleep", Category.SLEEP, 23 * 60, 24 * 60, sunday)

        return list
    }
}
