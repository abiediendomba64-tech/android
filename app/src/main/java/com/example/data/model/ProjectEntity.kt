package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val businessModel: String,
    val location: String,
    val startDate: String,
    val targetEndDate: String,
    val budgetAmount: Double,
    val status: String,
    val notes: String,
    val isActive: Boolean,
    val createdAt: Long
)
