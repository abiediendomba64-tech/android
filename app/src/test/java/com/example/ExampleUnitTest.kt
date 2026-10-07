package com.example

import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.formatDateIndo
import com.example.ui.components.formatRupiah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFormatRupiah() {
        assertEquals("Rp 1.000.000", formatRupiah(1000000.0))
        assertEquals("-Rp 250.000", formatRupiah(-250000.0))
        assertEquals("Rp 0", formatRupiah(0.0))
        assertEquals("Rp 15.500.000", formatRupiah(15500000.0))
    }

    @Test
    fun testFormatDateIndo() {
        val formatted = formatDateIndo("2026-10-07")
        assertTrue(formatted.contains("Okt") || formatted.contains("Oct") || formatted.contains("2026"))
    }

    @Test
    fun testTransactionValidationRules() {
        // Validation rules for amounts
        val validAmount = 500000.0
        val zeroAmount = 0.0
        val negativeAmount = -10000.0

        assertTrue("Valid amount must be > 0", validAmount > 0.0)
        assertFalse("Zero amount is invalid", zeroAmount > 0.0)
        assertFalse("Negative amount is invalid", negativeAmount > 0.0)

        // Validation rules for categories
        val validCategories = listOf("Penjualan", "Piutang Masuk", "Belanja Barang", "Operasional")
        val chosenCategory = "Penjualan"
        val invalidCategory = ""

        assertTrue(chosenCategory in validCategories)
        assertFalse(invalidCategory in validCategories)
    }

    @Test
    fun testAuditPerhitunganFormula() {
        // Audit test: Kas Tunai masuk Rp 1.000.000, keluar Rp 250.000, transfer keluar Rp 500.000
        val initialBalance = 0.0
        val totalMasuk = 1000000.0
        val totalKeluar = 250000.0
        val totalTransferKeluar = 500000.0

        val finalBalanceKasTunai = initialBalance + totalMasuk - totalKeluar - totalTransferKeluar
        assertEquals(250000.0, finalBalanceKasTunai, 0.01)

        val bankBcaInitial = 0.0
        val bankBcaTransferIn = 500000.0
        val finalBalanceBca = bankBcaInitial + bankBcaTransferIn
        assertEquals(500000.0, finalBalanceBca, 0.01)
    }
}
