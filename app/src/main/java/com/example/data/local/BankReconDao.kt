package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankReconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankReconDao {
    @Query("SELECT * FROM bank_reconciliations ORDER BY reconciledAt DESC")
    fun getAllReconciliations(): Flow<List<BankReconEntity>>

    @Query("SELECT * FROM bank_reconciliations WHERE accountName = :accountName AND period = :period LIMIT 1")
    suspend fun getReconByAccountAndPeriod(accountName: String, period: String): BankReconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(recon: BankReconEntity)

    @Update
    suspend fun updateReconciliation(recon: BankReconEntity)

    @Query("DELETE FROM bank_reconciliations WHERE id = :id")
    suspend fun deleteReconciliation(id: String)

    @Query("SELECT COUNT(*) FROM bank_reconciliations WHERE LOWER(TRIM(accountName)) = LOWER(TRIM(:accountName))")
    suspend fun countReferencesToAccount(accountName: String): Int
}
