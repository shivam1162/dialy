package com.dialy.app.data.repository

import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.DispatcherProvider
import com.dialy.app.data.local.dao.DailyPlannerDao
import com.dialy.app.data.local.dao.GratitudeDao
import com.dialy.app.data.local.dao.PriorityDao
import com.dialy.app.data.local.dao.ReminderDao
import com.dialy.app.data.local.dao.ScheduleDao
import com.dialy.app.data.local.dao.SelfCareDao
import com.dialy.app.data.local.dao.TodoDao
import com.dialy.app.data.mapper.toDomain
import com.dialy.app.data.mapper.toEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlannerRepositoryImpl(
    private var plannerDao: DailyPlannerDao,
    private var todoDao: TodoDao,
    private var priorityDao: PriorityDao,
    private var scheduleDao: ScheduleDao,
    private var selfCareDao: SelfCareDao,
    private var reminderDao: ReminderDao,
    private var gratitudeDao: GratitudeDao,
    private val dispatchers: DispatcherProvider,
    private val context: android.content.Context? = null
) : PlannerRepository {

    private var activeProfileId: String = ""

    override fun switchProfile(profileId: String) {
        val normalized = if (profileId.isBlank()) "guest" else profileId.trim().lowercase()
        if (activeProfileId != normalized) {
            android.util.Log.d("PlannerRepo", "switchProfile: switching active profile from '$activeProfileId' to '$normalized'")
            activeProfileId = normalized
            if (context != null) {
                val db = com.dialy.app.data.local.database.AppDatabase.getInstance(context, normalized)
                plannerDao = db.dailyPlannerDao()
                todoDao = db.todoDao()
                priorityDao = db.priorityDao()
                scheduleDao = db.scheduleDao()
                selfCareDao = db.selfCareDao()
                reminderDao = db.reminderDao()
                gratitudeDao = db.gratitudeDao()
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun getPlanner(date: String): DailyPlanner? = withContext(dispatchers.io) {
        val plannerEntity = plannerDao.getPlannerByDateOnce(date) ?: return@withContext null
        val todos = todoDao.getTodosByDateOnce(date).map { it.toDomain() }
        val priorities = priorityDao.getPrioritiesByDateOnce(date).map { it.toDomain() }
        val schedule = scheduleDao.getScheduleByDateOnce(date).map { it.toDomain() }
        val selfCare = selfCareDao.getSelfCareByDateOnce(date).map { it.toDomain() }
        val reminders = reminderDao.getRemindersByDateOnce(date).map { it.toDomain() }
        val gratitude = gratitudeDao.getGratitudeByDateOnce(date).map { it.toDomain() }

        plannerEntity.toDomain(
            todos = todos,
            priorities = priorities,
            schedule = schedule,
            selfCare = selfCare,
            reminders = reminders,
            gratitude = gratitude
        )
    }

    override fun getPlannerFlow(date: String): Flow<DailyPlanner?> {
        val plannerFlow = plannerDao.getPlannerByDate(date)
        val todosFlow = todoDao.getTodosByDate(date)
        val prioritiesFlow = priorityDao.getPrioritiesByDate(date)
        val scheduleFlow = scheduleDao.getScheduleByDate(date)
        val selfCareFlow = selfCareDao.getSelfCareByDate(date)
        val remindersFlow = reminderDao.getRemindersByDate(date)
        val gratitudeFlow = gratitudeDao.getGratitudeByDate(date)

        return combine(
            plannerFlow,
            todosFlow,
            prioritiesFlow,
            scheduleFlow,
            selfCareFlow
        ) { planner, todos, priorities, schedule, selfCare ->
            Tuple5(planner, todos, priorities, schedule, selfCare)
        }.combine(
            combine(remindersFlow, gratitudeFlow) { reminders, gratitude ->
                Pair(reminders, gratitude)
            }
        ) { t5, (reminders, gratitude) ->
            val plannerEntity = t5.planner ?: return@combine null
            plannerEntity.toDomain(
                todos = t5.todos.map { it.toDomain() },
                priorities = t5.priorities.map { it.toDomain() },
                schedule = t5.schedule.map { it.toDomain() },
                selfCare = t5.selfCare.map { it.toDomain() },
                reminders = reminders.map { it.toDomain() },
                gratitude = gratitude.map { it.toDomain() }
            )
        }.flowOn(dispatchers.io)
    }

    override fun getAllPlannersFlow(): Flow<List<DailyPlanner>> {
        return combine(
            plannerDao.getAllPlanners()
        ) { (planners) ->
            planners.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getAllPlannersOnce(): List<DailyPlanner> = withContext(dispatchers.io) {
        plannerDao.getAllPlannersOnce().mapNotNull { getPlanner(it.date) }
    }

    override suspend fun updateSyncState(date: String, syncState: String, lastSyncedAt: Long?) = withContext(dispatchers.io) {
        plannerDao.updateSyncState(date, syncState, lastSyncedAt)
    }

    override suspend fun clearAllLocalData() = withContext(dispatchers.io) {
        if (context != null) {
            val db = com.dialy.app.data.local.database.AppDatabase.getInstance(context, activeProfileId)
            db.clearAllTables()
        }
    }

    override suspend fun savePlanner(planner: DailyPlanner): DailyPlanner = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val updatedPlanner = if (planner.syncState == SyncState.SYNCED) {
            planner
        } else {
            planner.copy(
                updatedAt = now,
                syncState = SyncState.SYNC_PENDING
            )
        }

        plannerDao.upsertPlanner(updatedPlanner.toEntity())
        todoDao.deleteTodosByDate(planner.date)
        todoDao.upsertTodos(planner.todos.map { it.toEntity() })

        priorityDao.deletePrioritiesByDate(planner.date)
        priorityDao.upsertPriorities(planner.topPriorities.map { it.toEntity() })

        scheduleDao.deleteScheduleByDate(planner.date)
        scheduleDao.upsertSchedule(planner.schedule.map { it.toEntity() })

        selfCareDao.deleteSelfCareByDate(planner.date)
        selfCareDao.upsertSelfCareItems(planner.selfCare.map { it.toEntity() })

        reminderDao.deleteRemindersByDate(planner.date)
        reminderDao.upsertReminders(planner.dontForget.map { it.toEntity() })

        gratitudeDao.deleteGratitudeByDate(planner.date)
        gratitudeDao.upsertGratitude(planner.gratitude.map { it.toEntity() })

        updatedPlanner
    }

    override suspend fun deletePlanner(date: String): Unit = withContext(dispatchers.io) {
        plannerDao.deletePlanner(date)
        todoDao.deleteTodosByDate(date)
        priorityDao.deletePrioritiesByDate(date)
        scheduleDao.deleteScheduleByDate(date)
        selfCareDao.deleteSelfCareByDate(date)
        reminderDao.deleteRemindersByDate(date)
        gratitudeDao.deleteGratitudeByDate(date)
    }

    override suspend fun plannerExists(date: String): Boolean = withContext(dispatchers.io) {
        plannerDao.plannerExists(date)
    }

    override suspend fun updateFocus(date: String, focus: String): Unit = withContext(dispatchers.io) {
        ensurePlannerCreated(date)
        plannerDao.updateFocus(date, focus, System.currentTimeMillis())
    }

    override suspend fun updatePriorities(date: String, priorities: List<PriorityItem>): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            priorityDao.deletePrioritiesByDate(date)
            priorityDao.upsertPriorities(priorities.map { it.toEntity() })
            plannerDao.updateFocus(date, getFocusOrEmpty(date), System.currentTimeMillis())
        }

    override suspend fun addTodo(item: TodoItem): Unit = withContext(dispatchers.io) {
        ensurePlannerCreated(item.plannerDate)
        todoDao.upsertTodo(item.toEntity())
    }

    override suspend fun updateTodo(item: TodoItem): Unit = withContext(dispatchers.io) {
        todoDao.upsertTodo(item.toEntity())
    }

    override suspend fun toggleTodoCompletion(todoId: String, isCompleted: Boolean): Unit =
        withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            val completedAt = if (isCompleted) now else null
            todoDao.updateCompletion(todoId, isCompleted, completedAt, now)
        }

    override suspend fun deleteTodo(todoId: String): Unit = withContext(dispatchers.io) {
        todoDao.deleteTodoById(todoId)
    }

    override suspend fun reorderTodos(date: String, orderedIds: List<String>): Unit =
        withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            orderedIds.forEachIndexed { index, id ->
                todoDao.updateOrder(id, index, now)
            }
        }

    override suspend fun updateSchedule(date: String, schedule: List<ScheduleItem>): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            scheduleDao.deleteScheduleByDate(date)
            scheduleDao.upsertSchedule(schedule.map { it.toEntity() })
        }

    override suspend fun updateSelfCare(date: String, selfCare: List<SelfCareItem>): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            selfCareDao.deleteSelfCareByDate(date)
            selfCareDao.upsertSelfCareItems(selfCare.map { it.toEntity() })
        }

    override suspend fun toggleSelfCare(itemId: String, isCompleted: Boolean): Unit =
        withContext(dispatchers.io) {
            selfCareDao.toggleSelfCare(itemId, isCompleted, System.currentTimeMillis())
        }

    override suspend fun updateNotes(date: String, notes: String): Unit = withContext(dispatchers.io) {
        ensurePlannerCreated(date)
        plannerDao.updateNotes(date, notes, System.currentTimeMillis())
    }

    override suspend fun addReminder(item: ReminderItem): Unit = withContext(dispatchers.io) {
        ensurePlannerCreated(item.plannerDate)
        reminderDao.upsertReminder(item.toEntity())
    }

    override suspend fun updateReminder(item: ReminderItem): Unit = withContext(dispatchers.io) {
        reminderDao.upsertReminder(item.toEntity())
    }

    override suspend fun toggleReminderCompletion(reminderId: String, isCompleted: Boolean): Unit =
        withContext(dispatchers.io) {
            reminderDao.toggleReminder(reminderId, isCompleted, System.currentTimeMillis())
        }

    override suspend fun deleteReminder(reminderId: String): Unit = withContext(dispatchers.io) {
        reminderDao.deleteReminderById(reminderId)
    }

    override suspend fun updateGratitude(date: String, gratitude: List<GratitudeItem>): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            gratitudeDao.deleteGratitudeByDate(date)
            gratitudeDao.upsertGratitude(gratitude.map { it.toEntity() })
        }

    override suspend fun updateMood(date: String, mood: Mood?): Unit = withContext(dispatchers.io) {
        ensurePlannerCreated(date)
        val moodJson = mood?.let { json.encodeToString(it) }
        plannerDao.updateMood(date, moodJson, System.currentTimeMillis())
    }

    override suspend fun updateReflection(date: String, reflection: Reflection): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            val reflectionJson = json.encodeToString(reflection)
            plannerDao.updateReflection(date, reflectionJson, System.currentTimeMillis())
        }

    override suspend fun updateDailyReminder(date: String, reminder: String): Unit =
        withContext(dispatchers.io) {
            ensurePlannerCreated(date)
            plannerDao.updateDailyReminder(date, reminder, System.currentTimeMillis())
        }

    private suspend fun ensurePlannerCreated(date: String) {
        if (!plannerDao.plannerExists(date)) {
            val defaultPlanner = DailyPlanner.createDefault(date)
            plannerDao.upsertPlanner(defaultPlanner.toEntity())
        }
    }

    private suspend fun getFocusOrEmpty(date: String): String {
        return plannerDao.getPlannerByDateOnce(date)?.focus ?: ""
    }
}

private data class Tuple5<A, B, C, D, E>(
    val planner: A,
    val todos: B,
    val priorities: C,
    val schedule: D,
    val selfCare: E
)
