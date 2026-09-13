package com.dialy.app.domain

import com.dialy.app.core.sync.SyncState
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.GratitudeItem
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.MoodType
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.domain.model.Reflection
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.domain.model.SelfCareItem
import com.dialy.app.domain.model.TodoItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlannerModelTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `createDefault generates correct structure and default sections`() {
        val date = "2026-09-13"
        val planner = DailyPlanner.createDefault(date)

        assertEquals(date, planner.date)
        assertEquals("", planner.focus)
        assertEquals(3, planner.topPriorities.size)
        assertEquals(0, planner.todos.size)
        assertTrue(planner.schedule.isNotEmpty())
        assertEquals("06:00", planner.schedule.first().timeSlot)
        assertEquals(5, planner.selfCare.size)
        assertEquals(SyncState.LOCAL_ONLY, planner.syncState)
        assertEquals(1L, planner.localVersion)
        assertNotNull(planner.reflection)
    }

    @Test
    fun `full DailyPlanner serializes and deserializes accurately`() {
        val date = "2026-09-13"
        val planner = DailyPlanner(
            date = date,
            focus = "Build solid backend architecture",
            topPriorities = listOf(
                PriorityItem(id = "p1", plannerDate = date, order = 1, title = "Setup Room DB", isCompleted = true),
                PriorityItem(id = "p2", plannerDate = date, order = 2, title = "Domain models", isCompleted = true),
                PriorityItem(id = "p3", plannerDate = date, order = 3, title = "Unit tests", isCompleted = false)
            ),
            todos = listOf(
                TodoItem(id = "t1", plannerDate = date, title = "Write DAOs", isCompleted = true, order = 0),
                TodoItem(id = "t2", plannerDate = date, title = "Write Mappers", isCompleted = false, order = 1)
            ),
            schedule = listOf(
                ScheduleItem(id = "s1", plannerDate = date, timeSlot = "09:00", activity = "Sprint planning")
            ),
            selfCare = listOf(
                SelfCareItem(id = "sc1", plannerDate = date, title = "Drink Water", isCompleted = true, order = 0)
            ),
            notes = "Focus on clean architecture and swappable interfaces.",
            dontForget = listOf(
                ReminderItem(id = "r1", plannerDate = date, text = "Test Drive sync", isCompleted = false)
            ),
            gratitude = listOf(
                GratitudeItem(id = "g1", plannerDate = date, text = "Clean code and fast builds")
            ),
            mood = Mood(type = MoodType.VERY_HAPPY, note = "Productive morning", score = 5),
            reflection = Reflection(
                whatWentWell = "Everything compiled nicely",
                whatCanImprove = "Add more edge-case tests",
                proudOf = "Solid domain modeling"
            ),
            dailyReminder = "Consistency over perfection",
            syncState = SyncState.SYNCED
        )

        val serialized = json.encodeToString(planner)
        assertTrue(serialized.contains("Build solid backend architecture"))

        val deserialized = json.decodeFromString<DailyPlanner>(serialized)
        assertEquals(planner, deserialized)
        assertEquals(MoodType.VERY_HAPPY, deserialized.mood?.type)
        assertEquals("Solid domain modeling", deserialized.reflection.proudOf)
    }

    @Test
    fun `Reflection fields are independently maintainable`() {
        val initial = Reflection()
        val step1 = initial.copy(whatWentWell = "Database schema done")
        val step2 = step1.copy(whatCanImprove = "Speed up network queries")
        val step3 = step2.copy(proudOf = "Zero compiler errors")

        assertEquals("Database schema done", step3.whatWentWell)
        assertEquals("Speed up network queries", step3.whatCanImprove)
        assertEquals("Zero compiler errors", step3.proudOf)
    }
}
