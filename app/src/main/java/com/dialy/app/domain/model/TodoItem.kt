package com.dialy.app.domain.model

import androidx.compose.runtime.Immutable
import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Domain model for a Todo list item.
 */
@Immutable
@Serializable
data class TodoItem(
    val id: String = IdGenerator.generate(),
    val plannerDate: String,
    val title: String,
    val isCompleted: Boolean = false,
    val order: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
