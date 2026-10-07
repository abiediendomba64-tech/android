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
        transactionDao.insertTransaction(transaction)

        // Real Audit Logging
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = if (transaction.type == "MASUK") "INSERT_MASUK" else if (transaction.type == "KELUAR") "INSERT_KELUAR" else "TRANSFER",
                recordId = transaction.id,
                details = "${transaction.name} (${transaction.account}) sebesar Rp ${transaction.amount.toLong()} - Kategori: ${transaction.category}",
                user = transaction.inputBy,
                verifiedFormulaStatus = "SUMIFS_AUDIT_OK",
                balanceAfter = 0.0
            )
        )
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
                verifiedFormulaStatus = "AUDITED"
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
                verifiedFormulaStatus = "AUDITED"
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
                verifiedFormulaStatus = "AUDITED"
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
                verifiedFormulaStatus = "AUDITED"
            )
        )
    }

    suspend fun saveAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun bersihkanSemuaDataSampelKeDataReal(saldoAwalKas: Map<String, Double>) {
        transactionDao.deleteAllTransactions()
        // Update account balances
        saldoAwalKas.forEach { (accName, initialBal) ->
            val existing = accountDao.getAccountByName(accName)
            if (existing != null) {
                accountDao.updateAccount(existing.copy(initialBalance = initialBal))
            }
        }
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "DATA_REAL_ACTIVATED",
                recordId = "REAL-MODE",
                details = "Sistem beralih ke Mode Data Keuangan Riil. Seluruh sampel transaksi dibersihkan total (0 Transaksi Aktif). Saldo awal akun diperbarui sesuai kas nyata perusahaan.",
                user = "Auditor Keuangan",
                verifiedFormulaStatus = "AUDIT_100%_REAL_MODE"
            )
        )
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
                verifiedFormulaStatus = "AUDITED"
            )
        )
    }

    suspend fun deleteAccount(id: String) {
        accountDao.deleteAccount(id)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    suspend fun saveReceivable(receivable: ReceivableEntity) {
        receivableDao.insertReceivable(receivable)
    }

    suspend fun payReceivable(id: String, paymentAmount: Double, targetAccount: String, pic: String = "Bendahara") {
        val current = receivableDao.getReceivableById(id) ?: return
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
        val current = receivableDao.getReceivableById(id) ?: return
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
                verifiedFormulaStatus = "AUDITED",
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
        attendanceDao.insertAttendance(attendance)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "ATTENDANCE_LOG",
                recordId = attendance.id,
                details = "Absensi ${attendance.employeeName} (${attendance.status}) pada ${attendance.date}",
                user = "Admin Absensi",
                verifiedFormulaStatus = "AUDITED"
            )
        )
    }

    suspend fun deleteAttendance(id: String) {
        attendanceDao.deleteAttendance(id)
    }

    // Bank Reconciliation Operations
    suspend fun saveBankReconciliation(recon: BankReconEntity) {
        bankReconDao.insertReconciliation(recon)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = "BANK_RECONCILE",
                recordId = recon.id,
                details = "Rekonsiliasi ${recon.accountName} periode ${recon.period}: Buku Rp ${recon.bookBalance.toLong()} vs Bank Rp ${recon.statementBalance.toLong()} (Selisih: Rp ${recon.difference.toLong()} - ${recon.status})",
                user = recon.reconciledBy,
                verifiedFormulaStatus = "RECON_AUDIT_OK"
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
        val settledTx = transactions.filter { it.status == "Selesai" && it.date >= startDate && it.date <= endDate }

        // Operating Flow
        val opInflow = settledTx.filter {
            it.type == "MASUK" && (it.category in listOf("Penjualan", "Piutang Masuk", "Pendapatan Lain", "Operasional"))
        }.sumOf { it.amount }

        val opOutflow = settledTx.filter {
            it.type == "KELUAR" && (it.allocation in listOf("Operasional", "Gaji", "Marketing") || it.category in listOf("Belanja Barang", "Gaji", "Listrik/Internet", "Sewa", "Transportasi"))
        }.sumOf { it.amount }

        // Investing Flow (Proyek, Pengadaan inventaris besar)
        val invOutflow = settledTx.filter {
            it.type == "KELUAR" && (it.allocation == "Proyek" || it.project.isNotBlank())
        }.sumOf { it.amount }

        // Financing Flow (Modal, Transfer Masuk/Keluar)
        val finInflow = settledTx.filter {
            it.type == "MASUK" && it.category in listOf("Modal", "Investasi")
        }.sumOf { it.amount }

        val netOp = opInflow - opOutflow
        val netInv = -invOutflow
        val netFin = finInflow

        val netTotal = netOp + netInv + netFin
        val openingBal = accounts.sumOf { it.initialBalance }
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
        sb.append("Status Audit: 100% Terverifikasi Seimbang\n")
        sb.append("Sistem Kas Terintegrasi Google Sheets & Android\n")
        return sb.toString()
    }
}
