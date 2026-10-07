package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "Kas", "Bank", "E-Wallet", "Lainnya"
    val initialBalance: Double = 0.0,
    val colorHex: String = "#1E56A0",
    val isActive: Boolean = true
)
