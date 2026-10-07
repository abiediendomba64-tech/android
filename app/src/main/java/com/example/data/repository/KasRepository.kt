package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AttendanceDao
import com.example.data.local.AuditDao
import com.example.data.local.BankReconDao
import com.example.data.local.BudgetDao
import com.example.data.local.EmployeeDao
import com.example.data.local.NoteDao
import com.example.data.local.ReceivableDao
import com.example.data.local.TransactionDao
import com.example.data.model.AccountEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BankReconEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CashNoteEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.ReceivableEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AccountWithBalance(
    val account: AccountEntity,
    val totalMasuk: Double,
    val totalKeluar: Double,
    val currentBalance: Double
)

data class DashboardKpis(
    val totalBalance: Double,
    val masukToday: Double,
    val keluarToday: Double,
    val netToday: Double,
    val masukMonth: Double,
    val keluarMonth: Double,
    val netMonth: Double
)

data class BudgetRealization(
    val budget: BudgetEntity,
    val realization: Double,
    val remaining: Double,
    val percentUsed: Double,
    val isOver: Boolean
)

data class CashFlowStatement(
    val period: String,
    val operatingInflow: Double,
    val operatingOutflow: Double,
    val netOperatingFlow: Double,
    val investingOutflow: Double,
    val netInvestingFlow: Double,
    val financingInflow: Double,
    val netFinancingFlow: Double,
    val netCashChange: Double,
    val openingBalance: Double,
    val closingBalance: Double
)

data class AttendanceSummary(
    val totalEmployees: Int,
    val totalHadir: Int,
    val totalIzin: Int,
    val totalSakit: Int,
    val totalAlpa: Int,
    val totalCuti: Int,
    val totalOvertimeHours: Double,
    val totalAllowance: Double
)

data class IntegrityAuditResult(
    val passed: Boolean,
    val issues: List<String>,
    val checkedAt: String
)

class KasRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val receivableDao: ReceivableDao,
    private val noteDao: NoteDao,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val auditDao: AuditDao,
    private val bankReconDao: BankReconDao
) {
    val activeTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllActiveTransactions()
    val archivedTransactions: Flow<List<TransactionEntity>> = transactionDao.getArchivedTransactions()
    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val budgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val receivables: Flow<List<ReceivableEntity>> = receivableDao.getAllReceivables()
    val notes: Flow<List<CashNoteEntity>> = noteDao.getAllNotes()
    val employees: Flow<List<EmployeeEntity>> = employeeDao.getAllActiveEmployees()
    val attendances: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendances()
    val auditLogs: Flow<List<AuditLogEntity>> = auditDao.getRecentAuditLogs()
    val bankReconciliations: Flow<List<BankReconEntity>> = bankReconDao.getAllReconciliations()

    suspend fun saveTransaction(transaction: TransactionEntity) {
        validateTransaction(transaction)
        transactionDao.insertTransaction(transaction)

        // Audit trail records the write; it does not claim external formula verification.
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = if (transaction.type == "MASUK") "INSERT_MASUK" else if (transaction.type == "KELUAR") "INSERT_KELUAR" else "TRANSFER",
                recordId = transaction.id,
                details = "${transaction.name} (${transaction.account}) sebesar Rp ${transaction.amount.toLong()} - Kategori: ${transaction.category}",
                user = transaction.inputBy,
                verifiedFormulaStatus = "RECORDED",
                balanceAfter = 0.0
            )
        )
    }

    private suspend fun validateTransaction(transaction: TransactionEntity) {
        require(transaction.amount.isFinite() && transaction.amount > 0.0) { "Nominal transaksi harus lebih besar dari Rp 0." }
        require(transaction.type in setOf("MASUK", "KELUAR", "TRANSFER")) { "Tipe transaksi tidak valid." }
        require(transaction.status in setOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")) { "Status transaksi tidak valid." }
        require(accountDao.getAccountByName(transaction.account)?.isActive == true) { "Akun ${transaction.account} tidak terdaftar atau nonaktif." }
        if (transaction.type == "TRANSFER") {
            val destination = transaction.toAccount?.takeIf { it.isNotBlank() }
            require(destination != null) { "Akun tujuan transfer wajib diisi." }
            require(!destination.equals(transaction.account, ignoreCase = true)) { "Akun asal dan tujuan transfer tidak boleh sama." }
            require(accountDao.getAccountByName(destination) != null) { "Akun tujuan $destination tidak terdaftar." }
        } else {
            require(transaction.toAccount == null) { "Akun tujuan hanya boleh diisi untuk transfer." }
        }
    }
    suspend fun archiveTransaction(id: String, archivedBy: String = "Admin") {
        transactionDao.archiveTransaction(id, System.currentTimeMillis(), archivedBy)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "ARCHIVE_TRANSACTION",
                recordId = id,
                details = "Transaksi dipindahkan ke Transaksi_Dihapus (Arsip)",
                user = archivedBy,
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun restoreTransaction(id: String) {
        transactionDao.restoreTransaction(id)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "RESTORE_TRANSACTION",
                recordId = id,
                details = "Transaksi dipulihkan kembali ke buku ledger aktif",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun permanentlyDeleteTransaction(id: String) {
        transactionDao.permanentlyDelete(id)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "PERMANENT_DELETE",
                recordId = id,
                details = "Transaksi dihapus permanen dari basis data",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun emptyTrash() {
        transactionDao.emptyTrash()
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "EMPTY_TRASH",
                recordId = "TRASH-ALL",
                details = "Seluruh arsip transaksi dibersihkan permanen",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun saveAccount(account: AccountEntity) {
        require(account.name.isNotBlank()) { "Nama akun wajib diisi." }
        require(account.type in setOf("Kas", "Bank", "E-Wallet", "Lainnya")) { "Jenis akun tidak valid." }
        require(account.initialBalance.isFinite() && account.initialBalance >= 0.0) { "Saldo awal akun tidak valid." }
        val existing = accountDao.getAccountByName(account.name.trim())
        require(existing == null || existing.id == account.id) { "Nama akun sudah digunakan." }
        accountDao.insertAccount(account.copy(name = account.name.trim()))
    }

    suspend fun bulkMarkAllEmployeesHadir(date: String) {
        val allEmployees = employeeDao.getAllEmployeesList()
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        allEmployees.forEach { emp ->
            val existing = attendanceDao.getAttendanceByEmployeeAndDate(emp.id, date)
            if (existing == null) {
                val attId = "ATT-${date.replace("-", "")}-${emp.id}"
                attendanceDao.insertAttendance(
                    AttendanceEntity(
                        id = attId,
                        employeeId = emp.id,
                        employeeName = emp.name,
                        department = emp.department,
                        date = date,
                        timeIn = "08:00",
                        timeOut = "17:00",
                        status = "Hadir",
                        overtimeHours = 0.0,
                        dailyAllowance = emp.dailyRate,
                        notes = "Absensi tepat waktu"
                    )
                )
            }
        }
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "BULK_ATTENDANCE",
                recordId = "ATT-BULK-$date",
                details = "Pencatatan absensi cepat untuk seluruh karyawan hadir pada tanggal $date",
                user = "Admin Absensi",
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun deleteAccount(id: String) {
        accountDao.deleteAccount(id)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        require(budget.period.equals("All", ignoreCase = true) || budget.period.matches(Regex("""\\d{4}-\\d{2}"""))) { "Periode anggaran harus YYYY-MM atau All." }
        require(budget.category.isNotBlank()) { "Kategori anggaran wajib diisi." }
        require(budget.budgetAmount.isFinite() && budget.budgetAmount >= 0.0) { "Nominal anggaran tidak valid." }
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    suspend fun saveReceivable(receivable: ReceivableEntity) {
        require(receivable.type in setOf("PIUTANG", "HUTANG")) { "Jenis tagihan tidak valid." }
        require(receivable.customerName.isNotBlank()) { "Nama pihak wajib diisi." }
        require(receivable.totalAmount.isFinite() && receivable.totalAmount > 0.0) { "Nominal tagihan harus lebih besar dari Rp 0." }
        require(receivable.paidAmount.isFinite() && receivable.paidAmount >= 0.0 && receivable.paidAmount <= receivable.totalAmount) { "Nominal pembayaran tagihan tidak valid." }
        require(receivable.dueDate.matches(Regex("""\\d{4}-\\d{2}-\\d{2}"""))) { "Tanggal jatuh tempo harus YYYY-MM-DD." }
        require(accountDao.getAccountByName(receivable.targetAccount)?.isActive == true) { "Akun terkait tidak terdaftar atau nonaktif." }
        receivableDao.insertReceivable(receivable)
    }

    suspend fun payReceivable(id: String, paymentAmount: Double, targetAccount: String, pic: String = "Bendahara") {
        val current = receivableDao.getReceivableById(id)
            ?: throw IllegalArgumentException("Piutang $id tidak ditemukan.")
        require(paymentAmount.isFinite() && paymentAmount > 0.0) { "Nominal pembayaran harus lebih besar dari Rp 0." }
        require(current.type == "PIUTANG") { "Record ${current.id} bukan piutang." }
        require(paymentAmount <= current.remainingAmount) { "Pembayaran melebihi sisa piutang ${current.id}." }
        require(accountDao.getAccountByName(targetAccount)?.isActive == true) { "Akun penerimaan $targetAccount tidak terdaftar atau nonaktif." }
        val updatedPaid = current.paidAmount + paymentAmount
        val newStatus = if (updatedPaid >= current.totalAmount) "Lunas" else current.status
        receivableDao.updateReceivable(
            current.copy(
                paidAmount = updatedPaid,
                status = newStatus
            )
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val kmTransaction = TransactionEntity(
            id = generateId("KM"),
            type = "MASUK",
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            account = targetAccount,
            name = "Pelunasan Piutang: ${current.customerName}",
            category = "Piutang Masuk",
            description = "Pembayaran piutang (${current.customerName} - ${current.description})",
            amount = paymentAmount,
            allocation = "Operasional",
            pic = pic,
            receiptNo = "PIU-${current.id}",
            status = "Selesai"
        )
        saveTransaction(kmTransaction)
    }

    suspend fun payHutang(id: String, paymentAmount: Double, sourceAccount: String, pic: String = "Bendahara") {
        val current = receivableDao.getReceivableById(id)
            ?: throw IllegalArgumentException("Hutang $id tidak ditemukan.")
        require(paymentAmount.isFinite() && paymentAmount > 0.0) { "Nominal pembayaran harus lebih besar dari Rp 0." }
        require(current.type == "HUTANG") { "Record ${current.id} bukan hutang." }
        require(paymentAmount <= current.remainingAmount) { "Pembayaran melebihi sisa hutang ${current.id}." }
        require(accountDao.getAccountByName(sourceAccount)?.isActive == true) { "Akun pembayaran $sourceAccount tidak terdaftar atau nonaktif." }
        val updatedPaid = current.paidAmount + paymentAmount
        val newStatus = if (updatedPaid >= current.totalAmount) "Lunas" else current.status
        receivableDao.updateReceivable(
            current.copy(
                paidAmount = updatedPaid,
                status = newStatus
            )
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()
        val kkTransaction = TransactionEntity(
            id = generateId("KK"),
            type = "KELUAR",
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            account = sourceAccount,
            name = "Pembayaran Hutang: ${current.customerName}",
            category = "Belanja Barang",
            description = "Pelunasan hutang pembelian (${current.customerName} - ${current.description})",
            amount = paymentAmount,
            allocation = "Operasional",
            pic = pic,
            receiptNo = "HUT-${current.id}",
            status = "Selesai"
        )
        saveTransaction(kkTransaction)
    }

    suspend fun updateTransaction(newTx: TransactionEntity, oldTx: TransactionEntity) {
        transactionDao.updateTransaction(newTx)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "UPDATE_TRANSACTION",
                recordId = newTx.id,
                details = "Perubahan transaksi ${newTx.id}: [Lama: ${oldTx.name}, Rp ${oldTx.amount.toLong()}, ${oldTx.account}] -> [Baru: ${newTx.name}, Rp ${newTx.amount.toLong()}, ${newTx.account}]",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED",
                balanceAfter = 0.0
            )
        )
    }

    suspend fun deleteReceivable(id: String) {
        receivableDao.deleteReceivable(id)
    }

    suspend fun saveNote(note: CashNoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
    }

    // Employee & Attendance Operations
    suspend fun saveEmployee(employee: EmployeeEntity) {
        require(employee.name.isNotBlank()) { "Nama karyawan wajib diisi." }
        require(employee.dailyRate.isFinite() && employee.dailyRate >= 0.0) { "Uang harian tidak valid." }
        require(employee.monthlySalary.isFinite() && employee.monthlySalary >= 0.0) { "Gaji bulanan tidak valid." }
        require(employeeDao.getEmployeeById(employee.id) == null) { "ID karyawan sudah digunakan." }
        employeeDao.insertEmployee(employee)
    }

    suspend fun deleteEmployee(id: String) {
        employeeDao.deleteEmployee(id)
    }

    fun getAttendancesByDate(date: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendancesByDate(date)

    fun getAttendancesByMonth(monthPrefix: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendancesByMonth(monthPrefix)

    fun getAttendancesByYear(yearPrefix: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendancesByYear(yearPrefix)

    fun getAttendancesByPeriod(start: String, end: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendancesByPeriod(start, end)

    suspend fun saveAttendance(attendance: AttendanceEntity) {
        require(attendance.employeeId.isNotBlank()) { "ID karyawan wajib diisi." }
        require(employeeDao.getEmployeeById(attendance.employeeId) != null) { "Karyawan tidak ditemukan." }
        require(attendance.date.matches(Regex("""\\d{4}-\\d{2}-\\d{2}"""))) { "Tanggal absensi harus YYYY-MM-DD." }
        require(attendance.status in setOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti")) { "Status absensi tidak valid." }
        require(attendance.overtimeHours.isFinite() && attendance.overtimeHours >= 0.0) { "Jam lembur tidak valid." }
        require(attendance.dailyAllowance.isFinite() && attendance.dailyAllowance >= 0.0) { "Uang harian tidak valid." }
        attendanceDao.insertAttendance(attendance)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "ATTENDANCE_LOG",
                recordId = attendance.id,
                details = "Absensi ${attendance.employeeName} (${attendance.status}) pada ${attendance.date}",
                user = "Admin Absensi",
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun deleteAttendance(id: String) {
        attendanceDao.deleteAttendance(id)
    }

    // Bank Reconciliation Operations
    suspend fun saveBankReconciliation(recon: BankReconEntity) {
        require(accountDao.getAccountByName(recon.accountName)?.isActive == true) { "Akun rekonsiliasi tidak terdaftar atau nonaktif." }
        require(recon.statementBalance.isFinite() && recon.statementBalance >= 0.0) { "Saldo rekening koran tidak valid." }
        require(recon.period.matches(Regex("""\\d{4}-\\d{2}"""))) { "Periode rekonsiliasi harus YYYY-MM." }
        val computedDifference = recon.statementBalance - recon.bookBalance
        val normalized = recon.copy(
            difference = computedDifference,
            status = if (kotlin.math.abs(computedDifference) < 1.0) "Cocok" else "Selisih"
        )
        bankReconDao.insertReconciliation(normalized)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "BANK_RECONCILE",
                recordId = recon.id,
                details = "Rekonsiliasi ${recon.accountName} periode ${recon.period}: Buku Rp ${recon.bookBalance.toLong()} vs Bank Rp ${recon.statementBalance.toLong()} (Selisih: Rp ${recon.difference.toLong()} - ${recon.status})",
                user = recon.reconciledBy,
                verifiedFormulaStatus = "RECORDED"
            )
        )
    }

    suspend fun logDirectAudit(audit: AuditLogEntity) {
        auditDao.insertAuditLog(audit)
    }

    fun generateId(prefix: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val uuid = UUID.randomUUID().toString().take(6).uppercase()
        return "$prefix-$stamp-$uuid"
    }

    fun calculateAccountBalances(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): List<AccountWithBalance> {
        return accounts.map { account ->
            val settledTx = transactions.filter { it.status == "Selesai" }

            val totalMasuk = settledTx.filter { it.type == "MASUK" && it.account.equals(account.name, ignoreCase = true) }
                .sumOf { it.amount }

            val totalTransferMasuk = settledTx.filter { it.type == "TRANSFER" && it.toAccount?.equals(account.name, ignoreCase = true) == true }
                .sumOf { it.amount }

            val totalKeluar = settledTx.filter { it.type == "KELUAR" && it.account.equals(account.name, ignoreCase = true) }
                .sumOf { it.amount }

            val totalTransferKeluar = settledTx.filter { it.type == "TRANSFER" && it.account.equals(account.name, ignoreCase = true) }
                .sumOf { it.amount }

            val currentBalance = account.initialBalance + totalMasuk + totalTransferMasuk - totalKeluar - totalTransferKeluar

            AccountWithBalance(
                account = account,
                totalMasuk = totalMasuk + totalTransferMasuk,
                totalKeluar = totalKeluar + totalTransferKeluar,
                currentBalance = currentBalance
            )
        }
    }

    fun calculateDashboardKpis(
        accountsWithBalance: List<AccountWithBalance>,
        transactions: List<TransactionEntity>
    ): DashboardKpis {
        val totalBalance = accountsWithBalance.sumOf { it.currentBalance }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val settledTx = transactions.filter { it.status == "Selesai" }

        val masukToday = settledTx.filter { it.type == "MASUK" && it.date == today }
            .sumOf { it.amount }

        val keluarToday = settledTx.filter { it.type == "KELUAR" && it.date == today }
            .sumOf { it.amount }

        val masukMonth = settledTx.filter { it.type == "MASUK" && it.date.startsWith(currentMonth) }
            .sumOf { it.amount }

        val keluarMonth = settledTx.filter { it.type == "KELUAR" && it.date.startsWith(currentMonth) }
            .sumOf { it.amount }

        return DashboardKpis(
            totalBalance = totalBalance,
            masukToday = masukToday,
            keluarToday = keluarToday,
            netToday = masukToday - keluarToday,
            masukMonth = masukMonth,
            keluarMonth = keluarMonth,
            netMonth = masukMonth - keluarMonth
        )
    }

    fun calculateBudgetRealizations(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>
    ): List<BudgetRealization> {
        val settledExpenses = transactions.filter { it.type == "KELUAR" && it.status == "Selesai" }

        return budgets.map { budget ->
            val realization = settledExpenses.filter {
                (it.date.startsWith(budget.period) || budget.period.equals("All", ignoreCase = true)) &&
                        (it.category.equals(budget.category, ignoreCase = true) || it.allocation.equals(budget.category, ignoreCase = true))
            }.sumOf { it.amount }

            val remaining = budget.budgetAmount - realization
            val percentUsed = if (budget.budgetAmount > 0) (realization / budget.budgetAmount) * 100.0 else 0.0

            BudgetRealization(
                budget = budget,
                realization = realization,
                remaining = remaining,
                percentUsed = percentUsed,
                isOver = realization > budget.budgetAmount
            )
        }
    }

    fun calculateCashFlowStatement(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        startDate: String,
        endDate: String
    ): CashFlowStatement {
        val settledTx = transactions.filter { it.status == "Selesai" }
        val periodTx = settledTx.filter { it.date >= startDate && it.date <= endDate }

        val opInflow = periodTx.filter {
            it.type == "MASUK" && it.category !in listOf("Modal", "Investasi")
        }.sumOf { it.amount }

        val investingIds = periodTx.filter {
            it.type == "KELUAR" && (it.allocation == "Proyek" || it.project.isNotBlank())
        }.map { it.id }.toSet()

        val invOutflow = periodTx.filter { it.id in investingIds }.sumOf { it.amount }
        val opOutflow = periodTx.filter { it.type == "KELUAR" && it.id !in investingIds }.sumOf { it.amount }

        val finInflow = periodTx.filter {
            it.type == "MASUK" && it.category in listOf("Modal", "Investasi")
        }.sumOf { it.amount }

        val netOp = opInflow - opOutflow
        val netInv = -invOutflow
        val netFin = finInflow
        val netTotal = netOp + netInv + netFin

        // Opening balance is the consolidated ledger balance immediately before the report period.
        val openingNet = settledTx.filter { it.date < startDate }.sumOf {
            when (it.type) {
                "MASUK" -> it.amount
                "KELUAR" -> -it.amount
                else -> 0.0 // Internal transfers are neutral to consolidated cash.
            }
        }
        val openingBal = accounts.sumOf { it.initialBalance } + openingNet
        val closingBal = openingBal + netTotal

        return CashFlowStatement(
            period = "$startDate s/d $endDate",
            operatingInflow = opInflow,
            operatingOutflow = opOutflow,
            netOperatingFlow = netOp,
            investingOutflow = invOutflow,
            netInvestingFlow = netInv,
            financingInflow = finInflow,
            netFinancingFlow = netFin,
            netCashChange = netTotal,
            openingBalance = openingBal,
            closingBalance = closingBal
        )
    }
    fun calculateAttendanceSummary(attendances: List<AttendanceEntity>): AttendanceSummary {
        val empSet = attendances.map { it.employeeId }.toSet()
        val hadir = attendances.count { it.status.equals("Hadir", ignoreCase = true) }
        val izin = attendances.count { it.status.equals("Izin", ignoreCase = true) }
        val sakit = attendances.count { it.status.equals("Sakit", ignoreCase = true) }
        val alpa = attendances.count { it.status.equals("Alpa", ignoreCase = true) }
        val cuti = attendances.count { it.status.equals("Cuti", ignoreCase = true) }
        val lembur = attendances.sumOf { it.overtimeHours }
        val allowance = attendances.sumOf { it.dailyAllowance }

        return AttendanceSummary(
            totalEmployees = empSet.size,
            totalHadir = hadir,
            totalIzin = izin,
            totalSakit = sakit,
            totalAlpa = alpa,
            totalCuti = cuti,
            totalOvertimeHours = lembur,
            totalAllowance = allowance
        )
    }

    fun runIntegrityAudit(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receivables: List<ReceivableEntity>,
        employees: List<EmployeeEntity>,
        attendances: List<AttendanceEntity>
    ): IntegrityAuditResult {
        val issues = mutableListOf<String>()
        val checkedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val accountNames = accounts.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()
        val employeeIds = employees.map { it.id }.toSet()
        val validTypes = setOf("MASUK", "KELUAR", "TRANSFER")
        val validStatuses = setOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")

        transactions.forEach { tx ->
            if (!tx.amount.isFinite() || tx.amount <= 0.0) issues += "Transaksi ${tx.id}: nominal tidak valid."
            if (tx.type !in validTypes) issues += "Transaksi ${tx.id}: tipe ${tx.type} tidak valid."
            if (tx.status !in validStatuses) issues += "Transaksi ${tx.id}: status ${tx.status} tidak valid."
            if (tx.account.trim().lowercase(Locale.getDefault()) !in accountNames) issues += "Transaksi ${tx.id}: akun ${tx.account} tidak terdaftar."
            if (tx.type == "TRANSFER") {
                val destination = tx.toAccount?.trim().orEmpty()
                if (destination.isBlank()) issues += "Transfer ${tx.id}: akun tujuan kosong."
                else if (destination.lowercase(Locale.getDefault()) !in accountNames) issues += "Transfer ${tx.id}: akun tujuan $destination tidak terdaftar."
                else if (destination.equals(tx.account, ignoreCase = true)) issues += "Transfer ${tx.id}: akun asal sama dengan tujuan."
            }
        }

        budgets.forEach { budget ->
            if (!budget.budgetAmount.isFinite() || budget.budgetAmount < 0.0) issues += "Anggaran ${budget.id}: nominal pagu tidak valid."
        }

        receivables.forEach { item ->
            if (!item.totalAmount.isFinite() || item.totalAmount <= 0.0) issues += "${item.type} ${item.id}: total nominal tidak valid."
            if (!item.paidAmount.isFinite() || item.paidAmount < 0.0 || item.paidAmount > item.totalAmount) issues += "${item.type} ${item.id}: paidAmount tidak valid."
            if (item.targetAccount.trim().lowercase(Locale.getDefault()) !in accountNames) issues += "${item.type} ${item.id}: akun terkait tidak terdaftar."
        }

        attendances.forEach { att ->
            if (att.employeeId !in employeeIds) issues += "Absensi ${att.id}: karyawan ${att.employeeId} tidak terdaftar."
            if (att.overtimeHours < 0.0 || att.dailyAllowance < 0.0) issues += "Absensi ${att.id}: nilai lembur/tunjangan tidak valid."
        }

        val balances = calculateAccountBalances(accounts, transactions)
        val calculatedTotal = balances.sumOf { it.currentBalance }
        val expectedTotal = accounts.sumOf { it.initialBalance } +
            transactions.filter { it.status == "Selesai" && it.type == "MASUK" }.sumOf { it.amount } -
            transactions.filter { it.status == "Selesai" && it.type == "KELUAR" }.sumOf { it.amount }

        if (kotlin.math.abs(calculatedTotal - expectedTotal) > 0.01) {
            issues += "Invariant saldo gagal: total saldo akun tidak sama dengan saldo awal + arus kas."
        }

        return IntegrityAuditResult(passed = issues.isEmpty(), issues = issues.distinct(), checkedAt = checkedAt)
    }
    // Export WhatsApp Text Formatters
    fun formatWhatsAppText(message: String, phone: String = ""): String {
        val encoded = URLEncoder.encode(message, "UTF-8")
        return if (phone.isNotBlank()) {
            val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
            "https://wa.me/$cleanPhone?text=$encoded"
        } else {
            "https://api.whatsapp.com/send?text=$encoded"
        }
    }

    suspend fun exportDataToJson(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        budgets: List<BudgetEntity>,
        receivables: List<ReceivableEntity>,
        notes: List<CashNoteEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Sistem Kas Terintegrasi")
        root.put("version", "2.0")
        root.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("date", tx.date)
                put("time", tx.time)
                put("account", tx.account)
                put("toAccount", tx.toAccount ?: "")
                put("name", tx.name)
                put("category", tx.category)
                put("description", tx.description)
                put("amount", tx.amount)
                put("allocation", tx.allocation)
                put("pic", tx.pic)
                put("proofUrl", tx.proofUrl)
                put("receiptNo", tx.receiptNo)
                put("project", tx.project)
                put("note", tx.note)
                put("status", tx.status)
                put("inputTime", tx.inputTime)
                put("inputBy", tx.inputBy)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("initialBalance", acc.initialBalance)
                put("colorHex", acc.colorHex)
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val budgetArray = JSONArray()
        budgets.forEach { budget ->
            budgetArray.put(JSONObject().apply {
                put("id", budget.id)
                put("period", budget.period)
                put("category", budget.category)
                put("budgetAmount", budget.budgetAmount)
                put("notes", budget.notes)
            })
        }
        root.put("budgets", budgetArray)

        val receivableArray = JSONArray()
        receivables.forEach { item ->
            receivableArray.put(JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("date", item.date)
                put("customerName", item.customerName)
                put("description", item.description)
                put("totalAmount", item.totalAmount)
                put("dueDate", item.dueDate)
                put("paidAmount", item.paidAmount)
                put("targetAccount", item.targetAccount)
                put("notes", item.notes)
                put("status", item.status)
            })
        }
        root.put("receivables", receivableArray)

        val noteArray = JSONArray()
        notes.forEach { note ->
            noteArray.put(JSONObject().apply {
                put("id", note.id)
                put("date", note.date)
                put("title", note.title)
                put("content", note.content)
                put("pic", note.pic)
                put("priority", note.priority)
                put("status", note.status)
            })
        }
        root.put("notes", noteArray)

        return root.toString(2)
    }

    suspend fun restoreDataFromJson(jsonStr: String): Result<Int> {
        return try {
            val root = JSONObject(jsonStr)
            var count = 0

            if (root.has("accounts")) {
                val accArray = root.getJSONArray("accounts")
                val accList = mutableListOf<AccountEntity>()
                for (i in 0 until accArray.length()) {
                    val o = accArray.getJSONObject(i)
                    accList.add(
                        AccountEntity(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            type = o.optString("type", "Kas"),
                            initialBalance = o.optDouble("initialBalance", 0.0),
                            colorHex = o.optString("colorHex", "#1E56A0")
                        )
                    )
                }
                accountDao.insertAccounts(accList)
            }

            if (root.has("budgets")) {
                val array = root.getJSONArray("budgets")
                val list = mutableListOf<BudgetEntity>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(BudgetEntity(o.optLong("id", 0L), o.getString("period"), o.getString("category"), o.getDouble("budgetAmount"), o.optString("notes", "")))
                }
                budgetDao.insertBudgets(list)
            }

            if (root.has("receivables")) {
                val array = root.getJSONArray("receivables")
                val list = mutableListOf<ReceivableEntity>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(ReceivableEntity(
                        id = o.getString("id"),
                        type = o.optString("type", "PIUTANG"),
                        date = o.getString("date"),
                        customerName = o.getString("customerName"),
                        description = o.optString("description", ""),
                        totalAmount = o.getDouble("totalAmount"),
                        dueDate = o.getString("dueDate"),
                        paidAmount = o.optDouble("paidAmount", 0.0),
                        targetAccount = o.optString("targetAccount", ""),
                        notes = o.optString("notes", ""),
                        status = o.optString("status", "Belum Jatuh Tempo")
                    ))
                }
                receivableDao.insertReceivables(list)
            }

            if (root.has("notes")) {
                val array = root.getJSONArray("notes")
                val list = mutableListOf<CashNoteEntity>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(CashNoteEntity(
                        id = o.optLong("id", 0L),
                        date = o.getString("date"),
                        title = o.getString("title"),
                        content = o.optString("content", ""),
                        pic = o.optString("pic", "Admin"),
                        priority = o.optString("priority", "Sedang"),
                        status = o.optString("status", "Open")
                    ))
                }
                noteDao.insertNotes(list)
            }
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                val txList = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            id = o.getString("id"),
                            type = o.getString("type"),
                            date = o.getString("date"),
                            time = o.optString("time", "12:00:00"),
                            account = o.getString("account"),
                            toAccount = o.optString("toAccount").takeIf { it.isNotBlank() },
                            name = o.getString("name"),
                            category = o.getString("category"),
                            description = o.optString("description", ""),
                            amount = o.getDouble("amount"),
                            allocation = o.optString("allocation", "Operasional"),
                            pic = o.optString("pic", "Admin"),
                            proofUrl = o.optString("proofUrl", ""),
                            receiptNo = o.optString("receiptNo", ""),
                            project = o.optString("project", ""),
                            note = o.optString("note", ""),
                            status = o.optString("status", "Selesai"),
                            inputTime = o.optLong("inputTime", System.currentTimeMillis()),
                            inputBy = o.optString("inputBy", "Admin")
                        )
                    )
                    count++
                }
                transactionDao.insertTransactions(txList)
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Tipe,Tanggal,Jam,Akun,Ke Akun,Nama Transaksi,Kategori,Keterangan,Nominal,Alokasi,PIC,Bukti,No Bukti,Status\n")
        transactions.forEach { tx ->
            val cleanDesc = tx.description.replace(",", ";").replace("\n", " ")
            val cleanName = tx.name.replace(",", ";")
            sb.append("${tx.id},${tx.type},${tx.date},${tx.time},${tx.account},${tx.toAccount ?: ""},$cleanName,${tx.category},$cleanDesc,${tx.amount.toLong()},${tx.allocation},${tx.pic},${tx.proofUrl},${tx.receiptNo},${tx.status}\n")
        }
        return sb.toString()
    }

    suspend fun importTransactionsFromCsv(csvContent: String): Result<Int> {
        return try {
            val lines = csvContent.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) return Result.success(0)
            val header = lines.first()
            val dataRows = lines.drop(1)
            val txList = mutableListOf<TransactionEntity>()

            dataRows.forEach { row ->
                val cols = row.split(",").map { it.trim() }
                if (cols.size >= 10) {
                    val id = cols.getOrNull(0)?.ifBlank { generateId("TX") } ?: generateId("TX")
                    val type = cols.getOrNull(1)?.uppercase() ?: "MASUK"
                    val date = cols.getOrNull(2)?.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) } ?: ""
                    val time = cols.getOrNull(3)?.ifBlank { "12:00:00" } ?: "12:00:00"
                    val account = cols.getOrNull(4)?.ifBlank { "Kas Tunai" } ?: "Kas Tunai"
                    val toAccount = cols.getOrNull(5)?.takeIf { it.isNotBlank() }
                    val name = cols.getOrNull(6)?.ifBlank { "Transaksi CSV" } ?: "Transaksi CSV"
                    val category = cols.getOrNull(7)?.ifBlank { "Lainnya" } ?: "Lainnya"
                    val desc = cols.getOrNull(8) ?: ""
                    val amount = cols.getOrNull(9)?.toDoubleOrNull() ?: 0.0
                    val alloc = cols.getOrNull(10)?.ifBlank { "Operasional" } ?: "Operasional"
                    val pic = cols.getOrNull(11)?.ifBlank { "Admin" } ?: "Admin"
                    val proof = cols.getOrNull(12) ?: ""
                    val receiptNo = cols.getOrNull(13) ?: ""
                    val status = cols.getOrNull(14)?.ifBlank { "Selesai" } ?: "Selesai"

                    txList.add(
                        TransactionEntity(
                            id = id,
                            type = type,
                            date = date,
                            time = time,
                            account = account,
                            toAccount = toAccount,
                            name = name,
                            category = category,
                            description = desc,
                            amount = amount,
                            allocation = alloc,
                            pic = pic,
                            proofUrl = proof,
                            receiptNo = receiptNo,
                            status = status,
                            inputTime = System.currentTimeMillis(),
                            inputBy = "CSV Importer"
                        )
                    )
                }
            }

            if (txList.isNotEmpty()) {
                transactionDao.insertTransactions(txList)
            }
            Result.success(txList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generatePrintableSummaryText(
        companyName: String,
        accounts: List<AccountWithBalance>,
        transactions: List<TransactionEntity>
    ): String {
        val today = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val totalSaldo = accounts.sumOf { it.currentBalance }
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("             LAPORAN KAS & KEUANGAN RESMI           \n")
        sb.append("             $companyName\n")
        sb.append("             Waktu Cetak: $today\n")
        sb.append("====================================================\n\n")

        sb.append("1. REKAPITULASI SALDO AKUN KAS & BANK:\n")
        accounts.forEach { acc ->
            sb.append("   • ${acc.account.name.padEnd(20)} : Rp ${acc.currentBalance.toLong()} (In: Rp ${acc.totalMasuk.toLong()}, Out: Rp ${acc.totalKeluar.toLong()})\n")
        }
        sb.append("   -------------------------------------------------\n")
        sb.append("   TOTAL KAS TERSEDIA   : Rp ${totalSaldo.toLong()}\n\n")

        sb.append("2. DAFTAR 10 TRANSAKSI TERAKHIR:\n")
        transactions.take(10).forEach { tx ->
            val sign = if (tx.type == "MASUK") "+" else if (tx.type == "KELUAR") "-" else "="
            sb.append("   • [${tx.date}] ${tx.name.take(25).padEnd(25)} : $sign Rp ${tx.amount.toLong()} (${tx.account})\n")
        }
        sb.append("\n====================================================\n")
        sb.append("Status Audit: Ringkasan berdasarkan ledger aktif\n")
        sb.append("Sistem Kas - Ringkasan Ledger Lokal\n")
        return sb.toString()
    }
}
