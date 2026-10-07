package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BankReconEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.KasRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: KasRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KasRepository(
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            budgetDao = db.budgetDao(),
            receivableDao = db.receivableDao(),
            noteDao = db.noteDao(),
            employeeDao = db.employeeDao(),
            attendanceDao = db.attendanceDao(),
            auditDao = db.auditDao(),
            bankReconDao = db.bankReconDao()
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun freshDatabaseStartsWithoutSeedData() = runBlocking {
        assertTrue(db.accountDao().getAllAccounts().first().isEmpty())
        assertTrue(db.transactionDao().getAllActiveTransactions().first().isEmpty())
        assertTrue(db.budgetDao().getAllBudgets().first().isEmpty())
        assertTrue(db.employeeDao().getAllActiveEmployees().first().isEmpty())
        assertTrue(db.attendanceDao().getAllAttendances().first().isEmpty())
        assertTrue(db.auditDao().getRecentAuditLogs().first().isEmpty())
    }
    @Test
    fun testReadAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sistem Kas", appName)
    }

    @Test
    fun testRoomDatabaseArchitecture() = runBlocking {
        val accountDao = db.accountDao()
        val transactionDao = db.transactionDao()
        val budgetDao = db.budgetDao()
        val employeeDao = db.employeeDao()
        val attendanceDao = db.attendanceDao()
        val auditDao = db.auditDao()
        val bankReconDao = db.bankReconDao()

        // 1. Store Account locally
        val account = AccountEntity(
            id = "acc_tunai",
            name = "Kas Tunai",
            type = "Kas",
            initialBalance = 1000000.0,
            colorHex = "#16A34A"
        )
        accountDao.insertAccount(account)

        val retrievedAccount = accountDao.getAccountById("acc_tunai")
        assertNotNull(retrievedAccount)
        assertEquals("Kas Tunai", retrievedAccount?.name)

        // 2. Store Kas Masuk & Kas Keluar Transactions
        val kmTx = TransactionEntity(
            id = "KM-20261007-001",
            type = "MASUK",
            date = "2026-10-07",
            time = "10:30:00",
            account = "Kas Tunai",
            name = "Penjualan Toko",
            category = "Penjualan",
            description = "Penjualan retail tunai",
            amount = 750000.0,
            allocation = "Operasional",
            pic = "Admin",
            status = "Selesai"
        )
        transactionDao.insertTransaction(kmTx)

        val kkTx = TransactionEntity(
            id = "KK-20261007-002",
            type = "KELUAR",
            date = "2026-10-07",
            time = "14:15:00",
            account = "Kas Tunai",
            name = "Beli Perlengkapan",
            category = "Belanja Barang",
            description = "ATK kantor",
            amount = 150000.0,
            allocation = "Operasional",
            pic = "Admin",
            status = "Selesai"
        )
        transactionDao.insertTransaction(kkTx)

        val activeTxList = transactionDao.getAllActiveTransactions().first()
        assertEquals(2, activeTxList.size)

        // 3. Store Budget Category
        val budget = BudgetEntity(
            period = "2026-10",
            category = "Operasional",
            budgetAmount = 5000000.0,
            notes = "Target anggaran operasional"
        )
        budgetDao.insertBudget(budget)
        val retrievedBudgets = budgetDao.getAllBudgets().first()
        assertEquals(1, retrievedBudgets.size)

        // 4. Store Employee & Attendance
        val emp = EmployeeEntity(
            id = "EMP-001",
            name = "Ahmad Fauzi",
            position = "Kasir",
            department = "Keuangan",
            phone = "6281234567890",
            dailyRate = 150000.0
        )
        employeeDao.insertEmployee(emp)
        val empList = employeeDao.getAllActiveEmployees().first()
        assertEquals(1, empList.size)

        val att = AttendanceEntity(
            id = "ATT-20261007-001",
            employeeId = "EMP-001",
            employeeName = "Ahmad Fauzi",
            department = "Keuangan",
            date = "2026-10-07",
            timeIn = "08:00",
            timeOut = "17:00",
            status = "Hadir",
            dailyAllowance = 150000.0
        )
        attendanceDao.insertAttendance(att)
        val attList = attendanceDao.getAttendancesByDate("2026-10-07").first()
        assertEquals(1, attList.size)
        assertEquals("Hadir", attList.first().status)

        // 5. Store Real Audit Log
        val auditLog = AuditLogEntity(
            dateFormatted = "2026-10-07 10:30:00",
            action = "TEST_AUDIT",
            recordId = "AUD-001",
            details = "Audit verifikasi formula",
            user = "Auditor",
            verifiedFormulaStatus = "AUDITED_OK"
        )
        auditDao.insertAuditLog(auditLog)
        val auditLogs = auditDao.getRecentAuditLogs().first()
        assertEquals(1, auditLogs.size)

        // 6. Store Bank Reconciliation
        val recon = BankReconEntity(
            id = "REC-BCA-202610",
            accountName = "Bank BCA",
            period = "2026-10",
            bookBalance = 10000000.0,
            statementBalance = 10000000.0,
            difference = 0.0,
            status = "Cocok"
        )
        bankReconDao.insertReconciliation(recon)
        val recons = bankReconDao.getAllReconciliations().first()
        assertEquals(1, recons.size)
        assertEquals("Cocok", recons.first().status)
    }
    @Test
    fun repositoryRejectsUnknownAccountTransaction() = runBlocking {
        val tx = TransactionEntity(
            id = "TX-UNKNOWN-001",
            type = "KELUAR",
            date = "2026-10-07",
            time = "10:00:00",
            account = "Akun Fiktif",
            name = "Test Invalid",
            category = "Operasional",
            description = "Harus ditolak",
            amount = 1000.0
        )

        try {
            repository.saveTransaction(tx)
            throw AssertionError("Transaksi dengan akun fiktif harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("tidak terdaftar") == true)
        }
    }

    @Test
    fun transferBalanceAndCashFlowDoNotDoubleCount() {
        val accounts = listOf(
            AccountEntity("a", "Kas Tunai", "Kas", 1000000.0),
            AccountEntity("b", "Bank BCA", "Bank", 0.0)
        )
        val transactions = listOf(
            TransactionEntity("TX-PRIOR", "MASUK", "2026-10-01", "08:00:00", "Kas Tunai", null, "Saldo Sebelumnya", "Penjualan", "", 200000.0, status = "Selesai"),
            TransactionEntity("TR-001", "TRANSFER", "2026-10-05", "09:00:00", "Kas Tunai", "Bank BCA", "Transfer", "Transfer Masuk", "", 500000.0, status = "Selesai"),
            TransactionEntity("INV-001", "KELUAR", "2026-10-06", "09:00:00", "Bank BCA", null, "Biaya Proyek", "Proyek", "", 300000.0, allocation = "Proyek", project = "P-01", status = "Selesai"),
            TransactionEntity("OP-001", "KELUAR", "2026-10-06", "10:00:00", "Kas Tunai", null, "Biaya Operasional", "Operasional", "", 100000.0, allocation = "Operasional", status = "Selesai")
        )

        val balances = repository.calculateAccountBalances(accounts, transactions)
        assertEquals(800000.0, balances.first { it.account.name == "Kas Tunai" }.currentBalance, 0.01)
        assertEquals(200000.0, balances.first { it.account.name == "Bank BCA" }.currentBalance, 0.01)

        val cashFlow = repository.calculateCashFlowStatement(transactions, accounts, "2026-10-05", "2026-10-06")
        assertEquals(1200000.0, cashFlow.openingBalance, 0.01)
        assertEquals(100000.0, cashFlow.operatingOutflow, 0.01)
        assertEquals(300000.0, cashFlow.investingOutflow, 0.01)
        assertEquals(800000.0, cashFlow.closingBalance, 0.01)
    }

    @Test
    fun integrityAuditFindsInvalidReferences() {
        val accounts = listOf(AccountEntity("a", "Kas Tunai", "Kas", 0.0))
        val tx = TransactionEntity("TX-BAD", "KELUAR", "2026-10-07", "10:00:00", "Akun Fiktif", null, "Bad Reference", "Operasional", "", 1000.0)
        val result = repository.runIntegrityAudit(accounts, listOf(tx), emptyList(), emptyList(), emptyList(), emptyList())
        assertFalse(result.passed)
        assertTrue(result.issues.any { it.contains("Akun Fiktif") })
    }

    @Test
    fun receivablePaymentCannotExceedRemaining() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("a", "Kas Tunai", "Kas", 0.0))
        db.receivableDao().insertReceivable(
            com.example.data.model.ReceivableEntity(
                id = "PT-TEST-001",
                date = "2026-10-07",
                customerName = "Pelanggan Test",
                description = "Tagihan test",
                totalAmount = 500000.0,
                dueDate = "2026-10-31",
                targetAccount = "Kas Tunai"
            )
        )

        try {
            repository.payReceivable("PT-TEST-001", 600000.0, "Kas Tunai")
            throw AssertionError("Overpayment piutang harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("melebihi sisa") == true)
        }
    }
}
