package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendances")
data class AttendanceEntity(
    @PrimaryKey val id: String, // e.g. "ATT-20261007-EMP001"
    val employeeId: String,
    val employeeName: String,
    val department: String = "",
    val date: String, // "yyyy-MM-dd"
    val timeIn: String = "", // Jam Masuk
    val timeOut: String = "", // Jam Keluar
    val status: String = "", // "Hadir", "Izin", "Sakit", "Alpa", "Cuti"
    val overtimeHours: Double = 0.0, // Lembur (Jam)
    val dailyAllowance: Double = 0.0, // Uang kehadiran / makan harian
    val notes: String = ""
)
