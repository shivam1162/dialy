package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.GratitudeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Gratitude entries.
 */
@Dao
interface GratitudeDao {

    @Query("SELECT * FROM gratitude_items WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    fun getGratitudeByDate(date: String): Flow<List<GratitudeEntity>>

    @Query("SELECT * FROM gratitude_items WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getGratitudeByDateOnce(date: String): List<GratitudeEntity>

    @Upsert
    suspend fun upsertGratitude(items: List<GratitudeEntity>)

    @Query("DELETE FROM gratitude_items WHERE plannerDate = :date")
    suspend fun deleteGratitudeByDate(date: String)
}
