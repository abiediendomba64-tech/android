package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val date: String,
    val time: String,
    val account: String,
    val toAccount: String? = null,
    val name: String,
    val category: String,
    val description: String,
    val amount: Double,
    val allocation: String = "Operasional",
    val pic: String = "Admin",
    val proofUrl: String = "",
    val receiptNo: String = "",
    val project: String = "",
    val note: String = "",
    val status: String = "Selesai",
    val inputTime: Long = System.currentTimeMillis(),
    val inputBy: String = "Admin",
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val archivedBy: String? = null,
    @ColumnInfo(defaultValue = "'PT'") val fundBucket: String = "PT"
)
