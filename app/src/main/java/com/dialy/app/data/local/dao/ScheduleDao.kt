package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.ScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Schedule entries.
 */
@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_items WHERE plannerDate = :date ORDER BY timeSlot ASC")
    fun getScheduleByDate(date: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_items WHERE plannerDate = :date ORDER BY timeSlot ASC")
    suspend fun getScheduleByDateOnce(date: String): List<ScheduleEntity>

    @Upsert
    suspend fun upsertSchedule(schedule: List<ScheduleEntity>)

    @Query("DELETE FROM schedule_items WHERE plannerDate = :date")
    suspend fun deleteScheduleByDate(date: String)
}
