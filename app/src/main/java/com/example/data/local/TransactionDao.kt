package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isArchived = 0 ORDER BY date DESC, time DESC, inputTime DESC")
    fun getAllActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isArchived = 0 AND type = :type ORDER BY date DESC, time DESC, inputTime DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isArchived = 1 ORDER BY archivedAt DESC")
    fun getArchivedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isArchived = 1, archivedAt = :archivedAt, archivedBy = :archivedBy WHERE id = :id")
    suspend fun archiveTransaction(id: String, archivedAt: Long, archivedBy: String = "Admin")

    @Query("UPDATE transactions SET isArchived = 0, archivedAt = NULL, archivedBy = NULL WHERE id = :id")
    suspend fun restoreTransaction(id: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE isArchived = 0")
    suspend fun countActiveTransactions(): Int

    @Query(
        "SELECT COUNT(*) FROM transactions " +
            "WHERE LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) " +
            "OR (toAccount IS NOT NULL AND LOWER(TRIM(toAccount)) = LOWER(TRIM(:accountName)))"
    )
    suspend fun countReferencesToAccount(accountName: String): Int

    @Query(
        "SELECT COALESCE(SUM(CASE " +
            "WHEN status = 'Selesai' AND type = 'MASUK' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN amount " +
            "WHEN status = 'Selesai' AND type = 'KELUAR' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN -amount " +
            "WHEN status = 'Selesai' AND type = 'TRANSFER' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN -amount " +
            "WHEN status = 'Selesai' AND type = 'TRANSFER' AND LOWER(TRIM(toAccount)) = LOWER(TRIM(:accountName)) THEN amount " +
            "ELSE 0 END), 0.0) " +
            "FROM transactions"
    )
    suspend fun sumSettledAccountMovement(accountName: String): Double

    @Query(
        "SELECT COALESCE(SUM(CASE " +
            "WHEN status = 'Selesai' AND type = 'MASUK' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN amount " +
            "WHEN status = 'Selesai' AND type = 'KELUAR' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN -amount " +
            "WHEN status = 'Selesai' AND type = 'TRANSFER' AND LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) THEN -amount " +
            "WHEN status = 'Selesai' AND type = 'TRANSFER' AND LOWER(TRIM(toAccount)) = LOWER(TRIM(:accountName)) THEN amount " +
            "ELSE 0 END), 0.0) " +
            "FROM transactions " +
            "WHERE status = 'Selesai' AND date <= :endDate " +
            "AND (LOWER(TRIM(account)) = LOWER(TRIM(:accountName)) " +
            "OR LOWER(TRIM(toAccount)) = LOWER(TRIM(:accountName)))"
    )
    suspend fun sumSettledAccountMovementUntilDate(accountName: String, endDate: String): Double

    @Query("SELECT COUNT(*) FROM transactions WHERE category = :category AND receiptNo = :receiptNo")
    suspend fun countByCategoryAndReceiptNo(category: String, receiptNo: String): Int
}
