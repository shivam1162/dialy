package com.dialy.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing gratitude items.
 */
@Entity(
    tableName = "gratitude_items",
    indices = [Index(value = ["plannerDate"])],
    foreignKeys = [
        ForeignKey(
            entity = DailyPlannerEntity::class,
            parentColumns = ["date"],
            childColumns = ["plannerDate"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GratitudeEntity(
    @PrimaryKey
    val id: String,
    val plannerDate: String,
    val text: String,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
