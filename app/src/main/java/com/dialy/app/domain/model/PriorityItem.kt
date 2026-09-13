package com.dialy.app.domain.model

import androidx.compose.runtime.Immutable
import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Domain model for Top Priority items (typically 3 primary priorities per day).
 */
@Immutable
@Serializable
data class PriorityItem(
    val id: String = IdGenerator.generate(),
    val plannerDate: String,
    val order: Int, // 1, 2, or 3
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
