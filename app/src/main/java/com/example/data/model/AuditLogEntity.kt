package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String,
    val action: String,
    val recordId: String,
    val details: String,
    val user: String = "Admin",
    val verifiedFormulaStatus: String = "RECORDED",
    val balanceAfter: Double = 0.0
)
