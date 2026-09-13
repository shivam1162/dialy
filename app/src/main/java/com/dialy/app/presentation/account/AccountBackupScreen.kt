package com.dialy.app.presentation.account

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.dialy.app.core.notification.AppNotificationManager
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.sync.work.DailySyncScheduler
import com.dialy.app.presentation.dayplanner.DayPlannerViewModel
import com.dialy.app.presentation.dayplanner.components.UserAvatar
import com.dialy.app.presentation.theme.DiaryColors
import com.dialy.app.presentation.theme.DiaryTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBackupScreen(
    viewModel: DayPlannerViewModel,
    onNavigateBack: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val isGuestMode by viewModel.isGuestMode.collectAsState()
    val lastBackupInfo by viewModel.lastBackupInfo.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Dialog visibility states
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Auto-backup settings state
    var scheduledHour by remember {
        mutableIntStateOf(DailySyncScheduler.getScheduledHour(context))
    }
    var scheduledMinute by remember {
        mutableIntStateOf(DailySyncScheduler.getScheduledMinute(context))
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLastBackupInfo()
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            AppNotificationManager.postInfo("Cloud Backup", it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            AppNotificationManager.postWarning("Cloud Backup Alert", it)
            viewModel.clearMessages()
        }
    }

    DiaryTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Account & Cloud Backup",
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryColors.TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Planner",
                                tint = DiaryColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DiaryColors.PaperBackground
                    )
                )
            },
            containerColor = DiaryColors.PaperBackground,
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profile Summary Card
                ProfileSummaryCard(
                    authState = authState,
                    isGuestMode = isGuestMode,
                    onSignInClick = onSignInClick
                )

                // 2. Google Drive Cloud Backup & Restore Card
                GoogleDriveBackupCard(
                    authState = authState,
                    lastBackupInfo = lastBackupInfo,
                    isBackingUp = isBackingUp,
                    isRestoring = isRestoring,
                    onRefresh = { viewModel.refreshLastBackupInfo() },
                    onBackupClick = { viewModel.onManualBackupToCloud() },
                    onRestoreClick = { showRestoreConfirmDialog = true }
                )

                // 3. Automated Daily Backup Timing Card
                AutoBackupScheduleCard(
                    context = context,
                    hour = scheduledHour,
                    minute = scheduledMinute,
                    onChangeTimeClick = { showTimePickerDialog = true }
                )

                // 4. Account Actions / Sign Out Card
                AccountActionsCard(
                    authState = authState,
                    isGuestMode = isGuestMode,
                    onSignOutClick = { showSignOutConfirmDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // --- Dialogs ---

        // 1. Restore Confirmation Dialog
        if (showRestoreConfirmDialog) {
            val formattedDate = lastBackupInfo?.let { formatBackupTimestamp(it.lastBackupTime) } ?: "Google Drive backup"
            val countInfo = lastBackupInfo?.let { " (${it.plannerCount} daily planners)" } ?: ""

            AlertDialog(
                onDismissRequest = { showRestoreConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = DiaryColors.RoseAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Delete Local Data & Restore?",
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "⚠️ All current local planner entries on this device for this account will be completely deleted.",
                            color = DiaryColors.RoseAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Your device will then download and restore all data from your Google Drive backup from $formattedDate$countInfo.",
                            color = DiaryColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "This action cannot be undone. Are you sure you want to proceed?",
                            color = DiaryColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestoreConfirmDialog = false
                            viewModel.onRestoreFromCloud()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.RoseAccent)
                    ) {
                        Text("Delete & Restore", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirmDialog = false }) {
                        Text("Cancel", color = DiaryColors.TextSecondary)
                    }
                },
                containerColor = DiaryColors.CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 2. Time Picker Dialog for Daily Auto-Backup
        if (showTimePickerDialog) {
            val timePickerState = rememberTimePickerState(
                initialHour = scheduledHour,
                initialMinute = scheduledMinute,
                is24Hour = false
            )

            AlertDialog(
                onDismissRequest = { showTimePickerDialog = false },
                title = {
                    Text(
                        text = "Set Auto-Backup Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = DiaryColors.SubtleCard,
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = DiaryColors.TextPrimary,
                                selectorColor = DiaryColors.GoldAccent,
                                periodSelectorBorderColor = DiaryColors.BorderSubtle,
                                periodSelectorSelectedContainerColor = DiaryColors.GoldAccent,
                                periodSelectorUnselectedContainerColor = DiaryColors.SubtleCard,
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = DiaryColors.TextPrimary,
                                timeSelectorSelectedContainerColor = DiaryColors.GoldAccent,
                                timeSelectorUnselectedContainerColor = DiaryColors.SubtleCard,
                                timeSelectorSelectedContentColor = Color.White,
                                timeSelectorUnselectedContentColor = DiaryColors.TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scheduledHour = timePickerState.hour
                            scheduledMinute = timePickerState.minute
                            DailySyncScheduler.scheduleDailySync(
                                context = context,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                            showTimePickerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.GoldAccent)
                    ) {
                        Text("Set Time", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerDialog = false }) {
                        Text("Cancel", color = DiaryColors.TextSecondary)
                    }
                },
                containerColor = DiaryColors.CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 3. Sign Out Confirmation Dialog
        if (showSignOutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmDialog = false },
                title = {
                    Text(
                        text = if (isGuestMode) "Exit Guest Mode?" else "Sign Out of Google?",
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                },
                text = {
                    Text(
                        text = if (isGuestMode) {
                            "You will return to the welcome screen. Your local guest entries remain securely saved."
                        } else {
                            "Your local data will stay safely on this device. You can sign back in anytime to continue syncing with Google Drive."
                        },
                        color = DiaryColors.TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutConfirmDialog = false
                            onSignOutClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.GoldAccent)
                    ) {
                        Text(if (isGuestMode) "Exit" else "Sign Out", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirmDialog = false }) {
                        Text("Cancel", color = DiaryColors.TextSecondary)
                    }
                },
                containerColor = DiaryColors.CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * User Profile Summary Card
 */
@Composable
private fun ProfileSummaryCard(
    authState: AuthState,
    isGuestMode: Boolean,
    onSignInClick: () -> Unit
) {
    val user = (authState as? AuthState.Authenticated)?.user

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UserAvatar(
                    user = user,
                    size = 54.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.displayName ?: if (isGuestMode) "Guest Mode" else "Welcome",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user?.email ?: "Offline local storage only",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DiaryColors.TextSecondary
                    )
                }

                // Account status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (user != null) DiaryColors.SageSoft else DiaryColors.SubtleCard)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (user != null) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (user != null) DiaryColors.SageAccent else DiaryColors.TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (user != null) "Connected" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (user != null) DiaryColors.SageAccent else DiaryColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isGuestMode && user == null) {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.GoldAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Drive Backup", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Google Drive Backup & Destructive Restore Card
 */
@Composable
private fun GoogleDriveBackupCard(
    authState: AuthState,
    lastBackupInfo: com.dialy.app.domain.repository.BackupMetadata?,
    isBackingUp: Boolean,
    isRestoring: Boolean,
    onRefresh: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val isAuthenticated = authState is AuthState.Authenticated

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DiaryColors.PeachSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = DiaryColors.GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Google Drive Cloud Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                }

                if (isAuthenticated) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Cloud Status",
                            tint = DiaryColors.GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = "Backs up your full diary history (focus, to-dos, priorities, notes, mood, self-care, reflection) to your personal Google Drive app storage.",
                style = MaterialTheme.typography.bodySmall,
                color = DiaryColors.TextSecondary,
                lineHeight = 18.sp
            )

            // Last Backup Info Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DiaryColors.SubtleCard)
                    .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LAST BACKUP AVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.GoldAccent,
                        letterSpacing = 1.sp
                    )

                    if (lastBackupInfo != null) {
                        Text(
                            text = "${formatBackupTimestamp(lastBackupInfo.lastBackupTime)} (${lastBackupInfo.plannerCount} entries)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryColors.TextPrimary
                        )
                    } else {
                        Text(
                            text = if (isAuthenticated) "No backup found on Google Drive" else "Sign in to view Google Drive backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DiaryColors.TextTertiary
                        )
                    }
                }
            }

            // Buttons: Backup Now & Restore Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Backup Now Button
                Button(
                    onClick = onBackupClick,
                    enabled = isAuthenticated && !isBackingUp && !isRestoring,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiaryColors.GoldAccent,
                        disabledContainerColor = DiaryColors.BorderSubtle
                    )
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backing up...", color = Color.White, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Restore Now Button
                OutlinedButton(
                    onClick = onRestoreClick,
                    enabled = isAuthenticated && !isBackingUp && !isRestoring && lastBackupInfo != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DiaryColors.RoseAccent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isAuthenticated && lastBackupInfo != null) DiaryColors.RoseAccent else DiaryColors.BorderSubtle
                    )
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DiaryColors.RoseAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Explanatory note for restore
            Text(
                text = "Notice: 'Restore Now' will wipe all current local planner entries for this account on this device and replace them with the snapshot downloaded from Google Drive.",
                style = MaterialTheme.typography.labelSmall,
                color = DiaryColors.TextTertiary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Automated Daily Backup Scheduling Card
 */
@Composable
private fun AutoBackupScheduleCard(
    context: Context,
    hour: Int,
    minute: Int,
    onChangeTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DiaryColors.SageSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DiaryColors.SageAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Daily Auto-Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                    Text(
                        text = "Runs silently in background once a day",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiaryColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DiaryColors.SubtleCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BACKUP TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiaryColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DailySyncScheduler.getFormattedScheduledTime(context),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = DiaryColors.TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onChangeTimeClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DiaryColors.GoldAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.GoldAccent)
                ) {
                    Text("Change Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Account Actions / Sign Out Card
 */
@Composable
private fun AccountActionsCard(
    authState: AuthState,
    isGuestMode: Boolean,
    onSignOutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Account Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DiaryColors.TextPrimary
            )

            Text(
                text = if (isGuestMode) {
                    "Currently operating in local guest mode. Your daily entries remain permanently on this device."
                } else {
                    "Your planner entries are stored permanently on your device and can be backed up to Google Drive anytime."
                },
                style = MaterialTheme.typography.bodySmall,
                color = DiaryColors.TextSecondary
            )

            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DiaryColors.RoseAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.RoseAccent)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGuestMode) "Exit Guest Mode" else "Sign Out of Account",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatBackupTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0) return "Never"
    val sdf = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
