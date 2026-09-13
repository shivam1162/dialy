package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.SelfCareEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Self Care checklist items.
 */
@Dao
interface SelfCareDao {

    @Query("SELECT * FROM self_care_items WHERE plannerDate = :date ORDER BY orderIndex ASC")
    fun getSelfCareByDate(date: String): Flow<List<SelfCareEntity>>

    @Query("SELECT * FROM self_care_items WHERE plannerDate = :date ORDER BY orderIndex ASC")
    suspend fun getSelfCareByDateOnce(date: String): List<SelfCareEntity>

    @Upsert
    suspend fun upsertSelfCareItems(items: List<SelfCareEntity>)

    @Query("UPDATE self_care_items SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleSelfCare(id: String, isCompleted: Boolean, updatedAt: Long)

    @Query("DELETE FROM self_care_items WHERE plannerDate = :date")
    suspend fun deleteSelfCareByDate(date: String)
}
