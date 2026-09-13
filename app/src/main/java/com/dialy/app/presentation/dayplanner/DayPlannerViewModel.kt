package com.dialy.app.presentation.dayplanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.pdf.DiaryPdfGenerator
import com.dialy.app.core.pdf.PdfExportResult
import com.dialy.app.core.sync.SyncResult
import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.DateUtils
import com.dialy.app.core.util.IdGenerator
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
import com.dialy.app.domain.repository.AuthRepository
import com.dialy.app.domain.repository.PlannerRepository
import com.dialy.app.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayPlannerViewModel(
    private val plannerRepository: PlannerRepository,
    private val authRepository: AuthRepository? = null,
    private val syncRepository: SyncRepository? = null
) : ViewModel() {

    private val _currentDate = MutableStateFlow(DateUtils.toIsoString(LocalDate.now()))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _pdfExportResult = MutableStateFlow<PdfExportResult?>(null)
    val pdfExportResult: StateFlow<PdfExportResult?> = _pdfExportResult.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    // --- In-Memory Short-Term Cache ---
    // All typing and user edits update this in-memory cache instantly (0ms latency, zero jitter).
    // Writes to the local Room database are debounced and executed in the background.
    private val _planner = MutableStateFlow<DailyPlanner?>(null)
    val planner: StateFlow<DailyPlanner?> = _planner.asStateFlow()

    @Volatile
    private var hasUnsavedCache: Boolean = false

    private var dbFlushJob: Job? = null
    private var plannerObservationJob: Job? = null

    val authState: StateFlow<AuthState> = authRepository?.authState
        ?: MutableStateFlow(AuthState.Unauthenticated).asStateFlow()

    init {
        observePlannerForDate(_currentDate.value)
    }

    private fun observePlannerForDate(date: String) {
        plannerObservationJob?.cancel()
        plannerObservationJob = viewModelScope.launch {
            plannerRepository.getPlannerFlow(date).collect { dbPlanner ->
                // Only take DB emission if we don't currently have unsaved in-memory user edits
                if (!hasUnsavedCache) {
                    _planner.value = dbPlanner ?: DailyPlanner.createDefault(date)
                }
            }
        }
    }

    /**
     * Updates the in-memory cache synchronously (0ms delay, no disk I/O),
     * and schedules a debounced write to the Room database.
     */
    private fun updateCacheAndScheduleSave(updater: (DailyPlanner) -> DailyPlanner) {
        val current = _planner.value ?: DailyPlanner.createDefault(_currentDate.value)
        val updated = updater(current).copy(updatedAt = System.currentTimeMillis())
        _planner.value = updated
        hasUnsavedCache = true

        // Debounce write to local DB (1000ms of inactivity after user stops typing)
        dbFlushJob?.cancel()
        dbFlushJob = viewModelScope.launch {
            delay(1000L)
            flushCacheToDb()
        }
    }

    /**
     * Immediately flushes the in-memory cache to the local database.
     */
    suspend fun flushCacheToDb() {
        dbFlushJob?.cancel()
        if (hasUnsavedCache) {
            val toSave = _planner.value ?: return
            try {
                plannerRepository.savePlanner(toSave)
                hasUnsavedCache = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save planner: ${e.message}"
            }
        }
    }

    // --- Date Navigation ---

    fun onDateSelected(date: String) {
        if (_currentDate.value == date) return
        viewModelScope.launch {
            flushCacheToDb()
            _currentDate.value = date
            hasUnsavedCache = false
            observePlannerForDate(date)
        }
    }

    fun onPreviousDay() {
        val current = DateUtils.parseIsoDate(_currentDate.value) ?: LocalDate.now()
        onDateSelected(DateUtils.toIsoString(current.minusDays(1)))
    }

    fun onNextDay() {
        val current = DateUtils.parseIsoDate(_currentDate.value) ?: LocalDate.now()
        onDateSelected(DateUtils.toIsoString(current.plusDays(1)))
    }

    fun onToday() {
        onDateSelected(DateUtils.toIsoString(LocalDate.now()))
    }

    // --- Planner In-Memory Actions ---

    fun onCreateDefaultPlanner() {
        viewModelScope.launch {
            try {
                flushCacheToDb()
                val date = _currentDate.value
                val defaultPlanner = DailyPlanner.createDefault(date)
                plannerRepository.savePlanner(defaultPlanner)
                _planner.value = defaultPlanner
                hasUnsavedCache = false
                _statusMessage.value = "Created default planner for $date"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create planner: ${e.message}"
            }
        }
    }

    fun onSaveCurrentPlanner() {
        viewModelScope.launch {
            try {
                flushCacheToDb()
                _statusMessage.value = "Saved planner successfully."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save planner: ${e.message}"
            }
        }
    }

    fun onUpdateFocus(focus: String) {
        updateCacheAndScheduleSave { it.copy(focus = focus) }
    }

    fun onUpdatePriority(order: Int, title: String, isCompleted: Boolean = false) {
        updateCacheAndScheduleSave { current ->
            val list = current.topPriorities.toMutableList()
            val index = list.indexOfFirst { it.order == order }
            val updatedItem = if (index >= 0) {
                list[index].copy(title = title, isCompleted = isCompleted, updatedAt = System.currentTimeMillis())
            } else {
                PriorityItem(
                    id = IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    order = order,
                    title = title,
                    isCompleted = isCompleted
                )
            }
            if (index >= 0) list[index] = updatedItem else list.add(updatedItem)
            current.copy(topPriorities = list)
        }
    }

    fun onAddTodo(title: String) {
        if (title.isBlank()) return
        updateCacheAndScheduleSave { current ->
            val newTodo = TodoItem(
                id = IdGenerator.generate(),
                plannerDate = _currentDate.value,
                title = title,
                order = current.todos.size
            )
            current.copy(todos = current.todos + newTodo)
        }
        _statusMessage.value = "Added task: $title"
    }

    fun onToggleTodo(id: String, isCompleted: Boolean) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                todos = current.todos.map { if (it.id == id) it.copy(isCompleted = isCompleted) else it }
            )
        }
    }

    fun onDeleteTodo(id: String) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                todos = current.todos.filter { it.id != id }
            )
        }
        _statusMessage.value = "Deleted task"
    }

    fun onUpdateScheduleSlot(slot: String, activity: String) {
        updateCacheAndScheduleSave { current ->
            val list = current.schedule.toMutableList()
            val index = list.indexOfFirst { it.timeSlot == slot }
            val updatedItem = if (index >= 0) {
                list[index].copy(activity = activity, updatedAt = System.currentTimeMillis())
            } else {
                ScheduleItem(
                    id = IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    timeSlot = slot,
                    activity = activity
                )
            }
            if (index >= 0) list[index] = updatedItem else list.add(updatedItem)
            current.copy(schedule = list)
        }
    }

    fun onToggleSelfCare(id: String, isCompleted: Boolean) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                selfCare = current.selfCare.map { if (it.id == id) it.copy(isCompleted = isCompleted) else it }
            )
        }
    }

    fun onUpdateNotes(notes: String) {
        updateCacheAndScheduleSave { it.copy(notes = notes) }
    }

    fun onAddReminder(text: String) {
        if (text.isBlank()) return
        updateCacheAndScheduleSave { current ->
            val reminder = ReminderItem(
                id = IdGenerator.generate(),
                plannerDate = _currentDate.value,
                text = text,
                order = current.dontForget.size
            )
            current.copy(dontForget = current.dontForget + reminder)
        }
    }

    fun onToggleReminder(id: String, isCompleted: Boolean) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                dontForget = current.dontForget.map { if (it.id == id) it.copy(isCompleted = isCompleted) else it }
            )
        }
    }

    fun onDeleteReminder(id: String) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                dontForget = current.dontForget.filter { it.id != id }
            )
        }
    }

    fun onAddGratitude(text: String) {
        if (text.isBlank()) return
        updateCacheAndScheduleSave { current ->
            val list = current.gratitude.toMutableList()
            list.add(
                GratitudeItem(
                    id = IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    text = text,
                    order = list.size
                )
            )
            current.copy(gratitude = list)
        }
    }

    fun onSelectMood(moodType: MoodType) {
        updateCacheAndScheduleSave { current ->
            current.copy(mood = Mood(type = moodType))
        }
    }

    fun onUpdateReflection(whatWentWell: String, whatCanImprove: String, proudOf: String) {
        updateCacheAndScheduleSave { current ->
            current.copy(
                reflection = Reflection(
                    whatWentWell = whatWentWell,
                    whatCanImprove = whatCanImprove,
                    proudOf = proudOf,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun onUpdateDailyReminder(text: String) {
        updateCacheAndScheduleSave { it.copy(dailyReminder = text) }
    }

    // --- Authentication & Cloud Sync Actions ---

    fun onGoogleSignInSuccess(user: AuthUser) {
        viewModelScope.launch {
            try {
                flushCacheToDb()
                dbFlushJob?.cancel()
                _planner.value = null
                hasUnsavedCache = false

                authRepository?.signIn(user)
                _statusMessage.value = "Signed in as ${user.email}"
                _isGuestMode.value = false

                // Switch local storage profile to this user's email
                plannerRepository.switchProfile(user.email)
                observePlannerForDate(_currentDate.value)

                onTriggerSync()
            } catch (e: Exception) {
                _errorMessage.value = "Sign in error: ${e.message}"
            }
        }
    }

    fun onGoogleSignInFailure(errorMsg: String) {
        _errorMessage.value = errorMsg
        authRepository?.setError(errorMsg)
    }

    fun onSkipGuestMode() {
        viewModelScope.launch {
            flushCacheToDb()
            dbFlushJob?.cancel()
            _planner.value = null
            hasUnsavedCache = false

            _isGuestMode.value = true
            plannerRepository.switchProfile("guest")
            observePlannerForDate(_currentDate.value)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            try {
                flushCacheToDb()
                dbFlushJob?.cancel()
                _planner.value = null
                hasUnsavedCache = false

                authRepository?.signOut()
                _statusMessage.value = "Signed out"
                _isGuestMode.value = false

                // Switch local storage profile back to guest
                plannerRepository.switchProfile("guest")
                observePlannerForDate(_currentDate.value)
            } catch (e: Exception) {
                _errorMessage.value = "Sign out failed: ${e.message}"
            }
        }
    }

    fun onTriggerSync() {
        viewModelScope.launch {
            try {
                flushCacheToDb() // Guarantee disk has all current user edits before sync
                val result = syncRepository?.syncPlanner(_currentDate.value)
                when (result) {
                    is SyncResult.Success -> _statusMessage.value = "Drive sync complete"
                    is SyncResult.Error -> _errorMessage.value = "Sync error: ${result.message}"
                    is SyncResult.Offline -> _statusMessage.value = "Offline: changes queued"
                    is SyncResult.NotAuthenticated -> _statusMessage.value = "Sign in to enable Drive sync"
                    is SyncResult.Conflict -> _statusMessage.value = "Sync resolved: ${result.message}"
                    null -> {}
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.message}"
            }
        }
    }

    // --- PDF Export Action ---

    fun onExportPdf(context: Context) {
        viewModelScope.launch {
            _isExportingPdf.value = true
            _statusMessage.value = "Generating diary PDF..."
            try {
                flushCacheToDb() // Ensure cache is committed
                val currentPlanner = _planner.value ?: return@launch
                val result = DiaryPdfGenerator.generatePdf(context.applicationContext, currentPlanner)
                result.onSuccess { exportResult ->
                    _pdfExportResult.value = exportResult
                    _statusMessage.value = "PDF exported: ${exportResult.destinationDescription}"
                }.onFailure { e ->
                    _errorMessage.value = "Failed to export PDF: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to export PDF: ${e.message}"
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    fun clearPdfExportResult() {
        _pdfExportResult.value = null
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        dbFlushJob?.cancel()
        if (hasUnsavedCache) {
            val toSave = _planner.value
            if (toSave != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        plannerRepository.savePlanner(toSave)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
