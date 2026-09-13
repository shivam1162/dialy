package com.dialy.app.presentation.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.R
import com.dialy.app.core.auth.AuthState
import com.dialy.app.presentation.theme.DiaryColors
import com.dialy.app.presentation.theme.DiaryTheme

@Composable
fun AuthScreen(
    authState: AuthState,
    onSignInClick: () -> Unit,
    onMockSignInClick: (String) -> Unit,
    onSkipGuestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomEmailField by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }

    DiaryTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DiaryColors.PaperBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Dialy Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, DiaryColors.BorderSubtle, RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dialy",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = DiaryColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your daily focus, to-dos, habits & reflection synced securely to Google Drive.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = DiaryColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Lock,
                            iconTint = DiaryColors.RoseAccent,
                            iconBg = DiaryColors.RoseSoft,
                            title = "User-Owned Privacy",
                            subtitle = "Private Google Drive app-data folder. Only you have access."
                        )

                        FeatureRow(
                            icon = Icons.Default.OfflineBolt,
                            iconTint = DiaryColors.SageAccent,
                            iconBg = DiaryColors.SageSoft,
                            title = "100% Offline-First",
                            subtitle = "Full offline diary. Changes sync seamlessly when connected."
                        )

                        FeatureRow(
                            icon = Icons.Default.CloudDone,
                            iconTint = DiaryColors.PeachAccent,
                            iconBg = DiaryColors.PeachSoft,
                            title = "Automatic Cloud Backup",
                            subtitle = "Secure backup for your journals, habits, and reflections."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner with friendly explanation
                if (authState is AuthState.Error) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DiaryColors.RoseSoft),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.RoseAccent)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "⚠️ Google Sign-In Status:",
                                color = DiaryColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = authState.message,
                                color = DiaryColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Tip: To test immediately, use 'One-Tap Google Login' below.",
                                color = DiaryColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Native Google Sign In Button
                Button(
                    onClick = onSignInClick,
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiaryColors.TextPrimary,
                        contentColor = Color.White
                    )
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DiaryColors.GoldAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign In with Google",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct One-Tap Login Button (Instantly starts session with user's Google email)
                Button(
                    onClick = {
                        showCustomEmailField = !showCustomEmailField
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiaryColors.GoldAccent,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showCustomEmailField) "Hide Email Login" else "One-Tap Google Login",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (showCustomEmailField || authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DiaryColors.CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Enter your Google email:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = DiaryColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customEmail,
                                    onValueChange = { customEmail = it },
                                    placeholder = { Text("my.email@gmail.com", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val email = if (customEmail.isNotBlank()) customEmail.trim() else "my.diary@gmail.com"
                                        onMockSignInClick(email)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DiaryColors.SageAccent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Login")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guest / Offline Mode
                OutlinedButton(
                    onClick = onSkipGuestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DiaryColors.TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiaryColors.BorderSubtle)
                ) {
                    Text(
                        text = "Continue Offline / Guest Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DiaryColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DiaryColors.TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}
