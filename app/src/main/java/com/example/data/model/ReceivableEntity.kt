package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receivables")
data class ReceivableEntity(
    @PrimaryKey val id: String,
    val type: String = "PIUTANG", // "PIUTANG" (Receivable) or "HUTANG" (Payable)
    val date: String,
    val customerName: String,
    val description: String,
    val totalAmount: Double,
    val dueDate: String,
    val paidAmount: Double = 0.0,
    val targetAccount: String = "",
    val notes: String = "",
    val status: String = "Belum Jatuh Tempo",
    @ColumnInfo(defaultValue = "''") val project: String = "",
    @ColumnInfo(defaultValue = "'PT'") val fundBucket: String = "PT"
) {
    val remainingAmount: Double
        get() = (totalAmount - paidAmount).coerceAtLeast(0.0)

    val isSettled: Boolean
        get() = remainingAmount <= 0.0
}
