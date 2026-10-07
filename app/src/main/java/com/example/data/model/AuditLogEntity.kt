package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String, // "yyyy-MM-dd HH:mm:ss"
    val action: String, // "INSERT_MASUK", "INSERT_KELUAR", "TRANSFER", "ARCHIVE", "RESTORE", "RECONCILE", "PAYROLL"
    val recordId: String,
    val details: String,
    val user: String = "Admin",
    val verifiedFormulaStatus: String = "VALID_SUMIFS", // Status audit rumus
    val balanceAfter: Double = 0.0
)
