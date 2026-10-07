package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_reconciliations")
data class BankReconEntity(
    @PrimaryKey val id: String, // e.g. "REC-BCA-202610"
    val accountName: String, // e.g. "Bank BCA"
    val period: String, // "2026-10"
    val bookBalance: Double, // Saldo Buku Kas
    val statementBalance: Double, // Saldo Rekening Koran Bank
    val difference: Double, // Selisih = statementBalance - bookBalance
    val status: String = "Cocok", // "Cocok", "Selisih"
    val reconciledBy: String = "Bendahara",
    val reconciledAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
