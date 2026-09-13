package com.dialy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dialy.app.data.local.entities.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Todo items.
 */
@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    fun getTodosByDate(date: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE plannerDate = :date ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getTodosByDateOnce(date: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Upsert
    suspend fun upsertTodo(todo: TodoEntity)

    @Upsert
    suspend fun upsertTodos(todos: List<TodoEntity>)

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCompletion(id: String, isCompleted: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE todos SET orderIndex = :orderIndex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrder(id: String, orderIndex: Int, updatedAt: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    @Query("DELETE FROM todos WHERE plannerDate = :date")
    suspend fun deleteTodosByDate(date: String)
}
