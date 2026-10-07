package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY dueDate ASC, date DESC")
    fun getAllReceivables(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE id = :id LIMIT 1")
    suspend fun getReceivableById(id: String): ReceivableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: ReceivableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivables(receivables: List<ReceivableEntity>)

    @Update
    suspend fun updateReceivable(receivable: ReceivableEntity)

    @Query("DELETE FROM receivables WHERE id = :id")
    suspend fun deleteReceivable(id: String)
}
