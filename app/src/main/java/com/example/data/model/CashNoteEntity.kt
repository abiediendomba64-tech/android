package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_notes")
data class CashNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val content: String,
    val pic: String = "Admin",
    val priority: String = "Sedang", // "Rendah", "Sedang", "Tinggi"
    val status: String = "Open" // "Open", "Done", "Follow Up"
)
