package com.dialy.app.domain.repository

import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.GratitudeItem
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.domain.model.Reflection
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.domain.model.SelfCareItem
import com.dialy.app.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

/**
 * UI-agnostic repository interface for Daily Planner operations.
 * Allows swappable local persistence and remote synchronization.
 */
interface PlannerRepository {

    /**
     * Retrieves a single planner by ISO date ("yyyy-MM-dd"). Returns null if not created yet.
     */
    suspend fun getPlanner(date: String): DailyPlanner?

    /**
     * Observes continuous updates for a planner by date.
     */
    fun getPlannerFlow(date: String): Flow<DailyPlanner?>

    /**
     * Observes all planners stored locally.
     */
    fun getAllPlannersFlow(): Flow<List<DailyPlanner>>

    /**
     * Creates or completely overwrites a planner.
     */
    suspend fun savePlanner(planner: DailyPlanner): DailyPlanner

    /**
     * Deletes a planner and all its associated items for a date.
     */
    suspend fun deletePlanner(date: String)

    /**
     * Checks if a planner already exists for the given date.
     */
    suspend fun plannerExists(date: String): Boolean

    // --- Granular Section Update Methods ---

    suspend fun updateFocus(date: String, focus: String)

    suspend fun updatePriorities(date: String, priorities: List<PriorityItem>)

    suspend fun addTodo(item: TodoItem)

    suspend fun updateTodo(item: TodoItem)

    suspend fun toggleTodoCompletion(todoId: String, isCompleted: Boolean)

    suspend fun deleteTodo(todoId: String)

    suspend fun reorderTodos(date: String, orderedIds: List<String>)

    suspend fun updateSchedule(date: String, schedule: List<ScheduleItem>)

    suspend fun updateSelfCare(date: String, selfCare: List<SelfCareItem>)

    suspend fun toggleSelfCare(itemId: String, isCompleted: Boolean)

    suspend fun updateNotes(date: String, notes: String)

    suspend fun addReminder(item: ReminderItem)

    suspend fun updateReminder(item: ReminderItem)

    suspend fun toggleReminderCompletion(reminderId: String, isCompleted: Boolean)

    suspend fun deleteReminder(reminderId: String)

    suspend fun updateGratitude(date: String, gratitude: List<GratitudeItem>)

    suspend fun updateMood(date: String, mood: Mood?)

    suspend fun updateReflection(date: String, reflection: Reflection)

    suspend fun updateDailyReminder(date: String, reminder: String)

    suspend fun updateSyncState(date: String, syncState: String, lastSyncedAt: Long?) {}

    suspend fun getAllPlannersOnce(): List<DailyPlanner> = emptyList()

    /**
     * Switches the active local storage profile (e.g. "guest", "user@gmail.com").
     */
    fun switchProfile(profileId: String) {}
}
