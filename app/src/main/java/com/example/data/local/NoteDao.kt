package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CashNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM cash_notes ORDER BY date DESC, id DESC")
    fun getAllNotes(): Flow<List<CashNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CashNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<CashNoteEntity>)

    @Update
    suspend fun updateNote(note: CashNoteEntity)

    @Query("DELETE FROM cash_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
