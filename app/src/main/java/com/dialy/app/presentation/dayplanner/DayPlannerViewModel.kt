package com.dialy.app.presentation.dayplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
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

    // Debounce job holders to ensure smooth typing with zero jitter/recomposition lag
    private var focusDebounceJob: Job? = null
    private val priorityDebounceJobs = mutableMapOf<Int, Job>()
    private val scheduleDebounceJobs = mutableMapOf<String, Job>()
    private var notesDebounceJob: Job? = null
    private var reflectionDebounceJob: Job? = null
    private var reminderDebounceJob: Job? = null

    val planner: StateFlow<DailyPlanner?> = _currentDate
        .flatMapLatest { date ->
            plannerRepository.getPlannerFlow(date)
        }
        .catch { e ->
            _errorMessage.value = "Failed to load planner: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val authState: StateFlow<AuthState> = authRepository?.authState
        ?: MutableStateFlow(AuthState.Unauthenticated).asStateFlow()

    // --- Date Navigation ---

    fun onDateSelected(date: String) {
        _currentDate.value = date
    }

    fun onPreviousDay() {
        val current = DateUtils.parseIsoDate(_currentDate.value) ?: LocalDate.now()
        _currentDate.value = DateUtils.toIsoString(current.minusDays(1))
    }

    fun onNextDay() {
        val current = DateUtils.parseIsoDate(_currentDate.value) ?: LocalDate.now()
        _currentDate.value = DateUtils.toIsoString(current.plusDays(1))
    }

    fun onToday() {
        _currentDate.value = DateUtils.toIsoString(LocalDate.now())
    }

    // --- Planner Actions ---

    fun onCreateDefaultPlanner() {
        viewModelScope.launch {
            try {
                val date = _currentDate.value
                val defaultPlanner = DailyPlanner.createDefault(date)
                plannerRepository.savePlanner(defaultPlanner)
                _statusMessage.value = "Created default planner for $date"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create planner: ${e.message}"
            }
        }
    }

    fun onSaveCurrentPlanner() {
        viewModelScope.launch {
            try {
                val current = planner.value ?: DailyPlanner.createDefault(_currentDate.value)
                plannerRepository.savePlanner(current)
                _statusMessage.value = "Saved planner successfully."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save planner: ${e.message}"
            }
        }
    }

    fun onUpdateFocus(focus: String) {
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(300L)
            try {
                plannerRepository.updateFocus(_currentDate.value, focus)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update focus: ${e.message}"
            }
        }
    }

    fun onUpdatePriority(order: Int, title: String, isCompleted: Boolean = false) {
        priorityDebounceJobs[order]?.cancel()
        priorityDebounceJobs[order] = viewModelScope.launch {
            if (!isCompleted) {
                delay(300L)
            }
            try {
                val currentPriorities = planner.value?.topPriorities?.toMutableList() ?: mutableListOf()
                val existingIndex = currentPriorities.indexOfFirst { it.order == order }
                val updatedItem = PriorityItem(
                    id = if (existingIndex >= 0) currentPriorities[existingIndex].id else IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    order = order,
                    title = title,
                    isCompleted = isCompleted
                )

                if (existingIndex >= 0) {
                    currentPriorities[existingIndex] = updatedItem
                } else {
                    currentPriorities.add(updatedItem)
                }

                plannerRepository.updatePriorities(_currentDate.value, currentPriorities)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update priority: ${e.message}"
            }
        }
    }

    fun onAddTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val newTodo = TodoItem(
                    id = IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    title = title,
                    order = planner.value?.todos?.size ?: 0
                )
                plannerRepository.addTodo(newTodo)
                _statusMessage.value = "Added task: $title"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add todo: ${e.message}"
            }
        }
    }

    fun onToggleTodo(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                plannerRepository.toggleTodoCompletion(id, isCompleted)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle todo: ${e.message}"
            }
        }
    }

    fun onDeleteTodo(id: String) {
        viewModelScope.launch {
            try {
                plannerRepository.deleteTodo(id)
                _statusMessage.value = "Deleted task"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete todo: ${e.message}"
            }
        }
    }

    fun onUpdateScheduleSlot(slot: String, activity: String) {
        scheduleDebounceJobs[slot]?.cancel()
        scheduleDebounceJobs[slot] = viewModelScope.launch {
            delay(300L)
            try {
                val currentSchedule = planner.value?.schedule?.toMutableList() ?: mutableListOf()
                val index = currentSchedule.indexOfFirst { it.timeSlot == slot }
                if (index >= 0) {
                    currentSchedule[index] = currentSchedule[index].copy(activity = activity)
                } else {
                    currentSchedule.add(
                        ScheduleItem(
                            id = IdGenerator.generate(),
                            plannerDate = _currentDate.value,
                            timeSlot = slot,
                            activity = activity
                        )
                    )
                }
                plannerRepository.updateSchedule(_currentDate.value, currentSchedule)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update schedule: ${e.message}"
            }
        }
    }

    fun onToggleSelfCare(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                plannerRepository.toggleSelfCare(id, isCompleted)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle self-care: ${e.message}"
            }
        }
    }

    fun onUpdateNotes(notes: String) {
        notesDebounceJob?.cancel()
        notesDebounceJob = viewModelScope.launch {
            delay(350L)
            try {
                plannerRepository.updateNotes(_currentDate.value, notes)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update notes: ${e.message}"
            }
        }
    }

    fun onAddReminder(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val reminder = ReminderItem(
                    id = IdGenerator.generate(),
                    plannerDate = _currentDate.value,
                    text = text,
                    order = planner.value?.dontForget?.size ?: 0
                )
                plannerRepository.addReminder(reminder)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add reminder: ${e.message}"
            }
        }
    }

    fun onToggleReminder(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                plannerRepository.toggleReminderCompletion(id, isCompleted)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle reminder: ${e.message}"
            }
        }
    }

    fun onDeleteReminder(id: String) {
        viewModelScope.launch {
            try {
                plannerRepository.deleteReminder(id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete reminder: ${e.message}"
            }
        }
    }

    fun onAddGratitude(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val list = planner.value?.gratitude?.toMutableList() ?: mutableListOf()
                list.add(
                    GratitudeItem(
                        id = IdGenerator.generate(),
                        plannerDate = _currentDate.value,
                        text = text,
                        order = list.size
                    )
                )
                plannerRepository.updateGratitude(_currentDate.value, list)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update gratitude: ${e.message}"
            }
        }
    }

    fun onSelectMood(moodType: MoodType) {
        viewModelScope.launch {
            try {
                plannerRepository.updateMood(_currentDate.value, Mood(type = moodType))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update mood: ${e.message}"
            }
        }
    }

    fun onUpdateReflection(whatWentWell: String, whatCanImprove: String, proudOf: String) {
        reflectionDebounceJob?.cancel()
        reflectionDebounceJob = viewModelScope.launch {
            delay(350L)
            try {
                val reflection = Reflection(
                    whatWentWell = whatWentWell,
                    whatCanImprove = whatCanImprove,
                    proudOf = proudOf,
                    updatedAt = System.currentTimeMillis()
                )
                plannerRepository.updateReflection(_currentDate.value, reflection)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update reflection: ${e.message}"
            }
        }
    }

    fun onUpdateDailyReminder(text: String) {
        reminderDebounceJob?.cancel()
        reminderDebounceJob = viewModelScope.launch {
            delay(300L)
            try {
                plannerRepository.updateDailyReminder(_currentDate.value, text)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update daily reminder: ${e.message}"
            }
        }
    }

    // --- Authentication & Cloud Sync Actions ---

    fun onGoogleSignInSuccess(user: AuthUser) {
        viewModelScope.launch {
            try {
                authRepository?.signIn(user)
                _statusMessage.value = "Signed in as ${user.email}"
                _isGuestMode.value = false
                // Trigger auto sync upon sign in
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
        _isGuestMode.value = true
    }

    fun onSignOut() {
        viewModelScope.launch {
            try {
                authRepository?.signOut()
                _statusMessage.value = "Signed out"
                _isGuestMode.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Sign out failed: ${e.message}"
            }
        }
    }

    fun onTriggerSync() {
        viewModelScope.launch {
            try {
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

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }
}
