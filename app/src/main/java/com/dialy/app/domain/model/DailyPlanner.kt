package com.dialy.app.domain.model

import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Structured domain model representing a complete Daily Planner entry.
 */
@Serializable
data class DailyPlanner(
    val date: String, // ISO-8601 "yyyy-MM-dd"
    val focus: String = "",
    val topPriorities: List<PriorityItem> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val schedule: List<ScheduleItem> = emptyList(),
    val selfCare: List<SelfCareItem> = emptyList(),
    val notes: String = "",
    val dontForget: List<ReminderItem> = emptyList(),
    val gratitude: List<GratitudeItem> = emptyList(),
    val mood: Mood? = null,
    val reflection: Reflection = Reflection(),
    val dailyReminder: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val localVersion: Long = 1L,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val lastSyncedAt: Long? = null
) {
    companion object {
        /**
         * Creates a fresh DailyPlanner with default schedule slots and initial self-care items.
         */
        fun createDefault(date: String): DailyPlanner {
            val now = System.currentTimeMillis()
            val defaultScheduleHours = listOf(
                "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00"
            )

            val schedule = defaultScheduleHours.map { hour ->
                ScheduleItem(
                    id = IdGenerator.generate(),
                    plannerDate = date,
                    timeSlot = hour,
                    activity = "",
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val defaultSelfCareTitles = listOf(
                "Drink 2L Water",
                "30 min Workout / Walk",
                "Healthy Meals",
                "15 min Reading",
                "7-8h Sleep"
            )

            val selfCare = defaultSelfCareTitles.mapIndexed { index, title ->
                SelfCareItem(
                    id = IdGenerator.generate(),
                    plannerDate = date,
                    title = title,
                    isCompleted = false,
                    order = index,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val defaultPriorities = (1..3).map { order ->
                PriorityItem(
                    id = IdGenerator.generate(),
                    plannerDate = date,
                    order = order,
                    title = "",
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )
            }

            return DailyPlanner(
                date = date,
                focus = "",
                topPriorities = defaultPriorities,
                todos = emptyList(),
                schedule = schedule,
                selfCare = selfCare,
                notes = "",
                dontForget = emptyList(),
                gratitude = emptyList(),
                mood = null,
                reflection = Reflection(),
                dailyReminder = "",
                createdAt = now,
                updatedAt = now,
                localVersion = 1L,
                syncState = SyncState.LOCAL_ONLY,
                lastSyncedAt = null
            )
        }
    }
}
