package com.dialy.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a Todo list item.
 */
@Entity(
    tableName = "todos",
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
data class TodoEntity(
    @PrimaryKey
    val id: String,
    val plannerDate: String,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
