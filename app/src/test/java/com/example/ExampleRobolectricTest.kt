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
import com.example.data.model.HousingUnitEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectPlanEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.KasRepository
import kotlinx.coroutines.async
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
            database = db,
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            budgetDao = db.budgetDao(),
            receivableDao = db.receivableDao(),
            noteDao = db.noteDao(),
            employeeDao = db.employeeDao(),
            attendanceDao = db.attendanceDao(),
            auditDao = db.auditDao(),
            bankReconDao = db.bankReconDao(),
            projectDao = db.projectDao(),
            projectPlanDao = db.projectPlanDao(),
            housingUnitDao = db.housingUnitDao()
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
        assertEquals(600000.0, balances.first { it.account.name == "Kas Tunai" }.currentBalance, 0.01)
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

    @Test
    fun spreadsheetExportUsesCompleteQuotedRoundTripFormat() {
        val tx = TransactionEntity(
            id = "TX-CSV-001",
            type = "KELUAR",
            date = "2026-10-07",
            time = "10:00:00",
            account = "Kas Tunai",
            name = "Belanja, ATK",
            category = "Operasional",
            description = "Baris satu\nBaris dua",
            amount = 1250000.5,
            allocation = "Operasional",
            pic = "Admin",
            proofUrl = "https://example.test/a,b",
            receiptNo = "INV/001",
            project = "P-01",
            note = "Catatan \"penting\"",
            status = "Selesai",
            inputTime = 1791344400123L,
            inputBy = "Admin"
        )
        val csv = repository.exportTransactionsToCsv(listOf(tx))

        assertTrue(csv.startsWith("\uFEFFID,Tipe,Tanggal"))
        assertTrue(csv.contains("\"Belanja, ATK\""))
        assertTrue(csv.contains("\"Baris satu\nBaris dua\""))
        assertTrue(csv.contains("\"Catatan \"\"penting\"\"\""))
        assertTrue(csv.contains("1250000.5"))
        assertTrue(csv.contains("\"https://example.test/a,b\""))
        assertTrue(csv.contains("TX-CSV-001"))
    }

    @Test
    fun spreadsheetImportParsesQuotedCommaAndDecimalAndRejectsDuplicateId() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("csv-account", "Kas Tunai", "Kas", 0.0))
        repository.saveProject(ProjectEntity(
            id = "PRJ-CSV-001",
            name = "P-01",
            category = "Lainnya",
            businessModel = "Tidak berlaku",
            location = "",
            startDate = "2026-10-01",
            targetEndDate = "2026-12-31",
            budgetAmount = 0.0,
            status = "Berjalan",
            notes = "",
            isActive = true,
            createdAt = 1790812800000L
        ))

        val csv = "\uFEFFID,Tipe,Tanggal,Jam,Akun,Ke Akun,Nama Transaksi,Kategori,Keterangan,Nominal,Alokasi,PIC,Bukti,No Bukti,Proyek,Catatan,Status,Waktu Input,Input Oleh,Diarsipkan,Waktu Arsip,Diarsipkan Oleh\n" +
            "TX-CSV-IMPORT-001,KELUAR,2026-10-07,10:00:00,Kas Tunai,,\"Belanja, ATK\",Operasional,\"Keterangan, lengkap\",1.250.000,Operasional,Admin,,INV-001,P-01,\"Catatan \"\"A\"\"\",Selesai,2026-10-07 10:00:00.000,Admin,TIDAK,,\n"

        val result = repository.importTransactionsFromCsv(csv)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals("Belanja, ATK", db.transactionDao().getTransactionById("TX-CSV-IMPORT-001")?.name)
        assertEquals(1250000.0, db.transactionDao().getTransactionById("TX-CSV-IMPORT-001")?.amount ?: 0.0, 0.01)

        val duplicate = repository.importTransactionsFromCsv(csv)
        assertFalse(duplicate.isSuccess)
        assertTrue(duplicate.exceptionOrNull()?.message?.contains("sudah ada") == true)
    }


    @Test
    fun transactionAccountStreamExcludesInactiveAccountsButHistoryStreamKeepsThem() = runBlocking {
        db.accountDao().insertAccounts(listOf(
            AccountEntity("acc-active-picker", "Kas Aktif", "Kas", 100000.0, isActive = true),
            AccountEntity("acc-inactive-picker", "Bank Nonaktif", "Bank", 200000.0, isActive = false)
        ))
        assertEquals(listOf("Kas Aktif"), repository.activeAccounts.first().map { it.name })
        assertEquals(setOf("Kas Aktif", "Bank Nonaktif"), repository.accounts.first().map { it.name }.toSet())
    }

    @Test
    fun periodAndAccountIdentityRulesAreEnforced() = runBlocking {
        repository.saveBudget(BudgetEntity(period = "2026-10", category = "Operasional", budgetAmount = 1000000.0))
        db.accountDao().insertAccount(AccountEntity("acc-1", "Kas Tunai", "Kas", 0.0))
        try {
            repository.saveAccount(AccountEntity("acc-2", "kas tunai", "Kas", 0.0))
            throw AssertionError("Nama akun yang sama tanpa memperhatikan huruf besar/kecil harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("sudah digunakan") == true)
        }
        repository.saveBankReconciliation(
            BankReconEntity("REC-TEST-2026-10", "Kas Tunai", "2026-10", 0.0, 0.0, 0.0, "Belum Diverifikasi")
        )
        assertEquals("2026-10", db.bankReconDao().getAllReconciliations().first().first().period)
    }

    @Test
    fun updateTransactionRejectsInvalidReplacementAndKeepsOriginal() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-1", "Kas Tunai", "Kas", 0.0))
        val original = TransactionEntity("TX-UPD-001", "KELUAR", "2026-10-01", "10:00:00", "Kas Tunai", null, "Original", "Operasional", "", 100000.0, status = "Selesai")
        db.transactionDao().insertTransaction(original)
        val invalid = original.copy(amount = -1.0)
        try {
            repository.updateTransaction(invalid, original)
            throw AssertionError("Edit transaksi dengan nominal invalid harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("lebih besar") == true)
        }
        assertEquals(100000.0, db.transactionDao().getTransactionById(original.id)?.amount ?: 0.0, 0.01)
    }

    @Test
    fun concurrentReceivablePaymentsAreAtomicAndCannotDoublePay() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-1", "Kas Tunai", "Kas", 0.0))
        db.receivableDao().insertReceivable(
            com.example.data.model.ReceivableEntity(
                id = "PT-ATOMIC-001", date = "2026-10-07", customerName = "Pelanggan Atomic",
                description = "Tagihan", totalAmount = 300000.0, dueDate = "2026-10-31", targetAccount = "Kas Tunai"
            )
        )
        val results = kotlinx.coroutines.coroutineScope {
            val first = async { runCatching { repository.payReceivable("PT-ATOMIC-001", 200000.0, "Kas Tunai") } }
            val second = async { runCatching { repository.payReceivable("PT-ATOMIC-001", 200000.0, "Kas Tunai") } }
            listOf(first.await(), second.await())
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        assertEquals(200000.0, db.receivableDao().getReceivableById("PT-ATOMIC-001")?.paidAmount ?: 0.0, 0.01)
        assertEquals(1, db.transactionDao().getAllActiveTransactions().first().size)
    }

    @Test
    fun historicalBankBalanceUsesReconciliationPeriodNotCurrentBalance() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-1", "Bank BCA", "Bank", 1000000.0))
        db.transactionDao().insertTransactions(listOf(
            TransactionEntity("TX-SEP", "MASUK", "2026-09-15", "10:00:00", "Bank BCA", null, "September", "Penjualan", "", 100000.0, status = "Selesai"),
            TransactionEntity("TX-OCT", "MASUK", "2026-10-05", "10:00:00", "Bank BCA", null, "October", "Penjualan", "", 500000.0, status = "Selesai")
        ))
        repository.saveBankReconciliation(
            BankReconEntity("REC-BCA-2026-09", "Bank BCA", "2026-09", 0.0, 1100000.0, 0.0, "Belum Diverifikasi")
        )
        val recon = db.bankReconDao().getAllReconciliations().first().single()
        assertEquals(1100000.0, recon.bookBalance, 0.01)
        assertEquals(0.0, recon.difference, 0.01)
        assertEquals("Cocok", recon.status)
    }

    @Test
    fun payrollTotalMatchesSlipCalculationAndLedgerDisbursement() = runBlocking {
        val emp = EmployeeEntity("EMP-PAY-001", "Ahmad", "Staff", "Operasional", "", 100000.0, 3000000.0)
        db.employeeDao().insertEmployee(emp)
        val attendances = listOf(
            AttendanceEntity("ATT-PAY-01", emp.id, emp.name, emp.department, "2026-10-01", "08:00", "17:00", "Hadir", 2.0, 100000.0),
            AttendanceEntity("ATT-PAY-02", emp.id, emp.name, emp.department, "2026-10-02", "08:00", "17:00", "Alpa", 0.0, 0.0)
        )
        db.attendanceDao().insertAttendances(attendances)
        val payroll = repository.calculatePayrollForEmployee(emp, attendances)
        val total = repository.calculatePayrollTotal(listOf(emp), attendances)
        assertEquals(3000000.0, payroll.baseSalary, 0.01)
        assertEquals(100000.0, payroll.attendanceAllowance, 0.01)
        assertEquals(37500.0, payroll.overtimePay, 0.01)
        assertEquals(100000.0, payroll.absenceDeduction, 0.01)
        assertEquals(3037500.0, payroll.net, 0.01)
        assertEquals(payroll.net, total, 0.01)

        db.accountDao().insertAccount(AccountEntity("acc-pay", "Kas Tunai", "Kas", 5000000.0))
        repository.savePayrollDisbursement("2026-10", total, "Kas Tunai")
        val payrollTx = db.transactionDao().getAllActiveTransactions().first().single()
        assertEquals(total, payrollTx.amount, 0.01)
        assertEquals("PAYROLL-2026-10", payrollTx.receiptNo)
        try {
            repository.savePayrollDisbursement("2026-10", total, "Kas Tunai")
            throw AssertionError("Payroll periode yang sama tidak boleh dicairkan dua kali.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("sudah dicairkan") == true)
        }
    }

    @Test
    fun invalidCalendarDatesAndTrailingCharactersAreRejected() = runBlocking {
        db.employeeDao().insertEmployee(
            EmployeeEntity(
                id = "EMP-STRICT-DATE",
                name = "Tester Tanggal",
                position = "Staff",
                department = "Operasional",
                phone = ""
            )
        )
        val invalidDates = listOf("2026-02-31", "2026-13-01", "2026-10-08junk", "2026-1-08")
        invalidDates.forEachIndexed { index, date ->
            try {
                repository.saveAttendance(
                    AttendanceEntity(
                        id = "ATT-STRICT-$index",
                        employeeId = "EMP-STRICT-DATE",
                        employeeName = "Tester Tanggal",
                        department = "Operasional",
                        date = date,
                        status = "Hadir"
                    )
                )
                throw AssertionError("Tanggal tidak valid seharusnya ditolak: $date")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("Tanggal absensi") == true)
            }
        }
        assertTrue(db.attendanceDao().getAllAttendances().first().isEmpty())
    }

    @Test
    fun attendanceDateValidationAcceptsIsoDate() = runBlocking {
        db.employeeDao().insertEmployee(
            EmployeeEntity(
                id = "EMP-ATT-001",
                name = "Tester",
                position = "Staff",
                department = "Operasional",
                phone = ""
            )
        )
        repository.saveAttendance(
            AttendanceEntity(
                id = "ATT-ISO-001",
                employeeId = "EMP-ATT-001",
                employeeName = "Tester",
                department = "Operasional",
                date = "2026-10-08",
                timeIn = "08:00",
                timeOut = "17:00",
                status = "Hadir",
                dailyAllowance = 0.0
            )
        )
        assertNotNull(db.attendanceDao().getAttendanceByEmployeeAndDate("EMP-ATT-001", "2026-10-08"))
    }

    @Test
    fun referencedAccountIsDeactivatedInsteadOfDeleted() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-del-001", "Kas Referensi", "Kas", 0.0))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-REF-001",
                type = "KELUAR",
                date = "2026-10-08",
                time = "10:00:00",
                account = "Kas Referensi",
                name = "Referensi",
                category = "Operasional",
                description = "",
                amount = 1000.0,
                status = "Selesai"
            )
        )

        repository.deleteAccount("acc-del-001")

        val account = db.accountDao().getAccountById("acc-del-001")
        assertNotNull(account)
        assertFalse(account?.isActive ?: true)
        assertNotNull(db.transactionDao().getTransactionById("TX-REF-001"))
    }

    @Test
    fun referencedEmployeeIsDeactivatedInsteadOfDeleted() = runBlocking {
        db.employeeDao().insertEmployee(
            EmployeeEntity(
                id = "EMP-DEL-001",
                name = "Karyawan Lama",
                position = "Staff",
                department = "Operasional",
                phone = ""
            )
        )
        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                id = "ATT-REF-001",
                employeeId = "EMP-DEL-001",
                employeeName = "Karyawan Lama",
                department = "Operasional",
                date = "2026-10-08",
                timeIn = "08:00",
                timeOut = "17:00",
                status = "Hadir"
            )
        )

        repository.deleteEmployee("EMP-DEL-001")

        val employee = db.employeeDao().getEmployeeById("EMP-DEL-001")
        assertNotNull(employee)
        assertFalse(employee?.isActive ?: true)
        assertNotNull(db.attendanceDao().getAttendanceByEmployeeAndDate("EMP-DEL-001", "2026-10-08"))
    }

    @Test
    fun invalidJsonRestoreIsAtomicAndDoesNotInsertPartialData() = runBlocking {
        val backup = """
            {
              "accounts": [
                {
                  "id": "acc-restore-001",
                  "name": "Kas Restore",
                  "type": "Kas",
                  "initialBalance": 100000,
                  "colorHex": "#1E56A0",
                  "isActive": true
                }
              ],
              "transactions": [
                {
                  "id": "TX-RESTORE-BAD",
                  "type": "KELUAR",
                  "date": "08-10-2026",
                  "time": "10:00:00",
                  "account": "Kas Restore",
                  "toAccount": null,
                  "name": "Invalid Date",
                  "category": "Operasional",
                  "description": "",
                  "amount": 1000,
                  "allocation": "Operasional",
                  "pic": "Admin",
                  "proofUrl": "",
                  "receiptNo": "",
                  "project": "",
                  "note": "",
                  "status": "Selesai",
                  "inputTime": 1791434400000,
                  "inputBy": "Admin",
                  "isArchived": false,
                  "archivedAt": null,
                  "archivedBy": null
                }
              ]
            }
        """.trimIndent()

        val result = repository.restoreDataFromJson(backup)

        assertFalse(result.isSuccess)
        assertTrue(db.accountDao().getAccountById("acc-restore-001") == null)
        assertTrue(db.transactionDao().getTransactionById("TX-RESTORE-BAD") == null)
    }

    @Test
    fun saveTransactionRejectsInvalidDateAndDuplicateId() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-validation", "Kas Validasi", "Kas", 0.0))
        val valid = TransactionEntity(
            id = "TX-VALIDATION-001",
            type = "KELUAR",
            date = "2026-10-08",
            time = "10:00:00",
            account = "Kas Validasi",
            name = "Transaksi Valid",
            category = "Operasional",
            description = "",
            amount = 1000.0,
            allocation = "Operasional",
            pic = "Admin",
            status = "Selesai",
            inputBy = "Admin"
        )
        repository.saveTransaction(valid)

        try {
            repository.saveTransaction(valid.copy(id = "TX-VALIDATION-002", date = "2026-99-99"))
            throw AssertionError("Tanggal kalender invalid harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("tanggal kalender") == true)
        }

        try {
            repository.saveTransaction(valid)
            throw AssertionError("ID transaksi duplikat harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("ID transaksi") == true)
        }
        assertEquals(1, db.transactionDao().getAllActiveTransactions().first().size)
    }

    @Test
    fun transferRejectsInactiveDestinationAccount() = runBlocking {
        db.accountDao().insertAccounts(
            listOf(
                AccountEntity("acc-source", "Kas Sumber", "Kas", 0.0),
                AccountEntity("acc-dest", "Bank Tujuan", "Bank", 0.0, isActive = false)
            )
        )
        val tx = TransactionEntity(
            id = "TR-INACTIVE-001",
            type = "TRANSFER",
            date = "2026-10-08",
            time = "10:00:00",
            account = "Kas Sumber",
            toAccount = "Bank Tujuan",
            name = "Transfer",
            category = "Transfer Masuk",
            description = "",
            amount = 1000.0,
            allocation = "Operasional",
            pic = "Admin",
            status = "Selesai",
            inputBy = "Admin"
        )
        try {
            repository.saveTransaction(tx)
            throw AssertionError("Transfer ke akun nonaktif harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("nonaktif") == true)
        }
    }

    @Test
    fun restoreCollisionDoesNotOverwriteExistingTransaction() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-restore-collision", "Kas Collision", "Kas", 0.0))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-COLLISION-001",
                type = "MASUK",
                date = "2026-10-08",
                time = "10:00:00",
                account = "Kas Collision",
                name = "Data Asli",
                category = "Penjualan",
                description = "",
                amount = 5000.0,
                allocation = "Operasional",
                pic = "Admin",
                status = "Selesai"
            )
        )
        val backup = """
            {
              "transactions": [
                {
                  "id": "TX-COLLISION-001",
                  "type": "MASUK",
                  "date": "2026-10-08",
                  "time": "10:00:00",
                  "account": "Kas Collision",
                  "toAccount": null,
                  "name": "Harus Ditolak",
                  "category": "Penjualan",
                  "description": "",
                  "amount": 999999,
                  "allocation": "Operasional",
                  "pic": "Admin",
                  "proofUrl": "",
                  "receiptNo": "",
                  "project": "",
                  "note": "",
                  "status": "Selesai",
                  "inputTime": 1791434400000,
                  "inputBy": "Admin",
                  "isArchived": false,
                  "archivedAt": null,
                  "archivedBy": null
                }
              ]
            }
        """.trimIndent()
        val result = repository.restoreDataFromJson(backup)
        assertFalse(result.isSuccess)
        assertEquals(5000.0, db.transactionDao().getTransactionById("TX-COLLISION-001")?.amount ?: 0.0, 0.01)
    }

    @Test
    fun transactionAuditStoresActualPostBalance() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-audit", "Kas Audit", "Kas", 1000.0))
        repository.saveTransaction(
            TransactionEntity(
                id = "TX-AUDIT-001",
                type = "MASUK",
                date = "2026-10-08",
                time = "10:00:00",
                account = "Kas Audit",
                name = "Pemasukan",
                category = "Penjualan",
                description = "",
                amount = 2500.0,
                allocation = "Operasional",
                pic = "Admin",
                status = "Selesai",
                inputBy = "Admin"
            )
        )
        val log = db.auditDao().getAllAuditLogs().first { it.recordId == "TX-AUDIT-001" }
        assertEquals(3500.0, log.balanceAfter, 0.01)
    }

    @Test
    fun archivedTransactionStillContributesToLedgerBalance() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-1", "Kas Tunai", "Kas", 0.0))
        val tx = TransactionEntity("TX-ARCH-001", "MASUK", "2026-10-01", "10:00:00", "Kas Tunai", null, "Archived", "Penjualan", "", 500000.0, status = "Selesai")
        db.transactionDao().insertTransaction(tx)
        repository.archiveTransaction(tx.id)
        val active = db.transactionDao().getAllActiveTransactions().first()
        val archived = db.transactionDao().getArchivedTransactions().first()
        val balance = repository.calculateAccountBalances(
            listOf(AccountEntity("acc-1", "Kas Tunai", "Kas", 0.0)),
            active + archived
        ).single().currentBalance
        assertEquals(500000.0, balance, 0.01)
    }

    @Test
    fun projectCalendarPlanDoesNotCreateActualCashTransaction() = runBlocking {
        val project = ProjectEntity(
            id = "PRJ-LAND-001",
            name = "Pembebasan Tanah A",
            category = "Pembebasan Tanah",
            businessModel = "Tidak berlaku",
            location = "Area A",
            startDate = "2026-10-01",
            targetEndDate = "2026-12-31",
            budgetAmount = 150000000.0,
            status = "Berjalan",
            notes = "Jadwal dan biaya pembebasan tanah",
            isActive = true,
            createdAt = 1790812800000L
        )
        repository.saveProject(project)

        val plan = ProjectPlanEntity(
            id = "PLAN-LAND-001",
            project = project.name,
            planDate = "2026-10-15",
            title = "Pembayaran tahap relokasi",
            category = "Pembayaran",
            estimatedAmount = 12000000.0,
            status = "Direncanakan",
            details = "Menunggu bukti dan persetujuan",
            createdAt = 1790812800000L
        )
        repository.saveProjectPlan(plan)

        assertEquals(project.name, db.projectDao().getProjectById(project.id)?.name)
        assertEquals(plan.title, db.projectPlanDao().getPlanById(plan.id)?.title)
        assertTrue(db.transactionDao().getAllActiveTransactions().first().isEmpty())
    }

    @Test
    fun housingUnitStatusDoesNotFabricateSalesRevenue() = runBlocking {
        val project = ProjectEntity(
            id = "PRJ-HOUSE-001",
            name = "Perumahan Subsidi A",
            category = "Perumahan",
            businessModel = "Subsidi",
            location = "Kawasan A",
            startDate = "2026-10-01",
            targetEndDate = "2027-12-31",
            budgetAmount = 2500000000.0,
            status = "Berjalan",
            notes = "Daftar unit per blok sesuai site plan",
            isActive = true,
            createdAt = 1790812800000L
        )
        repository.saveProject(project)
        val unit = HousingUnitEntity(
            id = "UNIT-HOUSE-A1",
            project = project.name,
            unitCode = "A1",
            block = "A",
            sitePosition = "A1",
            businessModel = "Subsidi",
            landAreaM2 = 60.0,
            buildingAreaM2 = 30.0,
            salePrice = 166000000.0,
            buyerName = "",
            status = "Tersedia",
            notes = "Data inventaris unit, bukan penerimaan kas",
            createdAt = 1790812800000L
        )
        repository.saveHousingUnit(unit)
        repository.updateHousingUnitStatus(unit.id, "Terjual", "Pembeli Test")

        assertEquals("Terjual", db.housingUnitDao().getUnitById(unit.id)?.status)
        assertEquals("Pembeli Test", db.housingUnitDao().getUnitById(unit.id)?.buyerName)
        assertTrue(db.transactionDao().getAllActiveTransactions().first().isEmpty())
    }

    @Test
    fun payrollDisbursementSplitsActualCashOutflowByProject() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-pay-projects", "Kas Payroll", "Kas", 5000000.0))
        val projectA = ProjectEntity(
            id = "PRJ-PAY-LAND",
            name = "Cut Fill A",
            category = "Cut & Fill",
            businessModel = "Tidak berlaku",
            location = "",
            startDate = "2026-10-01",
            targetEndDate = "2026-12-31",
            budgetAmount = 500000000.0,
            status = "Berjalan",
            notes = "",
            isActive = true,
            createdAt = 1790812800000L
        )
        val projectB = ProjectEntity(
            id = "PRJ-PAY-HOUSE",
            name = "Perumahan Komersial B",
            category = "Perumahan",
            businessModel = "Komersial",
            location = "",
            startDate = "2026-10-01",
            targetEndDate = "2027-12-31",
            budgetAmount = 3000000000.0,
            status = "Berjalan",
            notes = "",
            isActive = true,
            createdAt = 1790812800000L
        )
        repository.saveProject(projectA)
        repository.saveProject(projectB)

        repository.savePayrollDisbursement(
            period = "2026-11",
            totalAmount = 3000000.0,
            accountName = "Kas Payroll",
            allocationsByProject = mapOf(projectA.name to 2000000.0, projectB.name to 1000000.0)
        )

        val payrollTransactions = db.transactionDao().getAllActiveTransactions().first()
            .filter { it.category == "Gaji" && it.receiptNo == "PAYROLL-2026-11" }
        assertEquals(2, payrollTransactions.size)
        assertEquals(3000000.0, payrollTransactions.sumOf { it.amount }, 0.01)
        assertEquals(2000000.0, payrollTransactions.filter { it.project == projectA.name }.sumOf { it.amount }, 0.01)
        assertEquals(1000000.0, payrollTransactions.filter { it.project == projectB.name }.sumOf { it.amount }, 0.01)
    }


    @Test
    fun jsonBackupRoundTripsInactiveAccountsEmployeesAndProjectTaggedReceivables() = runBlocking {
        db.accountDao().insertAccounts(listOf(
            AccountEntity("BK-ACTIVE", "Kas Backup", "Kas", 100000.0, isActive = true),
            AccountEntity("BK-INACTIVE", "Bank Lama", "Bank", 250000.0, isActive = false)
        ))
        val project = ProjectEntity(
            id = "PRJ-BACKUP-001",
            name = "Perdagangan Backup",
            category = "Perdagangan",
            businessModel = "Tidak berlaku",
            location = "",
            startDate = "2026-10-01",
            targetEndDate = "2026-12-31",
            budgetAmount = 1000000.0,
            status = "Berjalan",
            notes = "",
            isActive = false,
            createdAt = 1790812800000L
        )
        repository.saveProject(project)
        db.projectPlanDao().insertPlan(
            com.example.data.model.ProjectPlanEntity(
                id = "PLAN-INACTIVE-PROJECT",
                project = project.name,
                planDate = "2026-10-15",
                title = "Agenda historis proyek",
                category = "Pembayaran",
                estimatedAmount = 200000.0,
                status = "Direncanakan",
                details = "Agenda tetap dipertahankan setelah proyek dinonaktifkan",
                createdAt = 1790812800000L
            )
        )
        db.noteDao().insertNote(
            com.example.data.model.CashNoteEntity(
                date = "2026-10-05",
                title = "Catatan proyek historis",
                content = "Catatan tetap ditautkan ke proyek nonaktif",
                project = project.name
            )
        )
        db.budgetDao().insertBudget(
            BudgetEntity(period = "2026-10", category = "Proyek", budgetAmount = 300000.0, project = project.name)
        )
        val inactiveEmployee = EmployeeEntity(
            id = "EMP-BACKUP-INACTIVE",
            name = "Karyawan Nonaktif",
            position = "Staf",
            department = "Perdagangan",
            phone = "",
            dailyRate = 0.0,
            monthlySalary = 0.0,
            isActive = false
        )
        db.employeeDao().insertEmployee(inactiveEmployee)
        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                id = "ATT-BACKUP-INACTIVE",
                employeeId = inactiveEmployee.id,
                employeeName = inactiveEmployee.name,
                department = inactiveEmployee.department,
                date = "2026-10-05",
                timeIn = "08:00",
                timeOut = "17:00",
                status = "Hadir",
                project = project.name
            )
        )
        val receivable = com.example.data.model.ReceivableEntity(
            id = "PIU-BACKUP-001",
            date = "2026-10-05",
            customerName = "Pelanggan Backup",
            description = "Piutang proyek perdagangan",
            totalAmount = 500000.0,
            dueDate = "2026-11-05",
            targetAccount = "Bank Lama",
            project = project.name,
            fundBucket = "Perdagangan"
        )
        db.receivableDao().insertReceivable(receivable)
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-LEGACY-TRANSFER-INCOME",
                type = "MASUK",
                date = "2026-10-06",
                time = "11:00:00",
                account = "Kas Backup",
                name = "Transfer lama",
                category = "Transfer Masuk",
                description = "Catatan data lama yang harus dipertahankan",
                amount = 25000.0,
                status = "Selesai"
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-INACTIVE-ACCOUNT-HISTORY",
                type = "MASUK",
                date = "2026-10-06",
                time = "10:00:00",
                account = "Bank Lama",
                name = "Histori sebelum akun dinonaktifkan",
                category = "Penjualan",
                description = "",
                amount = 50000.0,
                status = "Selesai"
            )
        )
        db.bankReconDao().insertReconciliation(
            BankReconEntity(
                id = "REC-INACTIVE-ACCOUNT",
                accountName = "Bank Lama",
                period = "2026-10",
                bookBalance = 300000.0,
                statementBalance = 300000.0,
                difference = 0.0,
                status = "Cocok"
            )
        )

        val json = repository.exportDataToJson()

        val restoredDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val restoredRepo = KasRepository(
                database = restoredDb,
                transactionDao = restoredDb.transactionDao(),
                accountDao = restoredDb.accountDao(),
                budgetDao = restoredDb.budgetDao(),
                receivableDao = restoredDb.receivableDao(),
                noteDao = restoredDb.noteDao(),
                employeeDao = restoredDb.employeeDao(),
                attendanceDao = restoredDb.attendanceDao(),
                auditDao = restoredDb.auditDao(),
                bankReconDao = restoredDb.bankReconDao(),
                projectDao = restoredDb.projectDao(),
                projectPlanDao = restoredDb.projectPlanDao(),
                housingUnitDao = restoredDb.housingUnitDao()
            )
            val restored = restoredRepo.restoreDataFromJson(json)
            assertTrue("Backup JSON harus dapat direstore: ${restored.exceptionOrNull()?.message}", restored.isSuccess)
            assertFalse(restoredDb.accountDao().getAccountById("BK-INACTIVE")?.isActive ?: true)
            assertNotNull(restoredDb.employeeDao().getEmployeeById(inactiveEmployee.id))
            assertEquals(1, restoredDb.attendanceDao().getAllAttendances().first().size)
            val restoredReceivable = restoredDb.receivableDao().getReceivableById(receivable.id)
            assertEquals(project.name, restoredReceivable?.project)
            assertEquals("Perdagangan", restoredReceivable?.fundBucket)
            assertEquals("Bank Lama", restoredReceivable?.targetAccount)
            assertEquals(false, restoredDb.projectDao().getProjectById(project.id)?.isActive)
            assertEquals(project.name, restoredDb.projectPlanDao().getPlanById("PLAN-INACTIVE-PROJECT")?.project)
            assertTrue(restoredDb.noteDao().getAllNotes().first().any { it.project == project.name })
            assertNotNull(restoredDb.bankReconDao().getAllReconciliations().first().firstOrNull { it.id == "REC-INACTIVE-ACCOUNT" })
            assertNotNull(restoredDb.transactionDao().getTransactionById("TX-INACTIVE-ACCOUNT-HISTORY"))
            val restoredAudit = restoredRepo.runIntegrityAudit(
                accounts = restoredDb.accountDao().getAllAccounts().first(),
                transactions = restoredDb.transactionDao().getAllActiveTransactions().first(),
                budgets = restoredDb.budgetDao().getAllBudgets().first(),
                receivables = restoredDb.receivableDao().getAllReceivables().first(),
                employees = restoredDb.employeeDao().getAllEmployees().first(),
                attendances = restoredDb.attendanceDao().getAllAttendances().first()
            )
            assertTrue(restoredAudit.issues.any { it.contains("Transfer Masuk tercatat sebagai pemasukan") })
        } finally {
            restoredDb.close()
        }
    }

    @Test
    fun budgetRealizationUsesAllocationOnlyAndDoesNotDoubleCountExpenseCategory() {
        val budgets = listOf(
            BudgetEntity(period = "2026-10", category = "Gaji", budgetAmount = 1000000.0),
            BudgetEntity(period = "2026-10", category = "Operasional", budgetAmount = 1000000.0)
        )
        val transaction = TransactionEntity(
            id = "TX-BUDGET-ALLOCATION",
            type = "KELUAR",
            date = "2026-10-10",
            time = "10:00:00",
            account = "Kas",
            name = "Bahan proyek",
            category = "Gaji",
            description = "",
            amount = 250000.0,
            allocation = "Operasional",
            status = "Selesai"
        )

        val results = repository.calculateBudgetRealizations(budgets, listOf(transaction))
        assertEquals(0.0, results.first { it.budget.category == "Gaji" }.realization, 0.01)
        assertEquals(250000.0, results.first { it.budget.category == "Operasional" }.realization, 0.01)
    }

    @Test
    fun projectTaggedPayrollRemainsOperatingCashOutflowUnlessAllocationIsProject() {
        val account = AccountEntity("cash-project-payroll", "Kas Payroll", "Kas", 1000000.0)
        val payroll = TransactionEntity(
            id = "TX-PROJECT-PAYROLL-FLOW",
            type = "KELUAR",
            date = "2026-10-10",
            time = "10:00:00",
            account = account.name,
            name = "Gaji pekerja proyek",
            category = "Gaji",
            description = "",
            amount = 200000.0,
            allocation = "Gaji",
            project = "Perumahan A",
            status = "Selesai"
        )
        val flow = repository.calculateCashFlowStatement(
            transactions = listOf(payroll),
            accounts = listOf(account),
            startDate = "2026-10-01",
            endDate = "2026-10-31"
        )
        assertEquals(200000.0, flow.operatingOutflow, 0.01)
        assertEquals(0.0, flow.investingOutflow, 0.01)
        assertEquals(800000.0, flow.closingBalance, 0.01)
    }

    @Test
    fun transactionCannotRecordInternalTransferAsRevenue() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-transfer-validation", "Kas Transfer", "Kas", 0.0))
        val invalid = TransactionEntity(
            id = "TX-TRANSFER-AS-INCOME",
            type = "MASUK",
            date = "2026-10-10",
            time = "10:00:00",
            account = "Kas Transfer",
            name = "Transfer dari bank",
            category = "Transfer Masuk",
            description = "",
            amount = 100000.0,
            status = "Selesai"
        )
        try {
            repository.saveTransaction(invalid)
            throw AssertionError("Transfer internal tidak boleh tercatat sebagai pendapatan.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("menu Transfer") == true)
        }
    }

    @Test
    fun legacySeedCleanupRemovesOnlyKnownBootstrapDataAndPreservesReferencedCash() = runBlocking {
        db.accountDao().insertAccounts(listOf(
            AccountEntity("acc_tunai", "Kas Tunai", "Kas", 2500000.0, "#16A34A"),
            AccountEntity("acc_bca", "Bank BCA", "Bank", 12500000.0, "#2563EB")
        ))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-REAL-KEEP",
                type = "MASUK",
                date = "2026-10-10",
                time = "10:00:00",
                account = "Kas Tunai",
                name = "Uang nyata",
                category = "Penjualan",
                description = "",
                amount = 100000.0,
                status = "Selesai"
            )
        )
        db.employeeDao().insertEmployee(
            EmployeeEntity("EMP-001", "Ahmad Fauzi", "Staff Kasir & Keuangan", "Keuangan", "6281234567890", 150000.0, 3800000.0)
        )
        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                id = "ATT-2026-10-09-001",
                employeeId = "EMP-001",
                employeeName = "Ahmad Fauzi",
                department = "Keuangan",
                date = "2026-10-09",
                timeIn = "07:55",
                timeOut = "17:05",
                status = "Hadir",
                overtimeHours = 1.0,
                dailyAllowance = 150000.0,
                notes = "Tepat waktu"
            )
        )
        db.budgetDao().insertBudget(
            BudgetEntity(period = "2026-10", category = "Operasional", budgetAmount = 6000000.0, notes = "Kebutuhan operasional kantor harian")
        )
        db.auditDao().insertAuditLog(
            AuditLogEntity(
                dateFormatted = "2026-10-09 10:00:00",
                action = "INITIALIZE_SYSTEM",
                recordId = "SYS-SETUP",
                details = "Seed",
                user = "Sistem Audit",
                verifiedFormulaStatus = "RECORDED",
                balanceAfter = 45500000.0
            )
        )

        AppDatabase.cleanupLegacySeedRows(db.openHelper.writableDatabase)

        assertEquals(0.0, db.accountDao().getAccountById("acc_tunai")?.initialBalance ?: -1.0, 0.01)
        assertEquals(null, db.accountDao().getAccountById("acc_bca"))
        assertEquals(0, db.attendanceDao().getAllAttendances().first().size)
        assertEquals(null, db.employeeDao().getEmployeeById("EMP-001"))
        assertTrue(db.budgetDao().getAllBudgets().first().isEmpty())
        assertFalse(db.auditDao().getRecentAuditLogs().first().any { it.recordId == "SYS-SETUP" })
        assertNotNull(db.transactionDao().getTransactionById("TX-REAL-KEEP"))
    }

    @Test
    fun receivablePaymentsStayOnTheirProjectAndPaidReceivablesCannotBeDeleted() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-project-rec", "Kas Proyek", "Kas", 0.0))
        val project = ProjectEntity(
            id = "PRJ-PIU-001",
            name = "Perdagangan Piutang",
            category = "Perdagangan",
            businessModel = "Tidak berlaku",
            location = "",
            startDate = "2026-10-01",
            targetEndDate = "2026-12-31",
            budgetAmount = 1000000.0,
            status = "Berjalan",
            notes = "",
            isActive = true,
            createdAt = 1790812800000L
        )
        repository.saveProject(project)
        val receivable = com.example.data.model.ReceivableEntity(
            id = "PIU-PROJECT-001",
            date = "2026-10-01",
            customerName = "Pelanggan",
            description = "Pembayaran perdagangan",
            totalAmount = 300000.0,
            dueDate = "2026-11-01",
            targetAccount = "Kas Proyek",
            project = project.name,
            fundBucket = "Perdagangan"
        )
        repository.saveReceivable(receivable)
        repository.payReceivable(receivable.id, 100000.0, "Kas Proyek")
        val payment = db.transactionDao().getAllActiveTransactions().first().single()
        assertEquals(project.name, payment.project)
        assertEquals("Perdagangan", payment.fundBucket)

        try {
            repository.deleteReceivable(receivable.id)
            throw AssertionError("Tagihan dengan pembayaran tercatat tidak boleh dihapus.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("sudah memiliki pembayaran") == true)
        }
        assertNotNull(db.receivableDao().getReceivableById(receivable.id))
    }

    @Test
    fun attendanceCannotBeAddedAfterItsPayrollPeriodWasDisbursed() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-att-payroll-closed", "Kas Payroll", "Kas", 1000000.0))
        val employee = EmployeeEntity(
            id = "EMP-ATT-PAYROLL-CLOSED",
            name = "Karyawan Periode Terkunci",
            position = "Staf",
            department = "Operasional",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 0.0
        )
        repository.saveEmployee(employee)
        repository.savePayrollDisbursement(
            period = "2026-10",
            totalAmount = 100000.0,
            accountName = "Kas Payroll",
            allocationsByProject = mapOf("" to 100000.0)
        )
        val attendance = AttendanceEntity(
            id = "ATT-ATT-PAYROLL-CLOSED",
            employeeId = employee.id,
            employeeName = employee.name,
            department = employee.department,
            date = "2026-10-12",
            status = "Hadir",
            dailyAllowance = employee.dailyRate
        )
        try {
            repository.saveAttendance(attendance)
            throw AssertionError("Absensi tidak boleh ditambahkan setelah periode payroll dibayar.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("tidak dapat ditambahkan karena payroll") == true)
        }
        assertTrue(db.attendanceDao().getAllAttendances().first().isEmpty())
        try {
            repository.bulkMarkAllEmployeesHadir("2026-10-12")
            throw AssertionError("Absensi cepat tidak boleh melewati periode payroll yang sudah dibayar.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("tidak dapat ditambahkan karena payroll") == true)
        }
        assertTrue(db.attendanceDao().getAllAttendances().first().isEmpty())
    }

    @Test
    fun payrollDisbursementRejectsOverlappingPeriodsAndPreventsDoublePayment() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-payroll-overlap", "Kas Payroll Overlap", "Kas", 5000000.0))
        repository.savePayrollDisbursement(
            period = "2026-10-05",
            totalAmount = 100000.0,
            accountName = "Kas Payroll Overlap",
            allocationsByProject = mapOf("" to 100000.0)
        )
        try {
            repository.savePayrollDisbursement(
                period = "2026-10",
                totalAmount = 500000.0,
                accountName = "Kas Payroll Overlap",
                allocationsByProject = mapOf("" to 500000.0)
            )
            throw AssertionError("Pembayaran harian dan bulanan yang bertumpang tindih harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("bertumpang tindih") == true)
        }
        val payrollTransactions = db.transactionDao().getAllActiveTransactions().first()
            .filter { it.category == "Gaji" && it.receiptNo.startsWith("PAYROLL-") }
        assertEquals(1, payrollTransactions.size)
        assertEquals(100000.0, payrollTransactions.sumOf { it.amount }, 0.01)
    }

    @Test
    fun duplicateBudgetScopeUpdatesTheExistingLineInsteadOfDoubleCounting() = runBlocking {
        repository.saveBudget(BudgetEntity(
            period = "2026-10",
            category = "Gaji",
            budgetAmount = 1000000.0,
            notes = "Target awal",
            project = "",
            fundBucket = "PT"
        ))
        repository.saveBudget(BudgetEntity(
            period = "2026-10",
            category = "gaji",
            budgetAmount = 750000.0,
            notes = "Target terbaru",
            project = "",
            fundBucket = "pt"
        ))
        val budgets = db.budgetDao().getAllBudgets().first()
        assertEquals(1, budgets.size)
        assertEquals(750000.0, budgets.single().budgetAmount, 0.01)
        assertEquals("Target terbaru", budgets.single().notes)
    }

    @Test
    fun attendanceCannotBeDeletedAfterItsMonthlyPayrollWasDisbursed() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-payroll-lock", "Kas Gaji", "Kas", 1000000.0))
        val employee = EmployeeEntity(
            id = "EMP-PAYROLL-LOCK",
            name = "Karyawan Payroll",
            position = "Staf",
            department = "Umum",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 3000000.0
        )
        repository.saveEmployee(employee)
        val attendance = AttendanceEntity(
            id = "ATT-PAYROLL-LOCK",
            employeeId = employee.id,
            employeeName = employee.name,
            department = employee.department,
            date = "2026-11-05",
            status = "Hadir",
            dailyAllowance = 100000.0
        )
        repository.saveAttendance(attendance)
        repository.savePayrollDisbursement(
            period = "2026-10-01-2026-11-15",
            totalAmount = 3100000.0,
            accountName = "Kas Gaji",
            allocationsByProject = mapOf("" to 3100000.0)
        )
        try {
            repository.deleteAttendance(attendance.id)
            throw AssertionError("Absensi periode payroll tertutup tidak boleh dihapus.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("termasuk periode payroll 2026-10-01-2026-11-15") == true)
        }
        assertNotNull(db.attendanceDao().getAttendanceById(attendance.id))
    }

    @Test
    fun payrollAllocationSeparatesMonthlyBaseAndAttendanceProjectCosts() {
        val employee = EmployeeEntity(
            id = "EMP-ALLOC-001",
            name = "Karyawan Proyek",
            position = "Operator",
            department = "Lapangan",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 3000000.0,
            defaultProject = "PT/Umum"
        )
        val attendances = listOf(
            AttendanceEntity(
                id = "ATT-ALLOC-001",
                employeeId = employee.id,
                employeeName = employee.name,
                department = employee.department,
                date = "2026-11-01",
                status = "Hadir",
                overtimeHours = 2.0,
                dailyAllowance = 100000.0,
                project = "Cut & Fill A"
            ),
            AttendanceEntity(
                id = "ATT-ALLOC-002",
                employeeId = employee.id,
                employeeName = employee.name,
                department = employee.department,
                date = "2026-11-02",
                status = "Alpa",
                project = "Cut & Fill A"
            )
        )

        val allocations = repository.calculatePayrollAllocations(listOf(employee), attendances)
        assertEquals(2900000.0, allocations["PT/Umum"] ?: 0.0, 0.01)
        assertEquals(137500.0, allocations["Cut & Fill A"] ?: 0.0, 0.01)
        assertEquals(repository.calculatePayrollTotal(listOf(employee), attendances), allocations.values.sum(), 0.01)
        assertTrue(allocations.values.all { it >= 0.0 })
    }


    @Test
    fun exportsAndIntegrityAuditReadCurrentRoomDataWithoutUiCollectors() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-live-export", "Kas Live", "Kas", 500000.0))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-LIVE-EXPORT",
                type = "MASUK",
                date = "2026-10-10",
                time = "10:00:00",
                account = "Kas Live",
                name = "Pemasukan hidup",
                category = "Penjualan",
                description = "",
                amount = 125000.0,
                status = "Selesai"
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "TX-LIVE-AUDIT-INVALID",
                type = "KELUAR",
                date = "2026-10-10",
                time = "10:01:00",
                account = "Akun Tidak Ada",
                name = "Harus terdeteksi audit",
                category = "Operasional",
                description = "",
                amount = 25000.0,
                status = "Selesai"
            )
        )

        val json = repository.exportDataToJson()
        val csv = repository.exportCurrentTransactionsToCsv()
        val audit = repository.runCurrentIntegrityAudit()

        assertTrue("Backup mesti membaca akun dari DB saat ini.", json.contains("Kas Live"))
        assertTrue("Backup mesti membaca transaksi dari DB saat ini.", json.contains("TX-LIVE-EXPORT"))
        assertTrue("Export CSV mesti membaca transaksi dari DB saat ini.", csv.contains("TX-LIVE-EXPORT"))
        assertEquals(2, audit.transactionCount)
        assertFalse("Audit langsung ke DB harus menemukan akun yang tidak dikenal.", audit.result.passed)
        assertTrue(audit.result.issues.any { it.contains("Akun Tidak Ada") })
    }

    @Test
    fun linkedReceivablePaymentsCannotBeEditedAndCsvImportKeepsMasterBalanceSynchronized() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-csv-piu", "Kas CSV", "Kas", 1000000.0))
        val receivable = com.example.data.model.ReceivableEntity(
            id = "PIU-CSV-001",
            date = "2026-10-01",
            customerName = "Pelanggan CSV",
            description = "Penjualan",
            totalAmount = 300000.0,
            dueDate = "2026-12-01",
            targetAccount = "Kas CSV",
            project = "",
            fundBucket = "PT"
        )
        repository.saveReceivable(receivable)

        val header = listOf(
            "ID", "Tipe", "Tanggal", "Jam", "Akun", "Ke Akun", "Nama Transaksi", "Kategori",
            "Keterangan", "Nominal", "Alokasi", "PIC", "Bukti", "No Bukti", "Proyek",
            "Catatan", "Status", "Waktu Input", "Input Oleh", "Diarsipkan", "Waktu Arsip",
            "Diarsipkan Oleh", "Kelompok Dana"
        ).joinToString(",")
        fun paymentRow(id: String, amount: String) = listOf(
            id, "MASUK", "2026-10-10", "10:00:00", "Kas CSV", "", "Pelunasan Piutang",
            "Piutang Masuk", "Pembayaran spreadsheet", amount, "Operasional", "Bendahara", "",
            "PIU-PIU-CSV-001", "", "", "Selesai", "", "Spreadsheet Import", "TIDAK", "", "", "PT"
        ).joinToString(",")
        val imported = repository.importTransactionsFromCsv("$header\n${paymentRow("TX-PIU-CSV-OK", "100000")}\n")
        assertTrue("Import pembayaran terhubung harus sukses: ${imported.exceptionOrNull()?.message}", imported.isSuccess)
        assertEquals(100000.0, db.receivableDao().getReceivableById(receivable.id)?.paidAmount ?: -1.0, 0.01)

        val overpay = repository.importTransactionsFromCsv("$header\n${paymentRow("TX-PIU-CSV-OVER", "250000")}\n")
        assertTrue("Import yang melebihi sisa tagihan harus ditolak.", overpay.isFailure)
        assertEquals(100000.0, db.receivableDao().getReceivableById(receivable.id)?.paidAmount ?: -1.0, 0.01)
        assertEquals(1, db.transactionDao().getAllActiveTransactions().first().size)

        val transaction = db.transactionDao().getAllActiveTransactions().first().single()
        try {
            repository.updateTransaction(transaction.copy(amount = 50000.0), transaction)
            throw AssertionError("Transaksi pelunasan piutang tidak boleh diedit terpisah dari master tagihan.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("terikat") == true)
        }
        assertEquals(100000.0, db.transactionDao().getTransactionById(transaction.id)?.amount ?: -1.0, 0.01)
        assertEquals(100000.0, db.receivableDao().getReceivableById(receivable.id)?.paidAmount ?: -1.0, 0.01)
    }

    @Test
    fun payrollLedgerRowsCannotBeEditedToBypassPeriodDoublePaymentProtection() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-payroll-immutable", "Kas Payroll", "Kas", 500000.0))
        repository.savePayrollDisbursement(
            period = "2026-10",
            totalAmount = 100000.0,
            accountName = "Kas Payroll",
            allocationsByProject = mapOf("" to 100000.0)
        )
        val payroll = db.transactionDao().getAllActiveTransactions().first().single()
        try {
            repository.updateTransaction(payroll.copy(status = "Batal", receiptNo = "DIUBAH"), payroll)
            throw AssertionError("Transaksi payroll terkait tidak boleh diedit.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("terikat") == true)
        }
        assertEquals("PAYROLL-2026-10", db.transactionDao().getTransactionById(payroll.id)?.receiptNo)
        assertEquals("Selesai", db.transactionDao().getTransactionById(payroll.id)?.status)
    }


    @Test
    fun archivedTransactionCanBeRestoredAfterItsAccountIsDeactivated() = runBlocking {
        val account = AccountEntity("acc-archive-inactive", "Bank Histori", "Bank", 200000.0)
        db.accountDao().insertAccount(account)
        val transaction = TransactionEntity(
            id = "TX-ARCHIVE-INACTIVE",
            type = "MASUK",
            date = "2026-10-05",
            time = "10:00:00",
            account = account.name,
            name = "Penerimaan historis",
            category = "Penjualan",
            description = "Dibuat saat akun aktif",
            amount = 50000.0,
            status = "Selesai"
        )
        repository.saveTransaction(transaction)
        repository.archiveTransaction(transaction.id)
        db.accountDao().updateAccount(account.copy(isActive = false))

        repository.restoreTransaction(transaction.id)

        val restored = db.transactionDao().getTransactionById(transaction.id)
        assertNotNull(restored)
        assertFalse(restored!!.isArchived)
        assertEquals(account.name, restored.account)
        val balance = repository.calculateAccountBalances(
            db.accountDao().getAllAccounts().first(),
            db.transactionDao().getAllActiveTransactions().first() +
                db.transactionDao().getArchivedTransactions().first()
        ).single()
        assertEquals(250000.0, balance.currentBalance, 0.01)
    }

    @Test
    fun newReceivableCannotStartAsAlreadyPaidWithoutLinkedLedgerPayment() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-new-piu-validation", "Kas Tagihan", "Kas", 100000.0))
        val paidReceivable = com.example.data.model.ReceivableEntity(
            id = "PIU-PAID-ON-CREATE",
            date = "2026-10-01",
            customerName = "Pelanggan Baru",
            description = "Master baru tanpa transaksi",
            totalAmount = 200000.0,
            paidAmount = 50000.0,
            dueDate = "2026-12-01",
            targetAccount = "Kas Tagihan"
        )
        try {
            repository.saveReceivable(paidReceivable)
            throw AssertionError("Master tagihan baru tidak boleh menyatakan pembayaran yang belum masuk ledger.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("pembayaran Rp 0") == true)
        }
        val markedPaid = paidReceivable.copy(id = "PIU-LUNAS-ON-CREATE", paidAmount = 0.0, status = "Lunas")
        try {
            repository.saveReceivable(markedPaid)
            throw AssertionError("Master tagihan baru tidak boleh langsung Lunas tanpa transaksi pembayaran.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("status Lunas") == true)
        }
        assertTrue(db.receivableDao().getAllReceivables().first().isEmpty())
    }

    @Test
    fun inactiveEmployeesCannotReceiveNewAttendanceButRemainEligibleForHistoricalPayroll() = runBlocking {
        val active = EmployeeEntity(
            id = "EMP-PAYROLL-ACTIVE",
            name = "Karyawan Aktif",
            position = "Staf",
            department = "PT",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 1000000.0,
            isActive = true
        )
        val inactiveWithAttendance = EmployeeEntity(
            id = "EMP-PAYROLL-INACTIVE-WORKED",
            name = "Karyawan Nonaktif Pernah Bekerja",
            position = "Staf",
            department = "PT",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 2000000.0,
            isActive = false
        )
        val inactiveWithoutAttendance = EmployeeEntity(
            id = "EMP-PAYROLL-INACTIVE-NO-ATT",
            name = "Karyawan Nonaktif Tanpa Absensi",
            position = "Staf",
            department = "PT",
            phone = "",
            dailyRate = 100000.0,
            monthlySalary = 3000000.0,
            isActive = false
        )
        db.employeeDao().insertEmployees(listOf(active, inactiveWithAttendance, inactiveWithoutAttendance))
        val attendance = AttendanceEntity(
            id = "ATT-EMP-PAYROLL-INACTIVE-WORKED",
            employeeId = inactiveWithAttendance.id,
            employeeName = inactiveWithAttendance.name,
            department = inactiveWithAttendance.department,
            date = "2026-10-05",
            status = "Hadir",
            dailyAllowance = 100000.0
        )
        db.attendanceDao().insertAttendance(attendance)

        val eligible = repository.selectPayrollEligibleEmployees(
            db.employeeDao().getAllEmployees().first(),
            listOf(attendance)
        )
        assertEquals(setOf(active.id, inactiveWithAttendance.id), eligible.map { it.id }.toSet())
        val allocations = repository.calculatePayrollAllocations(eligible, listOf(attendance))
        assertEquals(3100000.0, allocations.values.sum(), 0.01)

        try {
            repository.saveAttendance(
                AttendanceEntity(
                    id = "ATT-INACTIVE-NEW",
                    employeeId = inactiveWithAttendance.id,
                    employeeName = inactiveWithAttendance.name,
                    department = inactiveWithAttendance.department,
                    date = "2026-10-06",
                    status = "Hadir",
                    dailyAllowance = 100000.0
                )
            )
            throw AssertionError("Absensi baru untuk karyawan nonaktif harus ditolak.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("karyawan nonaktif") == true)
        }
        assertNull(db.attendanceDao().getAttendanceById("ATT-INACTIVE-NEW"))
    }

    @Test
    fun csvImportCanPreserveHistoricalTransactionsOnInactiveAccounts() = runBlocking {
        db.accountDao().insertAccount(AccountEntity("acc-csv-inactive", "Bank Bekas", "Bank", 100000.0, isActive = false))
        val header = listOf(
            "ID", "Tipe", "Tanggal", "Jam", "Akun", "Ke Akun", "Nama Transaksi", "Kategori",
            "Keterangan", "Nominal", "Alokasi", "PIC", "Bukti", "No Bukti", "Proyek",
            "Catatan", "Status", "Waktu Input", "Input Oleh", "Diarsipkan", "Waktu Arsip",
            "Diarsipkan Oleh", "Kelompok Dana"
        ).joinToString(",")
        val row = listOf(
            "TX-CSV-INACTIVE-HISTORY", "MASUK", "2026-10-04", "09:00:00", "Bank Bekas", "",
            "Penerimaan lama", "Penjualan", "Data historis", "75000", "Operasional", "Bendahara",
            "", "", "", "", "Selesai", "", "Spreadsheet Import", "TIDAK", "", "", "PT"
        ).joinToString(",")
        val imported = repository.importTransactionsFromCsv("$header\n$row\n")
        assertTrue("CSV historis pada akun nonaktif harus dapat diimpor: ${imported.exceptionOrNull()?.message}", imported.isSuccess)
        assertEquals(
            75000.0,
            db.transactionDao().getAllActiveTransactions().first().single().amount,
            0.01
        )
    }

}
