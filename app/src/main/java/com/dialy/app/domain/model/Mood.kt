package com.dialy.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Standard mood categories for the mood tracker.
 */
@Serializable
enum class MoodType {
    VERY_HAPPY,
    HAPPY,
    NEUTRAL,
    SAD,
    STRESSED,
    TIRED,
    CALM,
    EXCITED
}

/**
 * Structured mood entry for a daily planner.
 */
@Immutable
@Serializable
data class Mood(
    val type: MoodType,
    val note: String = "",
    val score: Int = 3, // 1 to 5 scale
    val updatedAt: Long = System.currentTimeMillis()
)
