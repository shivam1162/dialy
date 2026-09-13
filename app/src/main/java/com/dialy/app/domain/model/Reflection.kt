package com.dialy.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model for End-of-Day Reflection containing independently updateable fields.
 */
@Serializable
data class Reflection(
    val whatWentWell: String = "",
    val whatCanImprove: String = "",
    val proudOf: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
