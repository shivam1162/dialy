package com.dialy.app.domain.model

import androidx.compose.runtime.Immutable
import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Domain model for daily schedule slots.
 */
@Immutable
@Serializable
data class ScheduleItem(
    val id: String = IdGenerator.generate(),
    val plannerDate: String,
    val timeSlot: String, // e.g. "06:00", "07:00", "08:00", etc.
    val activity: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
