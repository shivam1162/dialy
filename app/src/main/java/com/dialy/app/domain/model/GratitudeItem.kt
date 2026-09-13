package com.dialy.app.domain.model

import androidx.compose.runtime.Immutable
import com.dialy.app.core.util.IdGenerator
import kotlinx.serialization.Serializable

/**
 * Domain model for daily gratitude entries.
 */
@Immutable
@Serializable
data class GratitudeItem(
    val id: String = IdGenerator.generate(),
    val plannerDate: String,
    val text: String,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
