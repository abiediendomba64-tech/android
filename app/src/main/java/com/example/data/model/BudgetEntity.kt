package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String, // "2026-10"
    val category: String, // "Operasional", "Gaji", "Marketing", "Proyek", "Lainnya"
    val budgetAmount: Double,
    val notes: String = ""
)
