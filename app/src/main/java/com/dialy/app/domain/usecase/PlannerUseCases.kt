package com.dialy.app.domain.usecase

import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.GratitudeItem
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.domain.model.Reflection
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.domain.model.SelfCareItem
import com.dialy.app.domain.model.TodoItem
import com.dialy.app.domain.repository.PlannerRepository
import kotlinx.coroutines.flow.Flow

class GetPlannerUseCase(private val repository: PlannerRepository) {
    operator fun invoke(date: String): Flow<DailyPlanner?> = repository.getPlannerFlow(date)
}

class SavePlannerUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(planner: DailyPlanner): DailyPlanner = repository.savePlanner(planner)
}

class UpdateFocusUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, focus: String) = repository.updateFocus(date, focus)
}

class UpdatePrioritiesUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, priorities: List<PriorityItem>) =
        repository.updatePriorities(date, priorities)
}

class TodoUseCases(private val repository: PlannerRepository) {
    suspend fun add(item: TodoItem) = repository.addTodo(item)
    suspend fun update(item: TodoItem) = repository.updateTodo(item)
    suspend fun toggle(todoId: String, isCompleted: Boolean) = repository.toggleTodoCompletion(todoId, isCompleted)
    suspend fun delete(todoId: String) = repository.deleteTodo(todoId)
    suspend fun reorder(date: String, orderedIds: List<String>) = repository.reorderTodos(date, orderedIds)
}

class ScheduleUseCases(private val repository: PlannerRepository) {
    suspend fun update(date: String, schedule: List<ScheduleItem>) = repository.updateSchedule(date, schedule)
}

class SelfCareUseCases(private val repository: PlannerRepository) {
    suspend fun update(date: String, selfCare: List<SelfCareItem>) = repository.updateSelfCare(date, selfCare)
    suspend fun toggle(itemId: String, isCompleted: Boolean) = repository.toggleSelfCare(itemId, isCompleted)
}

class UpdateNotesUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, notes: String) = repository.updateNotes(date, notes)
}

class ReminderUseCases(private val repository: PlannerRepository) {
    suspend fun add(item: ReminderItem) = repository.addReminder(item)
    suspend fun update(item: ReminderItem) = repository.updateReminder(item)
    suspend fun toggle(reminderId: String, isCompleted: Boolean) =
        repository.toggleReminderCompletion(reminderId, isCompleted)
    suspend fun delete(reminderId: String) = repository.deleteReminder(reminderId)
}

class UpdateGratitudeUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, gratitude: List<GratitudeItem>) =
        repository.updateGratitude(date, gratitude)
}

class UpdateMoodUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, mood: Mood?) = repository.updateMood(date, mood)
}

class UpdateReflectionUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, reflection: Reflection) = repository.updateReflection(date, reflection)
}

class UpdateDailyReminderUseCase(private val repository: PlannerRepository) {
    suspend operator fun invoke(date: String, reminder: String) = repository.updateDailyReminder(date, reminder)
}
