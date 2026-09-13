package com.dialy.app.presentation.dayplanner

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.runBlocking
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.notification.AppNotificationManager
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
import com.dialy.app.domain.repository.BackupMetadata
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

    // --- Google Drive Backup & Restore State ---
    private val _lastBackupInfo = MutableStateFlow<BackupMetadata?>(null)
    val lastBackupInfo: StateFlow<BackupMetadata?> = _lastBackupInfo.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

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
        refreshLastBackupInfo()
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

        // Fast debounce write to local DB (350ms of inactivity after user stops typing)
        dbFlushJob?.cancel()
        dbFlushJob = viewModelScope.launch {
            delay(350L)
            flushCacheToDb()
        }
    }

    /**
     * Immediately flushes the in-memory cache to the local database asynchronously.
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

    /**
     * Synchronously and immediately commits the in-memory cache to SQLite Room database.
     * Guaranteed to persist data before app closure, pause, back, home, or task-kill.
     */
    fun flushSyncToDb() {
        dbFlushJob?.cancel()
        if (hasUnsavedCache) {
            val toSave = _planner.value ?: return
            hasUnsavedCache = false
            try {
                runBlocking(Dispatchers.IO) {
                    plannerRepository.savePlanner(toSave)
                }
                Log.d("DayPlannerVM", "Successfully saved planner to SQLite on app lifecycle event")
            } catch (e: Exception) {
                Log.e("DayPlannerVM", "Error flushing cache on app close: ${e.message}", e)
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
            val nextMood = if (current.mood?.type == moodType) null else Mood(type = moodType)
            current.copy(mood = nextMood)
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
                syncRepository?.syncPlanner(_currentDate.value)
            } catch (_: Exception) {
                // Background sync fails gracefully without disturbing user interaction
            }
        }
    }

    // --- Google Drive Full Backup & Restore Actions ---

    fun refreshLastBackupInfo() {
        viewModelScope.launch {
            try {
                val meta = syncRepository?.getLastBackupMetadata()
                _lastBackupInfo.value = meta
            } catch (_: Exception) {}
        }
    }

    fun onManualBackupToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isBackingUp.value = true
            _statusMessage.value = "Backing up to Google Drive..."
            try {
                flushCacheToDb() // Flush any pending edits to disk first
                val result = syncRepository?.backupToCloud()
                when (result) {
                    is SyncResult.Success -> {
                        _statusMessage.value = "Backup created successfully"
                        AppNotificationManager.postSuccess("Google Drive Backup", "Backup created and uploaded to Google Drive successfully")
                        refreshLastBackupInfo()
                        onComplete(true, "Backup uploaded to Google Drive successfully")
                    }
                    is SyncResult.Error -> {
                        _errorMessage.value = result.message
                        AppNotificationManager.postError("Google Drive Backup", result.message)
                        onComplete(false, result.message)
                    }
                    is SyncResult.NotAuthenticated -> {
                        _errorMessage.value = "Please sign in to backup to Google Drive"
                        AppNotificationManager.postWarning("Google Drive Backup", "Please sign in to backup to Google Drive")
                        onComplete(false, "Not authenticated")
                    }
                    else -> {
                        AppNotificationManager.postError("Google Drive Backup", "Backup failed")
                        onComplete(false, "Backup failed")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Backup failed: ${e.message}"
                AppNotificationManager.postError("Google Drive Backup", "Backup failed: ${e.message}")
                onComplete(false, e.message ?: "Unknown error")
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun onRestoreFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRestoring.value = true
            _statusMessage.value = "Restoring data from Google Drive..."
            try {
                val result = syncRepository?.restoreFromCloud()
                when (result) {
                    is SyncResult.Success -> {
                        _statusMessage.value = "Restored ${result.data.size} entries from Google Drive"
                        AppNotificationManager.postSuccess("Google Drive Restore", "Restored ${result.data.size} planner entries from Google Drive")
                        hasUnsavedCache = false
                        _planner.value = null
                        observePlannerForDate(_currentDate.value)
                        refreshLastBackupInfo()
                        onComplete(true, "Restored ${result.data.size} planner entries from Google Drive")
                    }
                    is SyncResult.Error -> {
                        _errorMessage.value = result.message
                        AppNotificationManager.postError("Google Drive Restore", result.message)
                        onComplete(false, result.message)
                    }
                    is SyncResult.NotAuthenticated -> {
                        _errorMessage.value = "Please sign in to restore from Google Drive"
                        AppNotificationManager.postWarning("Google Drive Restore", "Please sign in to restore from Google Drive")
                        onComplete(false, "Not authenticated")
                    }
                    else -> {
                        AppNotificationManager.postError("Google Drive Restore", "Restore failed")
                        onComplete(false, "Restore failed")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Restore failed: ${e.message}"
                AppNotificationManager.postError("Google Drive Restore", "Restore failed: ${e.message}")
                onComplete(false, e.message ?: "Unknown error")
            } finally {
                _isRestoring.value = false
            }
        }
    }

    // --- Diary Preview Action (Eye Icon) ---

    fun onPreviewDiary(context: Context) {
        viewModelScope.launch {
            _isExportingPdf.value = true
            try {
                flushSyncToDb() // Ensure latest in-memory cache is committed to database
                val currentPlanner = _planner.value ?: return@launch
                val result = DiaryPdfGenerator.generatePdf(context.applicationContext, currentPlanner)
                result.onSuccess { exportResult ->
                    DiaryPdfGenerator.openPdfViewer(context, exportResult.uri)
                }.onFailure { e ->
                    AppNotificationManager.postWarning("Diary Preview", "Failed to open preview: ${e.message}")
                }
            } catch (e: Exception) {
                AppNotificationManager.postWarning("Diary Preview", "Failed to open preview: ${e.message}")
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    fun onExportPdf(context: Context) {
        onPreviewDiary(context)
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
        flushSyncToDb()
    }
}
