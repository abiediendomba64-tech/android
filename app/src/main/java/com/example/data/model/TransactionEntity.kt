package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // "MASUK", "KELUAR", "TRANSFER"
    val date: String, // "yyyy-MM-dd"
    val time: String, // "HH:mm:ss"
    val account: String, // Dari Akun / Akun Utama
    val toAccount: String? = null, // Ke Akun (hanya untuk TRANSFER)
    val name: String, // Nama Transaksi
    val category: String, // Kategori
    val description: String, // Keterangan
    val amount: Double, // Nominal
    val allocation: String = "Operasional", // Alokasi Anggaran
    val pic: String = "Admin", // PIC
    val proofUrl: String = "", // Bukti Foto / Link
    val receiptNo: String = "", // No. Bukti
    val project: String = "", // Proyek / Unit
    val note: String = "", // Catatan
    val status: String = "Selesai", // "Selesai", "Draft", "Pending", "Batal", "Dihapus"
    val inputTime: Long = System.currentTimeMillis(), // Waktu Input Permanen
    val inputBy: String = "Admin", // Input Oleh
    val isArchived: Boolean = false, // Transaksi_Dihapus flag
    val archivedAt: Long? = null,
    val archivedBy: String? = null
)
