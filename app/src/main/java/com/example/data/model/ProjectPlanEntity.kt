package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_plans")
data class ProjectPlanEntity(
    @PrimaryKey val id: String,
    val project: String,
    val planDate: String,
    val title: String,
    val category: String,
    val estimatedAmount: Double,
    val status: String,
    val details: String,
    val createdAt: Long
)
