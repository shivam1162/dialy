package com.dialy.app.core.notification

import java.util.UUID

enum class NotificationType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
