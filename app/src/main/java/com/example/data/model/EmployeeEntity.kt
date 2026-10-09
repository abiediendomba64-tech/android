package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: String,
    val department: String,
    val phone: String,
    val dailyRate: Double = 0.0,
    val monthlySalary: Double = 0.0,
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "''") val defaultProject: String = ""
)
