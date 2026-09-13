package com.dialy.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing daily schedule entries.
 */
@Entity(
    tableName = "schedule_items",
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
data class ScheduleEntity(
    @PrimaryKey
    val id: String,
    val plannerDate: String,
    val timeSlot: String,
    val activity: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
