package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String, // e.g. "EMP-001"
    val name: String,
    val position: String, // Jabatan
    val department: String, // Divisi / Bagian
    val phone: String, // No WA
    val dailyRate: Double = 0.0, // Uang harian / tunjangan
    val monthlySalary: Double = 0.0, // Gaji pokok bulanan
    val isActive: Boolean = true
)
