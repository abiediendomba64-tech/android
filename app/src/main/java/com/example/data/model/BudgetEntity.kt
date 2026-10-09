package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val category: String,
    val budgetAmount: Double,
    val notes: String = "",
    @ColumnInfo(defaultValue = "''") val project: String = "",
    @ColumnInfo(defaultValue = "'PT'") val fundBucket: String = "PT"
)
