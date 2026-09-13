package com.dialy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.util.DefaultDispatcherProvider
import com.dialy.app.data.local.database.AppDatabase
import com.dialy.app.data.remote.auth.AuthRepositoryImpl
import com.dialy.app.data.remote.auth.GoogleAuthManager
import com.dialy.app.data.remote.drive.DriveDataSource
import com.dialy.app.data.remote.drive.GoogleDriveDataSourceImpl
import com.dialy.app.data.remote.drive.RemoteDriveFile
import com.dialy.app.data.repository.PlannerRepositoryImpl
import com.dialy.app.data.repository.SyncRepositoryImpl
import com.dialy.app.core.notification.AppNotificationManager
import com.dialy.app.presentation.account.AccountBackupScreen
import com.dialy.app.presentation.auth.AuthScreen
import com.dialy.app.presentation.dayplanner.DayPlannerScreen
import com.dialy.app.presentation.dayplanner.DayPlannerViewModel
import com.dialy.app.presentation.notifications.NotificationsScreen
import com.dialy.app.presentation.theme.DiaryColors
import com.dialy.app.presentation.theme.DiaryTheme

class MainActivity : ComponentActivity() {

    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: DayPlannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request maximum available display refresh rate (120Hz/90Hz/144Hz)
        enableHighRefreshRate()

        // Core Infrastructure initialization
        val dispatchers = DefaultDispatcherProvider()
        AppNotificationManager.initialize(applicationContext)
        googleAuthManager = GoogleAuthManager(applicationContext)
        authRepository = AuthRepositoryImpl(dispatchers)

        // Initialize SQLite Room database
        val initialProfile = googleAuthManager.getLastSignedInUser()?.email ?: "guest"
        val database = AppDatabase.getInstance(applicationContext, initialProfile)

        val plannerRepository = PlannerRepositoryImpl(
            plannerDao = database.dailyPlannerDao(),
            todoDao = database.todoDao(),
            priorityDao = database.priorityDao(),
            scheduleDao = database.scheduleDao(),
            selfCareDao = database.selfCareDao(),
            reminderDao = database.reminderDao(),
            gratitudeDao = database.gratitudeDao(),
            dispatchers = dispatchers,
            context = applicationContext
        )
        plannerRepository.switchProfile(initialProfile)

        // Initialize Real Google Drive Data Source (Google Drive REST API appDataFolder)
        val driveDataSource = GoogleDriveDataSourceImpl(
            context = applicationContext,
            dispatchers = dispatchers
        )

        val syncRepository = SyncRepositoryImpl(
            plannerRepository = plannerRepository,
            driveDataSource = driveDataSource,
            authRepository = authRepository,
            dispatchers = dispatchers
        )

        viewModel = DayPlannerViewModel(
            plannerRepository = plannerRepository,
            authRepository = authRepository,
            syncRepository = syncRepository
        )

        // Check if user is already signed in on startup
        googleAuthManager.getLastSignedInUser()?.let { signedInUser ->
            viewModel.onGoogleSignInSuccess(signedInUser)
        }

        setContent {
            DiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DiaryColors.PaperBackground
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        googleAuthManager = googleAuthManager
                    )
                }
            }
        }
    }

    /**
     * Enables maximum supported display refresh rate (e.g. 120Hz/90Hz/144Hz) for ultra-smooth UI.
     */
    private fun enableHighRefreshRate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val supportedModes = display?.supportedModes ?: return
                // Pick the mode with the maximum refresh rate (e.g. 120Hz, 144Hz, 90Hz)
                val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null && highestRefreshRateMode.refreshRate >= 60f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highestRefreshRateMode.modeId
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            preferredRefreshRate = highestRefreshRateMode.refreshRate
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Gracefully ignore if vendor ROM limits display mode override
        }
    }

    override fun onPause() {
        super.onPause()
        if (::viewModel.isInitialized) {
            viewModel.flushSyncToDb()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::viewModel.isInitialized) {
            viewModel.flushSyncToDb()
        }
    }
}

enum class AppScreen {
    PLANNER,
    ACCOUNT_BACKUP,
    NOTIFICATIONS
}

@Composable
fun AppNavigation(
    viewModel: DayPlannerViewModel,
    googleAuthManager: GoogleAuthManager
) {
    val authState by viewModel.authState.collectAsState()
    val isGuestMode by viewModel.isGuestMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.PLANNER) }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val authResult = googleAuthManager.handleSignInResult(result.data)
        authResult.onSuccess { authUser ->
            viewModel.onGoogleSignInSuccess(authUser)
            currentScreen = AppScreen.PLANNER
        }.onFailure { error ->
            viewModel.onGoogleSignInFailure(error.message ?: "Google Sign-In failed")
        }
    }

    // If authenticated OR continuing as guest
    if (authState is AuthState.Authenticated || isGuestMode) {
        when (currentScreen) {
            AppScreen.PLANNER -> {
                DayPlannerScreen(
                    viewModel = viewModel,
                    onNavigateToAccount = {
                        currentScreen = AppScreen.ACCOUNT_BACKUP
                    },
                    onNavigateToNotifications = {
                        currentScreen = AppScreen.NOTIFICATIONS
                    },
                    onSignOutClick = {
                        coroutineScope.launch {
                            googleAuthManager.signOut()
                            viewModel.onSignOut()
                            currentScreen = AppScreen.PLANNER
                        }
                    }
                )
            }
            AppScreen.NOTIFICATIONS -> {
                BackHandler {
                    currentScreen = AppScreen.PLANNER
                }

                NotificationsScreen(
                    onNavigateBack = { currentScreen = AppScreen.PLANNER }
                )
            }
            AppScreen.ACCOUNT_BACKUP -> {
                BackHandler {
                    currentScreen = AppScreen.PLANNER
                }

                AccountBackupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.PLANNER },
                    onSignInClick = {
                        coroutineScope.launch {
                            googleAuthManager.signOut()
                            googleSignInLauncher.launch(googleAuthManager.signInIntent)
                        }
                    },
                    onSignOutClick = {
                        coroutineScope.launch {
                            googleAuthManager.signOut()
                            viewModel.onSignOut()
                            currentScreen = AppScreen.PLANNER
                        }
                    }
                )
            }
        }
    } else {
        // First time / unauthenticated -> Show Google Sign In screen with Drive permission details
        AuthScreen(
            authState = authState,
            onSignInClick = {
                coroutineScope.launch {
                    // Sign out first to clear any cached session so Google Account Chooser is guaranteed to pop up
                    googleAuthManager.signOut()
                    googleSignInLauncher.launch(googleAuthManager.signInIntent)
                }
            },
            onSkipGuestClick = {
                viewModel.onSkipGuestMode()
                currentScreen = AppScreen.PLANNER
            }
        )
    }
}
