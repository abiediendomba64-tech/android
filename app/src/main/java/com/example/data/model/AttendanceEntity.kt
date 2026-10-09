package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendances")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val employeeName: String,
    val department: String = "",
    val date: String,
    val timeIn: String = "",
    val timeOut: String = "",
    val status: String = "",
    val overtimeHours: Double = 0.0,
    val dailyAllowance: Double = 0.0,
    val notes: String = "",
    @ColumnInfo(defaultValue = "''") val project: String = ""
)
