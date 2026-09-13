package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.PriorityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Top Priorities.
 */
@Dao
interface PriorityDao {

    @Query("SELECT * FROM priorities WHERE plannerDate = :date ORDER BY orderIndex ASC")
    fun getPrioritiesByDate(date: String): Flow<List<PriorityEntity>>

    @Query("SELECT * FROM priorities WHERE plannerDate = :date ORDER BY orderIndex ASC")
    suspend fun getPrioritiesByDateOnce(date: String): List<PriorityEntity>

    @Upsert
    suspend fun upsertPriorities(priorities: List<PriorityEntity>)

    @Query("DELETE FROM priorities WHERE plannerDate = :date")
    suspend fun deletePrioritiesByDate(date: String)
}
