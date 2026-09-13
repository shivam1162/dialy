package com.dialy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.util.DefaultDispatcherProvider
import com.dialy.app.data.local.database.AppDatabase
import com.dialy.app.data.remote.auth.AuthRepositoryImpl
import com.dialy.app.data.remote.auth.GoogleAuthManager
import com.dialy.app.data.remote.drive.DriveDataSource
import com.dialy.app.data.remote.drive.RemoteDriveFile
import com.dialy.app.data.repository.PlannerRepositoryImpl
import com.dialy.app.data.repository.SyncRepositoryImpl
import com.dialy.app.presentation.auth.AuthScreen
import com.dialy.app.presentation.dayplanner.DayPlannerScreen
import com.dialy.app.presentation.dayplanner.DayPlannerViewModel
import com.dialy.app.presentation.theme.DiaryColors
import com.dialy.app.presentation.theme.DiaryTheme

class MainActivity : ComponentActivity() {

    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: DayPlannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleAuthManager = GoogleAuthManager(this)
        authRepository = AuthRepositoryImpl()

        val database = AppDatabase.getInstance(applicationContext)
        val dispatchers = DefaultDispatcherProvider()

        val plannerRepository = PlannerRepositoryImpl(
            plannerDao = database.dailyPlannerDao(),
            todoDao = database.todoDao(),
            priorityDao = database.priorityDao(),
            scheduleDao = database.scheduleDao(),
            selfCareDao = database.selfCareDao(),
            reminderDao = database.reminderDao(),
            gratitudeDao = database.gratitudeDao(),
            dispatchers = dispatchers
        )

        // Initialize Drive Data Source (ready for Google Drive AppData folder operations)
        val driveDataSource = object : DriveDataSource {
            private val cache = mutableMapOf<String, String>()

            override suspend fun uploadFile(fileName: String, content: String): Result<String> {
                cache[fileName] = content
                return Result.success(fileName)
            }

            override suspend fun downloadFile(fileName: String): Result<String?> {
                return Result.success(cache[fileName])
            }

            override suspend fun listFiles(): Result<List<RemoteDriveFile>> {
                return Result.success(cache.map {
                    RemoteDriveFile(id = it.key, name = it.key, modifiedTime = System.currentTimeMillis())
                })
            }

            override suspend fun deleteFile(fileName: String): Result<Unit> {
                cache.remove(fileName)
                return Result.success(Unit)
            }
        }

        val syncRepository = SyncRepositoryImpl(
            plannerRepository = plannerRepository,
            plannerDao = database.dailyPlannerDao(),
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
}

@Composable
fun AppNavigation(
    viewModel: DayPlannerViewModel,
    googleAuthManager: GoogleAuthManager
) {
    val authState by viewModel.authState.collectAsState()
    val isGuestMode by viewModel.isGuestMode.collectAsState()

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val authResult = googleAuthManager.handleSignInResult(result.data)
        authResult.onSuccess { authUser ->
            viewModel.onGoogleSignInSuccess(authUser)
        }.onFailure { error ->
            viewModel.onGoogleSignInFailure(error.message ?: "Google Sign-In failed")
        }
    }

    // If authenticated OR continuing as guest -> show Day Planner
    if (authState is AuthState.Authenticated || isGuestMode) {
        DayPlannerScreen(viewModel = viewModel)
    } else {
        // First time / unauthenticated -> Show Google Sign In screen with Drive permission details
        AuthScreen(
            authState = authState,
            onSignInClick = {
                googleSignInLauncher.launch(googleAuthManager.signInIntent)
            },
            onSkipGuestClick = {
                viewModel.onSkipGuestMode()
            }
        )
    }
}
