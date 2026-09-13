package com.dialy.app.presentation.dayplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dialy.app.core.sync.SyncState
import com.dialy.app.presentation.dayplanner.components.DailyReminderSection
import com.dialy.app.presentation.dayplanner.components.DontForgetSection
import com.dialy.app.presentation.dayplanner.components.GratitudeSection
import com.dialy.app.presentation.dayplanner.components.HeaderSection
import com.dialy.app.presentation.dayplanner.components.MoodTrackerSection
import com.dialy.app.presentation.dayplanner.components.NotesIdeasSection
import com.dialy.app.presentation.dayplanner.components.ReflectionSection
import com.dialy.app.presentation.dayplanner.components.ScheduleSection
import com.dialy.app.presentation.dayplanner.components.SelfCareSection
import com.dialy.app.presentation.dayplanner.components.TodoListSection
import com.dialy.app.presentation.dayplanner.components.TodaysFocusSection
import com.dialy.app.presentation.dayplanner.components.TopPrioritiesSection
import com.dialy.app.presentation.theme.DiaryColors
import com.dialy.app.presentation.theme.DiaryTheme

@Composable
fun DayPlannerScreen(
    viewModel: DayPlannerViewModel
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val planner by viewModel.planner.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar("⚠️ $msg")
            viewModel.clearMessages()
        }
    }

    DiaryTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = DiaryColors.PaperBackground
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(DiaryColors.PaperBackground),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header Section (Date, Day of Week, Sync status, Account)
                item {
                    HeaderSection(
                        currentDateString = currentDate,
                        syncState = planner?.syncState ?: SyncState.LOCAL_ONLY,
                        authState = authState,
                        onPreviousDay = { viewModel.onPreviousDay() },
                        onNextDay = { viewModel.onNextDay() },
                        onToday = { viewModel.onToday() },
                        onSyncClick = { viewModel.onTriggerSync() },
                        onSignOutClick = { viewModel.onSignOut() }
                    )
                }

                // 2. Today's Focus
                item {
                    TodaysFocusSection(
                        focus = planner?.focus ?: "",
                        onFocusChange = { viewModel.onUpdateFocus(it) }
                    )
                }

                // 3. Top 3 Priorities
                item {
                    TopPrioritiesSection(
                        priorities = planner?.topPriorities ?: emptyList(),
                        onUpdatePriority = { order, title, isCompleted ->
                            viewModel.onUpdatePriority(order, title, isCompleted)
                        }
                    )
                }

                // 4. To-Do List
                item {
                    TodoListSection(
                        todos = planner?.todos ?: emptyList(),
                        onAddTodo = { viewModel.onAddTodo(it) },
                        onToggleTodo = { id, completed -> viewModel.onToggleTodo(id, completed) },
                        onDeleteTodo = { id -> viewModel.onDeleteTodo(id) }
                    )
                }

                // 5. Daily Schedule
                item {
                    ScheduleSection(
                        schedule = planner?.schedule ?: emptyList(),
                        onUpdateScheduleSlot = { slot, activity ->
                            viewModel.onUpdateScheduleSlot(slot, activity)
                        }
                    )
                }

                // 6. Self Care & Habits Checklist
                item {
                    SelfCareSection(
                        selfCareItems = planner?.selfCare ?: emptyList(),
                        onToggleSelfCare = { id, isDone -> viewModel.onToggleSelfCare(id, isDone) }
                    )
                }

                // 7. Mood Tracker
                item {
                    MoodTrackerSection(
                        currentMood = planner?.mood,
                        onSelectMood = { viewModel.onSelectMood(it) }
                    )
                }

                // 8. Daily Gratitude
                item {
                    GratitudeSection(
                        gratitudeList = planner?.gratitude ?: emptyList(),
                        onAddGratitude = { viewModel.onAddGratitude(it) }
                    )
                }

                // 9. End-of-Day Reflection
                item {
                    ReflectionSection(
                        reflection = planner?.reflection ?: com.dialy.app.domain.model.Reflection(),
                        onUpdateReflection = { wentWell, improve, proud ->
                            viewModel.onUpdateReflection(wentWell, improve, proud)
                        }
                    )
                }

                // 10. Notes & Ideas
                item {
                    NotesIdeasSection(
                        notes = planner?.notes ?: "",
                        onNotesChange = { viewModel.onUpdateNotes(it) }
                    )
                }

                // 11. Don't Forget / Reminders
                item {
                    DontForgetSection(
                        reminders = planner?.dontForget ?: emptyList(),
                        onAddReminder = { viewModel.onAddReminder(it) },
                        onToggleReminder = { id, completed -> viewModel.onToggleReminder(id, completed) },
                        onDeleteReminder = { id -> viewModel.onDeleteReminder(id) }
                    )
                }

                // 12. Daily Reminder & Affirmation Banner
                item {
                    DailyReminderSection(
                        reminder = planner?.dailyReminder ?: "",
                        onReminderChange = { viewModel.onUpdateDailyReminder(it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
