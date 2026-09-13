package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.DailyPlannerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Daily Planner core entries.
 */
@Dao
interface DailyPlannerDao {

    @Query("SELECT * FROM daily_planners WHERE date = :date")
    fun getPlannerByDate(date: String): Flow<DailyPlannerEntity?>

    @Query("SELECT * FROM daily_planners WHERE date = :date")
    suspend fun getPlannerByDateOnce(date: String): DailyPlannerEntity?

    @Query("SELECT * FROM daily_planners ORDER BY date DESC")
    fun getAllPlanners(): Flow<List<DailyPlannerEntity>>

    @Query("SELECT * FROM daily_planners ORDER BY date DESC")
    suspend fun getAllPlannersOnce(): List<DailyPlannerEntity>

    @Upsert
    suspend fun upsertPlanner(planner: DailyPlannerEntity)

    @Query("DELETE FROM daily_planners WHERE date = :date")
    suspend fun deletePlanner(date: String)

    @Query("SELECT EXISTS(SELECT 1 FROM daily_planners WHERE date = :date)")
    suspend fun plannerExists(date: String): Boolean

    @Query("UPDATE daily_planners SET focus = :focus, updatedAt = :updatedAt, syncState = 'SYNC_PENDING' WHERE date = :date")
    suspend fun updateFocus(date: String, focus: String, updatedAt: Long)

    @Query("UPDATE daily_planners SET notes = :notes, updatedAt = :updatedAt, syncState = 'SYNC_PENDING' WHERE date = :date")
    suspend fun updateNotes(date: String, notes: String, updatedAt: Long)

    @Query("UPDATE daily_planners SET moodJson = :moodJson, updatedAt = :updatedAt, syncState = 'SYNC_PENDING' WHERE date = :date")
    suspend fun updateMood(date: String, moodJson: String?, updatedAt: Long)

    @Query("UPDATE daily_planners SET reflectionJson = :reflectionJson, updatedAt = :updatedAt, syncState = 'SYNC_PENDING' WHERE date = :date")
    suspend fun updateReflection(date: String, reflectionJson: String, updatedAt: Long)

    @Query("UPDATE daily_planners SET dailyReminder = :reminder, updatedAt = :updatedAt, syncState = 'SYNC_PENDING' WHERE date = :date")
    suspend fun updateDailyReminder(date: String, reminder: String, updatedAt: Long)

    @Query("UPDATE daily_planners SET syncState = :syncState, lastSyncedAt = :lastSyncedAt WHERE date = :date")
    suspend fun updateSyncState(date: String, syncState: String, lastSyncedAt: Long?)
}
