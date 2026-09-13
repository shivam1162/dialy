package com.dialy.app.data.mapper

import com.dialy.app.data.local.entities.DailyPlannerEntity
import com.dialy.app.data.local.entities.GratitudeEntity
import com.dialy.app.data.local.entities.PriorityEntity
import com.dialy.app.data.local.entities.ReminderEntity
import com.dialy.app.data.local.entities.ScheduleEntity
import com.dialy.app.data.local.entities.SelfCareEntity
import com.dialy.app.data.local.entities.TodoEntity
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.GratitudeItem
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.domain.model.Reflection
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.domain.model.SelfCareItem
import com.dialy.app.domain.model.TodoItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

// --- Todo Mappers ---
fun TodoEntity.toDomain(): TodoItem = TodoItem(
    id = id,
    plannerDate = plannerDate,
    title = title,
    isCompleted = isCompleted,
    order = orderIndex,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TodoItem.toEntity(): TodoEntity = TodoEntity(
    id = id,
    plannerDate = plannerDate,
    title = title,
    isCompleted = isCompleted,
    orderIndex = order,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Priority Mappers ---
fun PriorityEntity.toDomain(): PriorityItem = PriorityItem(
    id = id,
    plannerDate = plannerDate,
    order = orderIndex,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PriorityItem.toEntity(): PriorityEntity = PriorityEntity(
    id = id,
    plannerDate = plannerDate,
    orderIndex = order,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Schedule Mappers ---
fun ScheduleEntity.toDomain(): ScheduleItem = ScheduleItem(
    id = id,
    plannerDate = plannerDate,
    timeSlot = timeSlot,
    activity = activity,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ScheduleItem.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    plannerDate = plannerDate,
    timeSlot = timeSlot,
    activity = activity,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- SelfCare Mappers ---
fun SelfCareEntity.toDomain(): SelfCareItem = SelfCareItem(
    id = id,
    plannerDate = plannerDate,
    title = title,
    isCompleted = isCompleted,
    order = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SelfCareItem.toEntity(): SelfCareEntity = SelfCareEntity(
    id = id,
    plannerDate = plannerDate,
    title = title,
    isCompleted = isCompleted,
    orderIndex = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Reminder Mappers ---
fun ReminderEntity.toDomain(): ReminderItem = ReminderItem(
    id = id,
    plannerDate = plannerDate,
    text = text,
    isCompleted = isCompleted,
    order = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReminderItem.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    plannerDate = plannerDate,
    text = text,
    isCompleted = isCompleted,
    orderIndex = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Gratitude Mappers ---
fun GratitudeEntity.toDomain(): GratitudeItem = GratitudeItem(
    id = id,
    plannerDate = plannerDate,
    text = text,
    order = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GratitudeItem.toEntity(): GratitudeEntity = GratitudeEntity(
    id = id,
    plannerDate = plannerDate,
    text = text,
    orderIndex = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Planner Core Mappers ---
fun DailyPlanner.toEntity(): DailyPlannerEntity = DailyPlannerEntity(
    date = date,
    focus = focus,
    notes = notes,
    dailyReminder = dailyReminder,
    moodJson = mood?.let { json.encodeToString(it) },
    reflectionJson = json.encodeToString(reflection),
    createdAt = createdAt,
    updatedAt = updatedAt,
    localVersion = localVersion,
    syncState = syncState,
    lastSyncedAt = lastSyncedAt
)

fun DailyPlannerEntity.toDomain(
    todos: List<TodoItem> = emptyList(),
    priorities: List<PriorityItem> = emptyList(),
    schedule: List<ScheduleItem> = emptyList(),
    selfCare: List<SelfCareItem> = emptyList(),
    reminders: List<ReminderItem> = emptyList(),
    gratitude: List<GratitudeItem> = emptyList()
): DailyPlanner {
    val moodObj = moodJson?.let {
        try {
            json.decodeFromString<Mood>(it)
        } catch (e: Exception) {
            null
        }
    }

    val reflectionObj = reflectionJson?.let {
        try {
            json.decodeFromString<Reflection>(it)
        } catch (e: Exception) {
            Reflection()
        }
    } ?: Reflection()

    return DailyPlanner(
        date = date,
        focus = focus,
        topPriorities = priorities,
        todos = todos,
        schedule = schedule,
        selfCare = selfCare,
        notes = notes,
        dontForget = reminders,
        gratitude = gratitude,
        mood = moodObj,
        reflection = reflectionObj,
        dailyReminder = dailyReminder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        localVersion = localVersion,
        syncState = syncState,
        lastSyncedAt = lastSyncedAt
    )
}
