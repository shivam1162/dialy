package com.dialy.app.data

import com.dialy.app.core.sync.SyncState
import com.dialy.app.data.local.entities.DailyPlannerEntity
import com.dialy.app.data.local.entities.GratitudeEntity
import com.dialy.app.data.local.entities.PriorityEntity
import com.dialy.app.data.local.entities.ReminderEntity
import com.dialy.app.data.local.entities.ScheduleEntity
import com.dialy.app.data.local.entities.SelfCareEntity
import com.dialy.app.data.local.entities.TodoEntity
import com.dialy.app.data.mapper.toDomain
import com.dialy.app.data.mapper.toEntity
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerMappersTest {

    @Test
    fun `TodoItem bidirectional mapping works correctly`() {
        val domain = TodoItem(
            id = "todo-1",
            plannerDate = "2026-09-13",
            title = "Test item",
            isCompleted = true,
            order = 2,
            completedAt = 123456789L,
            createdAt = 100000000L,
            updatedAt = 123456789L
        )

        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.plannerDate, entity.plannerDate)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.isCompleted, entity.isCompleted)
        assertEquals(domain.order, entity.orderIndex)
        assertEquals(domain.completedAt, entity.completedAt)

        val backToDomain = entity.toDomain()
        assertEquals(domain, backToDomain)
    }

    @Test
    fun `PriorityItem bidirectional mapping works correctly`() {
        val domain = PriorityItem(
            id = "pri-1",
            plannerDate = "2026-09-13",
            order = 1,
            title = "High Priority",
            isCompleted = false
        )

        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.order, entity.orderIndex)

        val backToDomain = entity.toDomain()
        assertEquals(domain, backToDomain)
    }

    @Test
    fun `ScheduleItem bidirectional mapping works correctly`() {
        val domain = ScheduleItem(
            id = "sch-1",
            plannerDate = "2026-09-13",
            timeSlot = "10:00",
            activity = "Meeting",
            isCompleted = true
        )

        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.timeSlot, entity.timeSlot)
        assertEquals(domain.activity, entity.activity)

        val backToDomain = entity.toDomain()
        assertEquals(domain, backToDomain)
    }

    @Test
    fun `DailyPlannerEntity to Domain gracefully handles null or invalid json in embedded fields`() {
        val entity = DailyPlannerEntity(
            date = "2026-09-13",
            focus = "Fallback test",
            notes = "Some notes",
            dailyReminder = "Daily note",
            moodJson = "invalid-json-string",
            reflectionJson = "{invalid json",
            syncState = SyncState.LOCAL_ONLY
        )

        val domain = entity.toDomain()
        assertEquals("Fallback test", domain.focus)
        assertNull(domain.mood)
        assertEquals("", domain.reflection.whatWentWell)
        assertEquals("", domain.reflection.whatCanImprove)
        assertEquals("", domain.reflection.proudOf)
    }

    @Test
    fun `DailyPlanner bidirectional mapping preserves complete state`() {
        val domain = DailyPlanner(
            date = "2026-09-13",
            focus = "Clean architecture",
            topPriorities = listOf(PriorityItem(id = "p1", plannerDate = "2026-09-13", order = 1, title = "P1")),
            todos = listOf(TodoItem(id = "t1", plannerDate = "2026-09-13", title = "T1")),
            schedule = listOf(ScheduleItem(id = "s1", plannerDate = "2026-09-13", timeSlot = "08:00")),
            selfCare = listOf(SelfCareItem(id = "sc1", plannerDate = "2026-09-13", title = "Walk")),
            notes = "Notes text",
            dontForget = listOf(ReminderItem(id = "r1", plannerDate = "2026-09-13", text = "Don't forget this")),
            gratitude = listOf(GratitudeItem(id = "g1", plannerDate = "2026-09-13", text = "Grateful")),
            mood = Mood(type = MoodType.CALM, note = "Peaceful"),
            reflection = Reflection(whatWentWell = "Well", whatCanImprove = "Improve", proudOf = "Proud"),
            dailyReminder = "Remember",
            syncState = SyncState.SYNC_PENDING
        )

        val entity = domain.toEntity()
        val mappedDomain = entity.toDomain(
            todos = domain.todos,
            priorities = domain.topPriorities,
            schedule = domain.schedule,
            selfCare = domain.selfCare,
            reminders = domain.dontForget,
            gratitude = domain.gratitude
        )

        assertEquals(domain.date, mappedDomain.date)
        assertEquals(domain.focus, mappedDomain.focus)
        assertEquals(domain.mood?.type, mappedDomain.mood?.type)
        assertEquals(domain.reflection, mappedDomain.reflection)
        assertEquals(domain.todos, mappedDomain.todos)
        assertEquals(domain.topPriorities, mappedDomain.topPriorities)
    }
}
