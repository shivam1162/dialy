package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for "Don't Forget" reminders.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    fun getRemindersByDate(date: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getRemindersByDateOnce(date: String): List<ReminderEntity>

    @Upsert
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleReminder(id: String, isCompleted: Boolean, updatedAt: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE plannerDate = :date")
    suspend fun deleteRemindersByDate(date: String)
}
