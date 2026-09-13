package com.dialy.app.domain.model

import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Domain model for "Don't Forget" reminder items.
 */
@Serializable
data class ReminderItem(
    val id: String = IdGenerator.generate(),
    val plannerDate: String,
    val text: String,
    val isCompleted: Boolean = false,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
