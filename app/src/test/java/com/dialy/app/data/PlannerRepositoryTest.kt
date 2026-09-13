package com.dialy.app.data

import com.dialy.app.core.TestDispatcherProvider
import com.dialy.app.core.sync.SyncState
import com.dialy.app.data.local.dao.DailyPlannerDao
import com.dialy.app.data.local.dao.GratitudeDao
import com.dialy.app.data.local.dao.PriorityDao
import com.dialy.app.data.local.dao.ReminderDao
import com.dialy.app.data.local.dao.ScheduleDao
import com.dialy.app.data.local.dao.SelfCareDao
import com.dialy.app.data.local.dao.TodoDao
import com.dialy.app.data.local.entities.DailyPlannerEntity
import com.dialy.app.data.local.entities.GratitudeEntity
import com.dialy.app.data.local.entities.PriorityEntity
import com.dialy.app.data.local.entities.ReminderEntity
import com.dialy.app.data.local.entities.ScheduleEntity
import com.dialy.app.data.local.entities.SelfCareEntity
import com.dialy.app.data.local.entities.TodoEntity
import com.dialy.app.data.repository.PlannerRepositoryImpl
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.MoodType
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.domain.model.Reflection
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.domain.model.SelfCareItem
import com.dialy.app.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlannerRepositoryTest {

    private lateinit var fakePlannerDao: FakeDailyPlannerDao
    private lateinit var fakeTodoDao: FakeTodoDao
    private lateinit var fakePriorityDao: FakePriorityDao
    private lateinit var fakeScheduleDao: FakeScheduleDao
    private lateinit var fakeSelfCareDao: FakeSelfCareDao
    private lateinit var fakeReminderDao: FakeReminderDao
    private lateinit var fakeGratitudeDao: FakeGratitudeDao

    private lateinit var repository: PlannerRepositoryImpl

    @Before
    fun setUp() {
        fakePlannerDao = FakeDailyPlannerDao()
        fakeTodoDao = FakeTodoDao()
        fakePriorityDao = FakePriorityDao()
        fakeScheduleDao = FakeScheduleDao()
        fakeSelfCareDao = FakeSelfCareDao()
        fakeReminderDao = FakeReminderDao()
        fakeGratitudeDao = FakeGratitudeDao()

        repository = PlannerRepositoryImpl(
            plannerDao = fakePlannerDao,
            todoDao = fakeTodoDao,
            priorityDao = fakePriorityDao,
            scheduleDao = fakeScheduleDao,
            selfCareDao = fakeSelfCareDao,
            reminderDao = fakeReminderDao,
            gratitudeDao = fakeGratitudeDao,
            dispatchers = TestDispatcherProvider()
        )
    }

    @Test
    fun `getPlanner returns null when planner does not exist`() = runTest {
        val result = repository.getPlanner("2026-09-13")
        assertNull(result)
    }

    @Test
    fun `savePlanner saves all sections and retrieves complete model`() = runTest {
        val date = "2026-09-13"
        val planner = DailyPlanner(
            date = date,
            focus = "Finish Phase C",
            topPriorities = listOf(
                PriorityItem(id = "p1", plannerDate = date, order = 1, title = "Write Repository")
            ),
            todos = listOf(
                TodoItem(id = "t1", plannerDate = date, title = "Task 1", isCompleted = false)
            ),
            schedule = listOf(
                ScheduleItem(id = "s1", plannerDate = date, timeSlot = "08:00", activity = "Breakfast")
            ),
            selfCare = listOf(
                SelfCareItem(id = "sc1", plannerDate = date, title = "Workout", isCompleted = true)
            ),
            notes = "Test notes",
            dontForget = listOf(
                ReminderItem(id = "r1", plannerDate = date, text = "Test reminder")
            ),
            mood = Mood(type = MoodType.HAPPY),
            reflection = Reflection(whatWentWell = "Fast progress")
        )

        repository.savePlanner(planner)

        val retrieved = repository.getPlanner(date)
        assertNotNull(retrieved)
        assertEquals(date, retrieved?.date)
        assertEquals("Finish Phase C", retrieved?.focus)
        assertEquals(1, retrieved?.topPriorities?.size)
        assertEquals(1, retrieved?.todos?.size)
        assertEquals(1, retrieved?.schedule?.size)
        assertEquals(1, retrieved?.selfCare?.size)
        assertEquals("Test notes", retrieved?.notes)
        assertEquals(MoodType.HAPPY, retrieved?.mood?.type)
        assertEquals("Fast progress", retrieved?.reflection?.whatWentWell)
    }

    @Test
    fun `addTodo and toggleTodoCompletion update state properly`() = runTest {
        val date = "2026-09-13"
        val todo = TodoItem(id = "t1", plannerDate = date, title = "Complete tests", isCompleted = false)

        repository.addTodo(todo)
        repository.toggleTodoCompletion("t1", true)

        val retrieved = repository.getPlanner(date)
        assertNotNull(retrieved)
        val savedTodo = retrieved?.todos?.firstOrNull { it.id == "t1" }
        assertNotNull(savedTodo)
        assertTrue(savedTodo!!.isCompleted)
        assertNotNull(savedTodo.completedAt)
    }

    @Test
    fun `deleteTodo removes item`() = runTest {
        val date = "2026-09-13"
        val todo = TodoItem(id = "t1", plannerDate = date, title = "Temporary task")
        repository.addTodo(todo)

        repository.deleteTodo("t1")

        val retrieved = repository.getPlanner(date)
        assertTrue(retrieved?.todos?.none { it.id == "t1" } == true)
    }

    @Test
    fun `reorderTodos updates item ordering`() = runTest {
        val date = "2026-09-13"
        repository.addTodo(TodoItem(id = "t1", plannerDate = date, title = "First", order = 0))
        repository.addTodo(TodoItem(id = "t2", plannerDate = date, title = "Second", order = 1))

        repository.reorderTodos(date, listOf("t2", "t1"))

        val todos = fakeTodoDao.getTodosByDateOnce(date)
        assertEquals("t2", todos[0].id)
        assertEquals(0, todos[0].orderIndex)
        assertEquals("t1", todos[1].id)
        assertEquals(1, todos[1].orderIndex)
    }

    @Test
    fun `updateFocus and updateNotes create planner automatically if not present`() = runTest {
        val date = "2026-09-14"
        assertFalse(repository.plannerExists(date))

        repository.updateFocus(date, "New focus for tomorrow")
        assertTrue(repository.plannerExists(date))

        val planner = repository.getPlanner(date)
        assertEquals("New focus for tomorrow", planner?.focus)

        repository.updateNotes(date, "New note content")
        val updated = repository.getPlanner(date)
        assertEquals("New note content", updated?.notes)
    }

    @Test
    fun `updateMood and updateReflection update fields independently`() = runTest {
        val date = "2026-09-15"
        repository.updateMood(date, Mood(type = MoodType.CALM, note = "Relaxing day"))

        var planner = repository.getPlanner(date)
        assertEquals(MoodType.CALM, planner?.mood?.type)
        assertEquals("Relaxing day", planner?.mood?.note)

        val reflection = Reflection(
            whatWentWell = "Handled all tasks",
            whatCanImprove = "Drink more water",
            proudOf = "Great test coverage"
        )
        repository.updateReflection(date, reflection)

        planner = repository.getPlanner(date)
        assertEquals(MoodType.CALM, planner?.mood?.type)
        assertEquals("Handled all tasks", planner?.reflection?.whatWentWell)
        assertEquals("Drink more water", planner?.reflection?.whatCanImprove)
        assertEquals("Great test coverage", planner?.reflection?.proudOf)
    }

    @Test
    fun `getPlannerFlow emits changes reactively`() = runTest {
        val date = "2026-09-16"
        val planner = DailyPlanner.createDefault(date)
        repository.savePlanner(planner)

        val flowValue = repository.getPlannerFlow(date).first()
        assertNotNull(flowValue)
        assertEquals(date, flowValue?.date)
    }
}

// =========================================================================
// Fake DAOs for Unit Testing
// =========================================================================

class FakeDailyPlannerDao : DailyPlannerDao {
    private val plannersMap = mutableMapOf<String, DailyPlannerEntity>()
    private val flow = MutableStateFlow<Map<String, DailyPlannerEntity>>(emptyMap())

    override fun getPlannerByDate(date: String): Flow<DailyPlannerEntity?> {
        return flow.map { it[date] }
    }

    override suspend fun getPlannerByDateOnce(date: String): DailyPlannerEntity? {
        return plannersMap[date]
    }

    override fun getAllPlanners(): Flow<List<DailyPlannerEntity>> {
        return flow.map { it.values.toList() }
    }

    override suspend fun getAllPlannersOnce(): List<DailyPlannerEntity> {
        return plannersMap.values.toList()
    }

    override suspend fun upsertPlanner(planner: DailyPlannerEntity) {
        plannersMap[planner.date] = planner
        flow.value = plannersMap.toMap()
    }

    override suspend fun deletePlanner(date: String) {
        plannersMap.remove(date)
        flow.value = plannersMap.toMap()
    }

    override suspend fun plannerExists(date: String): Boolean {
        return plannersMap.containsKey(date)
    }

    override suspend fun updateFocus(date: String, focus: String, updatedAt: Long) {
        val current = plannersMap[date] ?: DailyPlannerEntity(date = date)
        plannersMap[date] = current.copy(focus = focus, updatedAt = updatedAt, syncState = SyncState.SYNC_PENDING)
        flow.value = plannersMap.toMap()
    }

    override suspend fun updateNotes(date: String, notes: String, updatedAt: Long) {
        val current = plannersMap[date] ?: DailyPlannerEntity(date = date)
        plannersMap[date] = current.copy(notes = notes, updatedAt = updatedAt, syncState = SyncState.SYNC_PENDING)
        flow.value = plannersMap.toMap()
    }

    override suspend fun updateMood(date: String, moodJson: String?, updatedAt: Long) {
        val current = plannersMap[date] ?: DailyPlannerEntity(date = date)
        plannersMap[date] = current.copy(moodJson = moodJson, updatedAt = updatedAt, syncState = SyncState.SYNC_PENDING)
        flow.value = plannersMap.toMap()
    }

    override suspend fun updateReflection(date: String, reflectionJson: String, updatedAt: Long) {
        val current = plannersMap[date] ?: DailyPlannerEntity(date = date)
        plannersMap[date] = current.copy(reflectionJson = reflectionJson, updatedAt = updatedAt, syncState = SyncState.SYNC_PENDING)
        flow.value = plannersMap.toMap()
    }

    override suspend fun updateDailyReminder(date: String, reminder: String, updatedAt: Long) {
        val current = plannersMap[date] ?: DailyPlannerEntity(date = date)
        plannersMap[date] = current.copy(dailyReminder = reminder, updatedAt = updatedAt, syncState = SyncState.SYNC_PENDING)
        flow.value = plannersMap.toMap()
    }

    override suspend fun updateSyncState(date: String, syncState: String, lastSyncedAt: Long?) {
        val current = plannersMap[date] ?: return
        plannersMap[date] = current.copy(syncState = SyncState.valueOf(syncState), lastSyncedAt = lastSyncedAt)
        flow.value = plannersMap.toMap()
    }
}

class FakeTodoDao : TodoDao {
    private val todosMap = mutableMapOf<String, TodoEntity>()
    private val flow = MutableStateFlow<List<TodoEntity>>(emptyList())

    override fun getTodosByDate(date: String): Flow<List<TodoEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.orderIndex } }
    }

    override suspend fun getTodosByDateOnce(date: String): List<TodoEntity> {
        return todosMap.values.filter { it.plannerDate == date }.sortedBy { it.orderIndex }
    }

    override suspend fun getTodoById(id: String): TodoEntity? = todosMap[id]

    override suspend fun upsertTodo(todo: TodoEntity) {
        todosMap[todo.id] = todo
        flow.value = todosMap.values.toList()
    }

    override suspend fun upsertTodos(todos: List<TodoEntity>) {
        todos.forEach { todosMap[it.id] = it }
        flow.value = todosMap.values.toList()
    }

    override suspend fun updateCompletion(id: String, isCompleted: Boolean, completedAt: Long?, updatedAt: Long) {
        todosMap[id]?.let {
            todosMap[id] = it.copy(isCompleted = isCompleted, completedAt = completedAt, updatedAt = updatedAt)
            flow.value = todosMap.values.toList()
        }
    }

    override suspend fun updateOrder(id: String, orderIndex: Int, updatedAt: Long) {
        todosMap[id]?.let {
            todosMap[id] = it.copy(orderIndex = orderIndex, updatedAt = updatedAt)
            flow.value = todosMap.values.toList()
        }
    }

    override suspend fun deleteTodoById(id: String) {
        todosMap.remove(id)
        flow.value = todosMap.values.toList()
    }

    override suspend fun deleteTodosByDate(date: String) {
        todosMap.entries.removeIf { it.value.plannerDate == date }
        flow.value = todosMap.values.toList()
    }
}

class FakePriorityDao : PriorityDao {
    private val priorities = mutableMapOf<String, PriorityEntity>()
    private val flow = MutableStateFlow<List<PriorityEntity>>(emptyList())

    override fun getPrioritiesByDate(date: String): Flow<List<PriorityEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.orderIndex } }
    }

    override suspend fun getPrioritiesByDateOnce(date: String): List<PriorityEntity> {
        return priorities.values.filter { it.plannerDate == date }.sortedBy { it.orderIndex }
    }

    override suspend fun upsertPriorities(priorities: List<PriorityEntity>) {
        priorities.forEach { this.priorities[it.id] = it }
        flow.value = this.priorities.values.toList()
    }

    override suspend fun deletePrioritiesByDate(date: String) {
        priorities.entries.removeIf { it.value.plannerDate == date }
        flow.value = priorities.values.toList()
    }
}

class FakeScheduleDao : ScheduleDao {
    private val schedule = mutableMapOf<String, ScheduleEntity>()
    private val flow = MutableStateFlow<List<ScheduleEntity>>(emptyList())

    override fun getScheduleByDate(date: String): Flow<List<ScheduleEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.timeSlot } }
    }

    override suspend fun getScheduleByDateOnce(date: String): List<ScheduleEntity> {
        return schedule.values.filter { it.plannerDate == date }.sortedBy { it.timeSlot }
    }

    override suspend fun upsertSchedule(schedule: List<ScheduleEntity>) {
        schedule.forEach { this.schedule[it.id] = it }
        flow.value = this.schedule.values.toList()
    }

    override suspend fun deleteScheduleByDate(date: String) {
        schedule.entries.removeIf { it.value.plannerDate == date }
        flow.value = schedule.values.toList()
    }
}

class FakeSelfCareDao : SelfCareDao {
    private val items = mutableMapOf<String, SelfCareEntity>()
    private val flow = MutableStateFlow<List<SelfCareEntity>>(emptyList())

    override fun getSelfCareByDate(date: String): Flow<List<SelfCareEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.orderIndex } }
    }

    override suspend fun getSelfCareByDateOnce(date: String): List<SelfCareEntity> {
        return items.values.filter { it.plannerDate == date }.sortedBy { it.orderIndex }
    }

    override suspend fun upsertSelfCareItems(items: List<SelfCareEntity>) {
        items.forEach { this.items[it.id] = it }
        flow.value = this.items.values.toList()
    }

    override suspend fun toggleSelfCare(id: String, isCompleted: Boolean, updatedAt: Long) {
        items[id]?.let {
            items[id] = it.copy(isCompleted = isCompleted, updatedAt = updatedAt)
            flow.value = items.values.toList()
        }
    }

    override suspend fun deleteSelfCareByDate(date: String) {
        items.entries.removeIf { it.value.plannerDate == date }
        flow.value = items.values.toList()
    }
}

class FakeReminderDao : ReminderDao {
    private val reminders = mutableMapOf<String, ReminderEntity>()
    private val flow = MutableStateFlow<List<ReminderEntity>>(emptyList())

    override fun getRemindersByDate(date: String): Flow<List<ReminderEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.orderIndex } }
    }

    override suspend fun getRemindersByDateOnce(date: String): List<ReminderEntity> {
        return reminders.values.filter { it.plannerDate == date }.sortedBy { it.orderIndex }
    }

    override suspend fun upsertReminder(reminder: ReminderEntity) {
        reminders[reminder.id] = reminder
        flow.value = reminders.values.toList()
    }

    override suspend fun upsertReminders(reminders: List<ReminderEntity>) {
        reminders.forEach { this.reminders[it.id] = it }
        flow.value = this.reminders.values.toList()
    }

    override suspend fun toggleReminder(id: String, isCompleted: Boolean, updatedAt: Long) {
        reminders[id]?.let {
            reminders[id] = it.copy(isCompleted = isCompleted, updatedAt = updatedAt)
            flow.value = reminders.values.toList()
        }
    }

    override suspend fun deleteReminderById(id: String) {
        reminders.remove(id)
        flow.value = reminders.values.toList()
    }

    override suspend fun deleteRemindersByDate(date: String) {
        reminders.entries.removeIf { it.value.plannerDate == date }
        flow.value = reminders.values.toList()
    }
}

class FakeGratitudeDao : GratitudeDao {
    private val items = mutableMapOf<String, GratitudeEntity>()
    private val flow = MutableStateFlow<List<GratitudeEntity>>(emptyList())

    override fun getGratitudeByDate(date: String): Flow<List<GratitudeEntity>> {
        return flow.map { list -> list.filter { it.plannerDate == date }.sortedBy { it.orderIndex } }
    }

    override suspend fun getGratitudeByDateOnce(date: String): List<GratitudeEntity> {
        return items.values.filter { it.plannerDate == date }.sortedBy { it.orderIndex }
    }

    override suspend fun upsertGratitude(items: List<GratitudeEntity>) {
        items.forEach { this.items[it.id] = it }
        flow.value = this.items.values.toList()
    }

    override suspend fun deleteGratitudeByDate(date: String) {
        items.entries.removeIf { it.value.plannerDate == date }
        flow.value = items.values.toList()
    }
}
