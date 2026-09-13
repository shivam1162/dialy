package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.sync.SyncState
import com.dialy.app.core.util.DateUtils
import com.dialy.app.presentation.theme.DiaryColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HeaderSection(
    currentDateString: String,
    syncState: SyncState = SyncState.LOCAL_ONLY,
    authState: AuthState = AuthState.Unauthenticated,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val date = DateUtils.parseIsoDate(currentDateString) ?: LocalDate.now()
    val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())).uppercase(Locale.getDefault())
    val formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))

    var showAccountDialog by remember { mutableStateOf(false) }

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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            Text("Signed in as:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = authState.user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            authState.user.displayName?.let {
                                Text(text = it, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cloud storage: Google Drive (appDataFolder)",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiaryColors.SageAccent
                            )
                        }
                        else -> {
                            Text(
                                text = "Running in Offline / Guest Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DiaryColors.TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (authState is AuthState.Authenticated) {
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

                // Profile Avatar / Settings button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DiaryColors.SubtleCard)
                        .border(1.dp, DiaryColors.BorderSubtle, CircleShape)
                        .clickable { showAccountDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (authState is AuthState.Authenticated) {
                        val initial = authState.user.displayName?.firstOrNull()?.uppercase()
                            ?: authState.user.email.firstOrNull()?.uppercase()
                            ?: "U"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DiaryColors.GoldAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            tint = DiaryColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
                modifier = Modifier.clickable { onToday() }
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Today",
                    tint = DiaryColors.GoldAccent,
                    modifier = Modifier.size(14.dp)
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
