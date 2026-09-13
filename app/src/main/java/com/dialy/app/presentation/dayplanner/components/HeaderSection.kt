package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.DateUtils
import com.dialy.app.presentation.theme.DiaryColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    currentDateString: String,
    syncState: SyncState = SyncState.LOCAL_ONLY,
    authState: AuthState = AuthState.Unauthenticated,
    isExportingPdf: Boolean = false,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (String) -> Unit = {},
    onSyncClick: () -> Unit,
    onDownloadPdfClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val date = DateUtils.parseIsoDate(currentDateString) ?: LocalDate.now()
    val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())).uppercase(Locale.getDefault())
    val formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialMillis = remember(currentDateString) {
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateSelected(DateUtils.toIsoString(selectedLocalDate))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.GoldAccent)
                ) {
                    Text("Select", color = Color.White)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onToday()
                            showDatePicker = false
                        }
                    ) {
                        Text("Today", color = DiaryColors.GoldAccent)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = DiaryColors.TextSecondary)
                    }
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DiaryColors.CardBackground
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DiaryColors.CardBackground,
                    titleContentColor = DiaryColors.TextPrimary,
                    headlineContentColor = DiaryColors.GoldAccent,
                    weekdayContentColor = DiaryColors.TextSecondary,
                    subheadContentColor = DiaryColors.TextPrimary,
                    yearContentColor = DiaryColors.TextPrimary,
                    currentYearContentColor = DiaryColors.GoldAccent,
                    selectedYearContentColor = DiaryColors.PaperBackground,
                    selectedYearContainerColor = DiaryColors.GoldAccent,
                    dayContentColor = DiaryColors.TextPrimary,
                    selectedDayContentColor = DiaryColors.PaperBackground,
                    selectedDayContainerColor = DiaryColors.GoldAccent,
                    todayContentColor = DiaryColors.GoldAccent,
                    todayDateBorderColor = DiaryColors.GoldAccent
                )
            )
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    text = "Account & Cloud Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DiaryColors.SubtleCard)
                                    .padding(10.dp)
                            ) {
                                UserAvatar(
                                    user = authState.user,
                                    size = 46.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    authState.user.displayName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DiaryColors.TextPrimary
                                        )
                                    }
                                    Text(
                                        text = authState.user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DiaryColors.TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cloud storage: Google Drive (appDataFolder)",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiaryColors.SageAccent
                            )
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DiaryColors.SubtleCard)
                                    .padding(10.dp)
                            ) {
                                UserAvatar(
                                    user = null,
                                    size = 46.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Guest User",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DiaryColors.TextPrimary
                                    )
                                    Text(
                                        text = "Offline Guest Profile",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DiaryColors.TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Data is stored locally on this device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiaryColors.TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (authState is AuthState.Authenticated) {
                        Button(
                            onClick = {
                                showAccountDialog = false
                                onSyncClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DiaryColors.GoldAccent)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showAccountDialog = false
                            onSignOutClick()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Sign Out")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Top row: App title & Sync Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DAILY PLANNER",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 24.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sync Icon Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (syncState) {
                                SyncState.SYNCED -> DiaryColors.SageSoft
                                SyncState.SYNC_PENDING -> DiaryColors.PeachSoft
                                SyncState.SYNC_ERROR -> DiaryColors.RoseSoft
                                SyncState.LOCAL_ONLY -> DiaryColors.SubtleCard
                            }
                        )
                        .clickable { onSyncClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (syncState) {
                                SyncState.SYNCED -> Icons.Default.CloudDone
                                SyncState.SYNC_PENDING -> Icons.Default.CloudSync
                                else -> Icons.Default.CloudQueue
                            },
                            contentDescription = "Sync",
                            tint = when (syncState) {
                                SyncState.SYNCED -> DiaryColors.SageAccent
                                SyncState.SYNC_PENDING -> DiaryColors.PeachAccent
                                SyncState.SYNC_ERROR -> DiaryColors.RoseAccent
                                SyncState.LOCAL_ONLY -> DiaryColors.TextSecondary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (syncState) {
                                SyncState.SYNCED -> "Synced"
                                SyncState.SYNC_PENDING -> "Pending"
                                SyncState.SYNC_ERROR -> "Sync Error"
                                SyncState.LOCAL_ONLY -> "Offline"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = DiaryColors.TextSecondary
                        )
                    }
                }

                // Download PDF Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DiaryColors.PeachSoft)
                        .clickable(enabled = !isExportingPdf) { onDownloadPdfClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = DiaryColors.GoldAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download PDF",
                                tint = DiaryColors.GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isExportingPdf) "Exporting..." else "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryColors.TextPrimary
                        )
                    }
                }

                // Profile Avatar / Settings button
                UserAvatar(
                    user = (authState as? AuthState.Authenticated)?.user,
                    size = 30.dp,
                    modifier = Modifier.clickable { showAccountDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date selector bar with arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DiaryColors.SubtleCard)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = DiaryColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Open Calendar",
                    tint = DiaryColors.GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryColors.TextPrimary
                )
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = DiaryColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Renders the user's Google profile picture.
 * If the profile picture URL is not set or fails to load, gracefully falls back to the user's first letter initial.
 * If unauthenticated, displays the generic account icon.
 */
@Composable
fun UserAvatar(
    user: AuthUser?,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DiaryColors.SubtleCard)
            .border(1.dp, DiaryColors.BorderSubtle, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (user != null) {
            val initial = user.displayName?.firstOrNull()?.uppercase()
                ?: user.email.firstOrNull()?.uppercase()
                ?: "U"

            if (!user.photoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = user.photoUrl,
                    contentDescription = user.displayName ?: "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DiaryColors.GoldAccent
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DiaryColors.GoldAccent
                            )
                        }
                    }
                )
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DiaryColors.GoldAccent
                )
            }
        } else {
            Text(
                text = "G",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.48f).sp,
                color = DiaryColors.GoldAccent
            )
        }
    }
}

