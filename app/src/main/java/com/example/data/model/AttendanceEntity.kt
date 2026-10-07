package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendances")
data class AttendanceEntity(
    @PrimaryKey val id: String, // e.g. "ATT-20261007-EMP001"
    val employeeId: String,
    val employeeName: String,
    val department: String = "Operasional",
    val date: String, // "yyyy-MM-dd"
    val timeIn: String = "08:00", // Jam Masuk
    val timeOut: String = "17:00", // Jam Keluar
    val status: String = "Hadir", // "Hadir", "Izin", "Sakit", "Alpa", "Cuti"
    val overtimeHours: Double = 0.0, // Lembur (Jam)
    val dailyAllowance: Double = 0.0, // Uang kehadiran / makan harian
    val notes: String = ""
)
