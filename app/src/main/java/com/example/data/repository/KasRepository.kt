package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceDao
import com.example.data.local.AuditDao
import com.example.data.local.BankReconDao
import com.example.data.local.BudgetDao
import com.example.data.local.EmployeeDao
import com.example.data.local.NoteDao
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectPlanDao
import com.example.data.local.HousingUnitDao
import com.example.data.local.ReceivableDao
import com.example.data.local.TransactionDao
import androidx.room.withTransaction
import com.example.data.model.AccountEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BankReconEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CashNoteEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.ReceivableEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectPlanEntity
import com.example.data.model.HousingUnitEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
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

data class PayrollCalculation(
    val employeeId: String,
    val baseSalary: Double,
    val attendanceAllowance: Double,
    val overtimePay: Double,
    val absenceDeduction: Double,
    val gross: Double,
    val totalDeduction: Double,
    val net: Double
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

data class IntegrityAuditSnapshot(
    val result: IntegrityAuditResult,
    val totalBalance: Double,
    val transactionCount: Int
)

class KasRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val receivableDao: ReceivableDao,
    private val noteDao: NoteDao,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val auditDao: AuditDao,
    private val bankReconDao: BankReconDao,
    private val projectDao: ProjectDao,
    private val projectPlanDao: ProjectPlanDao,
    private val housingUnitDao: HousingUnitDao
) {
    val activeTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllActiveTransactions()
    val archivedTransactions: Flow<List<TransactionEntity>> = transactionDao.getArchivedTransactions()
    // Archived records remain part of the financial ledger; archive is not a reversal.
    val ledgerTransactions: Flow<List<TransactionEntity>> = combine(
        activeTransactions,
        archivedTransactions
    ) { active, archived ->
        (active + archived).distinctBy { it.id }
    }
    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val activeAccounts: Flow<List<AccountEntity>> = accountDao.getAllActiveAccounts()
    val budgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val receivables: Flow<List<ReceivableEntity>> = receivableDao.getAllReceivables()
    val notes: Flow<List<CashNoteEntity>> = noteDao.getAllNotes()
    val employees: Flow<List<EmployeeEntity>> = employeeDao.getAllActiveEmployees()
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    val attendances: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendances()
    val auditLogs: Flow<List<AuditLogEntity>> = auditDao.getRecentAuditLogs()
    val bankReconciliations: Flow<List<BankReconEntity>> = bankReconDao.getAllReconciliations()
    val projects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val activeProjects: Flow<List<ProjectEntity>> = projectDao.getActiveProjects()
    val projectPlans: Flow<List<ProjectPlanEntity>> = projectPlanDao.getAllPlans()
    val housingUnits: Flow<List<HousingUnitEntity>> = housingUnitDao.getAllUnits()

    suspend fun saveTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            require(transactionDao.getTransactionById(transaction.id) == null) {
                "ID transaksi " + transaction.id + " sudah digunakan."
            }
            insertValidatedTransaction(transaction)
        }
    }

    private suspend fun insertValidatedTransaction(
        transaction: TransactionEntity,
        allowSystemReceiptNo: Boolean = false
    ) {
        validateTransaction(transaction, allowSystemReceiptNo = allowSystemReceiptNo)
        transactionDao.insertTransaction(transaction)
        val postBalance = accountDao.getAccountByName(transaction.account)?.let { account ->
            account.initialBalance + transactionDao.sumSettledAccountMovement(account.name)
        } ?: 0.0
        val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        auditDao.insertAuditLog(
            AuditLogEntity(
                dateFormatted = nowStamp,
                action = if (transaction.type == "MASUK") "INSERT_MASUK" else if (transaction.type == "KELUAR") "INSERT_KELUAR" else "TRANSFER",
                recordId = transaction.id,
                details = transaction.name + " (" + transaction.account + ") sebesar Rp " + transaction.amount.toLong() + " - Kategori: " + transaction.category,
                user = transaction.inputBy,
                verifiedFormulaStatus = "RECORDED",
                balanceAfter = postBalance
            )
        )
    }

    private fun requireIsoDate(value: String, field: String) {
        require(value.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) { field + " harus YYYY-MM-DD." }
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = parser.parse(value, position)
        require(parsed != null && position.index == value.length && parser.format(parsed) == value) {
            field + " bukan tanggal kalender yang valid."
        }
    }

    private fun requireIsoTime(value: String, field: String) {
        require(value.matches(Regex("""\d{2}:\d{2}:\d{2}"""))) { field + " harus HH:mm:ss." }
        val parser = SimpleDateFormat("HH:mm:ss", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = parser.parse(value, position)
        require(parsed != null && position.index == value.length && parser.format(parsed) == value) {
            field + " bukan waktu yang valid."
        }
    }

    private suspend fun validateTransaction(
        transaction: TransactionEntity,
        allowInactiveAccountReferences: Boolean = false,
        allowLegacyTransferIncome: Boolean = false,
        allowSystemReceiptNo: Boolean = false
    ) {
        require(transaction.id.isNotBlank()) { "ID transaksi wajib diisi." }
        val normalizedReceiptNo = transaction.receiptNo.trim()
        if (!allowSystemReceiptNo) {
            require(
                !normalizedReceiptNo.startsWith("PAYROLL-", ignoreCase = true) &&
                    !normalizedReceiptNo.startsWith("PIU-", ignoreCase = true) &&
                    !normalizedReceiptNo.startsWith("HUT-", ignoreCase = true)
            ) {
                "Nomor bukti PAYROLL-, PIU-, dan HUT- hanya dibuat melalui alur payroll/pembayaran tagihan, bukan transaksi manual."
            }
        }
        requireIsoDate(transaction.date, "Tanggal transaksi")
        requireIsoTime(transaction.time, "Jam transaksi")
        require(transaction.name.isNotBlank()) { "Nama transaksi wajib diisi." }
        require(transaction.category.isNotBlank()) { "Kategori transaksi wajib diisi." }
        require(transaction.allocation.isNotBlank()) { "Alokasi transaksi wajib diisi." }
        require(transaction.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana transaksi tidak valid." }
        require(transaction.pic.isNotBlank()) { "PIC transaksi wajib diisi." }
        require(transaction.inputBy.isNotBlank()) { "Input Oleh transaksi wajib diisi." }
        require(transaction.inputTime > 0L) { "Waktu input transaksi tidak valid." }
        require(transaction.amount.isFinite() && transaction.amount > 0.0) { "Nominal transaksi harus lebih besar dari Rp 0." }
        require(transaction.type in setOf("MASUK", "KELUAR", "TRANSFER")) { "Tipe transaksi tidak valid." }
        require(allowLegacyTransferIncome || !(transaction.type == "MASUK" && transaction.category.equals("Transfer Masuk", ignoreCase = true))) {
            "Transfer antar akun harus dicatat melalui menu Transfer agar tidak dihitung sebagai pendapatan."
        }
        require(transaction.status in setOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")) { "Status transaksi tidak valid." }
        val sourceAccountCount = accountDao.countByName(transaction.account)
        require(sourceAccountCount == 1) {
            if (sourceAccountCount == 0) "Akun ${transaction.account} tidak terdaftar."
            else "Nama akun ${transaction.account} duplikat; rapikan master akun sebelum transaksi baru."
        }
        val sourceAccount = accountDao.getAccountByName(transaction.account)
        require(sourceAccount != null && (sourceAccount.isActive || allowInactiveAccountReferences)) {
            "Akun " + transaction.account + " nonaktif."
        }
        if (transaction.project.isNotBlank()) {
            require(projectDao.getProjectByName(transaction.project) != null) {
                "Proyek transaksi tidak terdaftar: " + transaction.project
            }
        }
        if (transaction.isArchived) {
            require(transaction.archivedAt != null && transaction.archivedAt > 0L) { "Transaksi arsip harus memiliki waktu arsip." }
            require(transaction.archivedBy?.isNotBlank() == true) { "Transaksi arsip harus memiliki pengarsip." }
        } else {
            require(transaction.archivedAt == null && transaction.archivedBy == null) {
                "Transaksi aktif tidak boleh memiliki metadata arsip."
            }
        }
        if (transaction.type == "TRANSFER") {
            val destination = transaction.toAccount?.trim()?.takeIf { it.isNotBlank() }
            require(destination != null) { "Akun tujuan transfer wajib diisi." }
            require(!destination.equals(transaction.account, ignoreCase = true)) {
                "Akun asal dan tujuan transfer tidak boleh sama."
            }
            val destinationAccountCount = accountDao.countByName(destination)
            require(destinationAccountCount == 1) {
                if (destinationAccountCount == 0) "Akun tujuan ${destination} tidak terdaftar."
                else "Nama akun tujuan ${destination} duplikat; rapikan master akun sebelum transfer."
            }
            val destinationAccount = accountDao.getAccountByName(destination)
            require(destinationAccount != null && (destinationAccount.isActive || allowInactiveAccountReferences)) {
                "Akun tujuan " + destination + " nonaktif."
            }
        } else {
            require(transaction.toAccount == null) {
                "Akun tujuan hanya boleh diisi untuk transfer."
            }
        }
    }

    suspend fun archiveTransaction(id: String, archivedBy: String = "Admin") {
        database.withTransaction {
            val current = transactionDao.getTransactionById(id)
                ?: throw IllegalArgumentException("Transaksi " + id + " tidak ditemukan.")
            require(!current.isArchived) { "Transaksi " + id + " sudah diarsipkan." }
            require(archivedBy.isNotBlank()) { "Pengarsip wajib diisi." }
            transactionDao.archiveTransaction(id, System.currentTimeMillis(), archivedBy)
            auditDao.insertAuditLog(
                AuditLogEntity(
                    dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    action = "ARCHIVE_TRANSACTION",
                    recordId = id,
                    details = "Transaksi dipindahkan ke Arsip tanpa menghapus histori ledger.",
                    user = archivedBy,
                    verifiedFormulaStatus = "RECORDED"
                )
            )
        }
    }

    suspend fun restoreTransaction(id: String) {
        database.withTransaction {
            val current = transactionDao.getTransactionById(id)
                ?: throw IllegalArgumentException("Transaksi " + id + " tidak ditemukan.")
            require(current.isArchived) { "Transaksi " + id + " tidak sedang diarsipkan." }
            validateTransaction(
                current.copy(isArchived = false, archivedAt = null, archivedBy = null),
                allowInactiveAccountReferences = true
            )
            transactionDao.restoreTransaction(id)
            auditDao.insertAuditLog(
                AuditLogEntity(
                    dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    action = "RESTORE_TRANSACTION",
                    recordId = id,
                    details = "Transaksi dipulihkan kembali ke ledger aktif.",
                    user = "Admin",
                    verifiedFormulaStatus = "RECORDED"
                )
            )
        }
    }

    suspend fun permanentlyDeleteTransaction(id: String): Nothing =
        error("Penghapusan permanen transaksi dinonaktifkan untuk menjaga histori ledger.")

    suspend fun emptyTrash(): Nothing =
        error("Pengosongan arsip permanen dinonaktifkan untuk menjaga histori ledger.")

    suspend fun saveAccount(account: AccountEntity) {
        require(account.id.isNotBlank()) { "ID akun wajib diisi." }
        require(account.name.isNotBlank()) { "Nama akun wajib diisi." }
        require(account.type in setOf("Kas", "Bank", "E-Wallet", "Lainnya")) { "Jenis akun tidak valid." }
        require(account.initialBalance.isFinite() && account.initialBalance >= 0.0) { "Saldo awal akun tidak valid." }
        database.withTransaction {
            require(accountDao.getAccountById(account.id) == null) { "ID akun " + account.id + " sudah digunakan." }
            require(accountDao.countByName(account.name.trim()) == 0) { "Nama akun sudah digunakan (tidak peka huruf besar/kecil)." }
            val saved = account.copy(name = account.name.trim())
            accountDao.insertAccount(saved)
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "CREATE_ACCOUNT",
                recordId = saved.id,
                details = "Akun ${saved.name} dibuat dengan saldo awal Rp ${saved.initialBalance.toLong()} dan jenis ${saved.type}.",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED",
                balanceAfter = saved.initialBalance
            ))
        }
    }

    suspend fun bulkMarkAllEmployeesHadir(date: String) {
        database.withTransaction {
            requireIsoDate(date, "Tanggal absensi")
            val paidPayroll = paidPayrollCoveringDate(date)
            require(paidPayroll == null) {
                "Absensi tanggal $date tidak dapat ditambahkan karena payroll ${paidPayroll?.receiptNo?.removePrefix("PAYROLL-")} sudah dicairkan."
            }
            val allEmployees = employeeDao.getAllEmployeesList().filter { it.isActive }
            require(allEmployees.isNotEmpty()) { "Tidak ada karyawan aktif untuk dicatat absensinya." }
            allEmployees.forEach { emp ->
                if (attendanceDao.getAttendanceByEmployeeAndDate(emp.id, date) == null) {
                    attendanceDao.insertAttendance(
                        AttendanceEntity(
                            id = "ATT-" + date.replace("-", "") + "-" + emp.id,
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
                    dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    action = "BULK_ATTENDANCE",
                    recordId = "ATT-BULK-" + date,
                    details = "Pencatatan absensi cepat untuk seluruh karyawan hadir pada tanggal " + date,
                    user = "Admin Absensi",
                    verifiedFormulaStatus = "RECORDED"
                )
            )
        }
    }

    suspend fun deleteAccount(id: String): Boolean {
        return database.withTransaction {
            val account = accountDao.getAccountById(id)
                ?: throw IllegalArgumentException("Akun " + id + " tidak ditemukan.")
            val references = transactionDao.countReferencesToAccount(account.name) +
                receivableDao.countReferencesToAccount(account.name) +
                bankReconDao.countReferencesToAccount(account.name)
            val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            if (references > 0) {
                accountDao.updateAccount(account.copy(isActive = false))
                auditDao.insertAuditLog(AuditLogEntity(
                    dateFormatted = nowStamp,
                    action = "DEACTIVATE_ACCOUNT",
                    recordId = account.id,
                    details = "Akun " + account.name + " dinonaktifkan karena masih memiliki " + references + " referensi ledger/master.",
                    user = "Admin",
                    verifiedFormulaStatus = "RECORDED"
                ))
                false
            } else {
                accountDao.deleteAccount(id)
                auditDao.insertAuditLog(AuditLogEntity(
                    dateFormatted = nowStamp,
                    action = "DELETE_ACCOUNT",
                    recordId = account.id,
                    details = "Akun " + account.name + " dihapus karena tidak memiliki referensi data.",
                    user = "Admin",
                    verifiedFormulaStatus = "RECORDED"
                ))
                true
            }
        }
    }


    suspend fun saveProject(project: ProjectEntity) {
        require(project.id.isNotBlank()) { "ID proyek wajib diisi." }
        require(project.name.isNotBlank()) { "Nama proyek wajib diisi." }
        require(project.category in setOf("Pembebasan Tanah", "Cut & Fill", "Perumahan", "Perdagangan", "Operasional PT", "Lainnya")) { "Jenis proyek tidak valid." }
        require(project.businessModel in setOf("Subsidi", "Komersial", "Tidak berlaku")) { "Model perumahan tidak valid." }
        require(project.category == "Perumahan" || project.businessModel == "Tidak berlaku") { "Model Subsidi/Komersial hanya digunakan untuk proyek perumahan." }
        requireIsoDate(project.startDate, "Tanggal mulai proyek")
        requireIsoDate(project.targetEndDate, "Target selesai proyek")
        require(project.targetEndDate >= project.startDate) { "Target selesai tidak boleh lebih awal dari tanggal mulai." }
        require(project.budgetAmount.isFinite() && project.budgetAmount >= 0.0) { "Pagu proyek tidak valid." }
        require(project.status in setOf("Berjalan", "Ditunda", "Selesai")) { "Status proyek tidak valid." }
        database.withTransaction {
            require(projectDao.getProjectById(project.id) == null) { "ID proyek sudah digunakan." }
            require(projectDao.countByName(project.name) == 0) { "Nama proyek sudah ada. Gunakan nama yang berbeda." }
            projectDao.insertProject(project.copy(name = project.name.trim()))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "CREATE_PROJECT",
                recordId = project.id,
                details = "Proyek " + project.name.trim() + " dibuat; jenis " + project.category + "; model " + project.businessModel + "; pagu Rp " + project.budgetAmount.toLong() + ".",
                user = "Admin Proyek",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }


    suspend fun updateProjectStatus(id: String, status: String) {
        require(status in setOf("Berjalan", "Ditunda", "Selesai")) { "Status proyek tidak valid." }
        database.withTransaction {
            val current = projectDao.getProjectById(id)
                ?: throw IllegalArgumentException("Proyek $id tidak ditemukan.")
            if (current.status == status) return@withTransaction
            projectDao.updateProject(current.copy(status = status))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "UPDATE_PROJECT_STATUS",
                recordId = current.id,
                details = "Status proyek ${current.name} berubah dari ${current.status} menjadi $status.",
                user = "Admin Proyek",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    suspend fun saveHousingUnit(unit: HousingUnitEntity) {
        require(unit.id.isNotBlank()) { "ID unit wajib diisi." }
        require(unit.project.isNotBlank()) { "Unit harus ditautkan ke proyek perumahan." }
        require(unit.unitCode.isNotBlank()) { "Kode unit/kavling wajib diisi." }
        require(unit.businessModel in setOf("Subsidi", "Komersial")) { "Segmen unit harus Subsidi atau Komersial." }
        require(unit.landAreaM2.isFinite() && unit.landAreaM2 > 0.0) { "Luas tanah unit harus lebih besar dari nol." }
        require(unit.buildingAreaM2.isFinite() && unit.buildingAreaM2 >= 0.0) { "Luas bangunan unit tidak valid." }
        require(unit.salePrice.isFinite() && unit.salePrice > 0.0) { "Harga jual unit harus lebih besar dari nol." }
        require(unit.status in setOf("Tersedia", "Booking", "Terjual", "Dibatalkan")) { "Status unit tidak valid." }
        require(unit.status != "Terjual" || unit.buyerName.isNotBlank()) { "Nama pembeli diperlukan untuk unit berstatus Terjual." }
        database.withTransaction {
            require(housingUnitDao.getUnitById(unit.id) == null) { "ID unit sudah digunakan." }
            val project = projectDao.getProjectByName(unit.project)
                ?: throw IllegalArgumentException("Proyek perumahan tidak ditemukan.")
            require(project.isActive && project.category == "Perumahan") { "Unit hanya dapat ditambahkan ke proyek perumahan yang aktif." }
            require(project.businessModel == unit.businessModel) { "Segmen unit harus sama dengan segmen proyek perumahan." }
            require(housingUnitDao.countCodeInProject(project.name, unit.unitCode.trim()) == 0) { "Kode unit/kavling sudah digunakan pada proyek ini." }
            housingUnitDao.insertUnit(unit.copy(project = project.name, unitCode = unit.unitCode.trim()))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "CREATE_HOUSING_UNIT",
                recordId = unit.id,
                details = "Unit " + unit.unitCode.trim() + " ditambahkan ke proyek " + project.name + "; segmen " + unit.businessModel + "; harga Rp " + unit.salePrice.toLong() + ".",
                user = "Admin Proyek",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    suspend fun updateHousingUnitStatus(id: String, status: String, buyerName: String) {
        require(status in setOf("Tersedia", "Booking", "Terjual", "Dibatalkan")) { "Status unit tidak valid." }
        require(status != "Terjual" || buyerName.isNotBlank()) { "Nama pembeli diperlukan untuk unit berstatus Terjual." }
        database.withTransaction {
            val current = housingUnitDao.getUnitById(id) ?: throw IllegalArgumentException("Unit/kavling tidak ditemukan.")
            housingUnitDao.updateUnit(current.copy(status = status, buyerName = buyerName.trim()))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "UPDATE_HOUSING_UNIT_STATUS",
                recordId = id,
                details = "Status unit " + current.unitCode + " pada proyek " + current.project + " berubah menjadi " + status + ". Pendapatan hanya dicatat melalui transaksi kas masuk nyata.",
                user = "Admin Proyek",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    suspend fun saveProjectPlan(plan: ProjectPlanEntity) {
        require(plan.id.isNotBlank()) { "ID rencana wajib diisi." }
        require(plan.title.isNotBlank()) { "Judul rencana wajib diisi." }
        requireIsoDate(plan.planDate, "Tanggal rencana")
        require(plan.category.isNotBlank()) { "Kategori rencana wajib diisi." }
        require(plan.estimatedAmount.isFinite() && plan.estimatedAmount >= 0.0) { "Estimasi rencana tidak valid." }
        require(plan.status in setOf("Direncanakan", "Selesai", "Batal")) { "Status rencana tidak valid." }
        database.withTransaction {
            require(projectPlanDao.getPlanById(plan.id) == null) { "ID rencana sudah digunakan." }
            if (plan.project.isNotBlank()) require(projectDao.getProjectByName(plan.project)?.isActive == true) {
                "Proyek rencana tidak terdaftar atau nonaktif."
            }
            projectPlanDao.insertPlan(plan.copy(title = plan.title.trim(), project = plan.project.trim()))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "CREATE_PROJECT_PLAN",
                recordId = plan.id,
                details = "Rencana " + plan.title.trim() + " pada " + plan.planDate + "; proyek " + plan.project.ifBlank { "Umum/PT" } + "; status " + plan.status + "; estimasi Rp " + plan.estimatedAmount.toLong() + ".",
                user = "Admin Kalender",
                verifiedFormulaStatus = "PLAN_ONLY"
            ))
        }
    }

    suspend fun updateProjectPlanStatus(id: String, status: String) {
        require(status in setOf("Direncanakan", "Selesai", "Batal")) { "Status rencana tidak valid." }
        database.withTransaction {
            val current = projectPlanDao.getPlanById(id) ?: throw IllegalArgumentException("Rencana tidak ditemukan.")
            projectPlanDao.updatePlan(current.copy(status = status))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "UPDATE_PROJECT_PLAN_STATUS",
                recordId = id,
                details = "Status rencana " + current.title + " berubah menjadi " + status + ".",
                user = "Admin Kalender",
                verifiedFormulaStatus = "PLAN_ONLY"
            ))
        }
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        val requestedBucket = budget.fundBucket.trim()
        val normalizedBucket = listOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")
            .firstOrNull { it.equals(requestedBucket, ignoreCase = true) } ?: requestedBucket
        val requestedPeriod = budget.period.trim()
        val normalized = budget.copy(
            period = if (requestedPeriod.equals("All", ignoreCase = true)) "All" else requestedPeriod,
            category = budget.category.trim(),
            project = budget.project.trim(),
            fundBucket = normalizedBucket
        )
        require(normalized.period.equals("All", ignoreCase = true) || normalized.period.matches(Regex("""\d{4}-\d{2}"""))) { "Periode anggaran harus YYYY-MM atau All." }
        if (!normalized.period.equals("All", ignoreCase = true)) periodEndDate(normalized.period)
        require(normalized.category.isNotBlank()) { "Kategori anggaran wajib diisi." }
        require(normalized.budgetAmount.isFinite() && normalized.budgetAmount >= 0.0) { "Nominal anggaran tidak valid." }
        require(normalized.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana tidak valid." }
        if (normalized.project.isNotBlank()) require(projectDao.getProjectByName(normalized.project)?.isActive == true) { "Proyek anggaran tidak terdaftar atau nonaktif." }
        database.withTransaction {
            val matches = budgetDao.getMatchingBudgets(
                normalized.period, normalized.category, normalized.project, normalized.fundBucket
            )
            if (matches.isEmpty()) {
                budgetDao.insertBudget(normalized)
            } else {
                // Keep the most recently created line as the stable row, apply the new target,
                // and remove duplicate lines that would otherwise multiply realization totals.
                val keeper = matches.maxBy { it.id }
                budgetDao.updateBudget(normalized.copy(id = keeper.id))
                matches.filter { it.id != keeper.id }.forEach { budgetDao.deleteBudget(it.id) }
            }
        }
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    suspend fun saveReceivable(receivable: ReceivableEntity) {
        require(receivableDao.getReceivableById(receivable.id) == null) { "ID tagihan " + receivable.id + " sudah digunakan." }
        require(receivable.type in setOf("PIUTANG", "HUTANG")) { "Jenis tagihan tidak valid." }
        require(receivable.customerName.isNotBlank()) { "Nama pihak wajib diisi." }
        requireIsoDate(receivable.date, "Tanggal pencatatan")
        require(receivable.totalAmount.isFinite() && receivable.totalAmount > 0.0) { "Nominal tagihan harus lebih besar dari Rp 0." }
        require(receivable.paidAmount.isFinite() && receivable.paidAmount == 0.0) {
            "Tagihan baru harus dimulai dengan pembayaran Rp 0; catat pembayaran melalui menu Pembayaran Tagihan agar master dan ledger tetap sinkron."
        }
        requireIsoDate(receivable.dueDate, "Tanggal jatuh tempo")
        require(receivable.status in setOf("Belum Jatuh Tempo", "Jatuh Tempo")) {
            "Tagihan baru tidak dapat langsung berstatus Lunas. Catat pembayaran melalui menu Pembayaran Tagihan."
        }
        require(accountDao.countByName(receivable.targetAccount) == 1) {
            "Nama akun ${receivable.targetAccount} tidak unik; rapikan master akun terlebih dahulu."
        }
        require(accountDao.getAccountByName(receivable.targetAccount)?.isActive == true) { "Akun terkait nonaktif." }
        require(receivable.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana tagihan tidak valid." }
        if (receivable.project.isNotBlank()) require(projectDao.getProjectByName(receivable.project)?.isActive == true) {
            "Proyek tagihan tidak terdaftar atau nonaktif."
        }
        val normalizedStatus = if (receivable.paidAmount >= receivable.totalAmount) "Lunas" else receivable.status
        receivableDao.insertReceivable(receivable.copy(status = normalizedStatus, project = receivable.project.trim()))
    }

    suspend fun payReceivable(id: String, paymentAmount: Double, targetAccount: String, pic: String = "Bendahara") {
        database.withTransaction {
            val current = receivableDao.getReceivableById(id)
                ?: throw IllegalArgumentException("Piutang $id tidak ditemukan.")
            require(paymentAmount.isFinite() && paymentAmount > 0.0) { "Nominal pembayaran harus lebih besar dari Rp 0." }
            require(current.type == "PIUTANG") { "Record ${current.id} bukan piutang." }
            require(paymentAmount <= current.remainingAmount) { "Pembayaran melebihi sisa piutang ${current.id}." }
            require(accountDao.getAccountByName(targetAccount)?.isActive == true) { "Akun penerimaan $targetAccount tidak terdaftar atau nonaktif." }
            val updatedPaid = current.paidAmount + paymentAmount
            val newStatus = if (updatedPaid >= current.totalAmount) "Lunas" else current.status
            receivableDao.updateReceivable(current.copy(paidAmount = updatedPaid, status = newStatus))
            val now = Date()
            val tx = TransactionEntity(
                id = generateId("KM"),
                type = "MASUK",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
                account = targetAccount,
                name = "Pelunasan Piutang: ${current.customerName}",
                category = "Piutang Masuk",
                description = "Pembayaran piutang (${current.customerName} - ${current.description})",
                amount = paymentAmount,
                allocation = "Operasional",
                pic = pic,
                receiptNo = "PIU-${current.id}",
                project = current.project,
                fundBucket = current.fundBucket,
                status = "Selesai"
            )
            insertValidatedTransaction(tx, allowSystemReceiptNo = true)
        }
    }

    suspend fun payHutang(id: String, paymentAmount: Double, sourceAccount: String, pic: String = "Bendahara") {
        database.withTransaction {
            val current = receivableDao.getReceivableById(id)
                ?: throw IllegalArgumentException("Hutang $id tidak ditemukan.")
            require(paymentAmount.isFinite() && paymentAmount > 0.0) { "Nominal pembayaran harus lebih besar dari Rp 0." }
            require(current.type == "HUTANG") { "Record ${current.id} bukan hutang." }
            require(paymentAmount <= current.remainingAmount) { "Pembayaran melebihi sisa hutang ${current.id}." }
            require(accountDao.getAccountByName(sourceAccount)?.isActive == true) { "Akun pembayaran $sourceAccount tidak terdaftar atau nonaktif." }
            val updatedPaid = current.paidAmount + paymentAmount
            val newStatus = if (updatedPaid >= current.totalAmount) "Lunas" else current.status
            receivableDao.updateReceivable(current.copy(paidAmount = updatedPaid, status = newStatus))
            val now = Date()
            val tx = TransactionEntity(
                id = generateId("KK"),
                type = "KELUAR",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
                account = sourceAccount,
                name = "Pembayaran Hutang: ${current.customerName}",
                category = "Belanja Barang",
                description = "Pelunasan hutang pembelian (${current.customerName} - ${current.description})",
                amount = paymentAmount,
                allocation = "Operasional",
                pic = pic,
                receiptNo = "HUT-${current.id}",
                project = current.project,
                fundBucket = current.fundBucket,
                status = "Selesai"
            )
            insertValidatedTransaction(tx, allowSystemReceiptNo = true)
        }
    }

    suspend fun updateTransaction(newTx: TransactionEntity, oldTx: TransactionEntity) {
        database.withTransaction {
            require(transactionDao.getTransactionById(newTx.id) != null) { "Transaksi ${newTx.id} tidak ditemukan." }
            val actualOld = transactionDao.getTransactionById(newTx.id)
                ?: throw IllegalArgumentException("Transaksi ${newTx.id} tidak ditemukan.")
            require(oldTx.id == actualOld.id) { "ID transaksi lama dan baru tidak cocok." }
            require(
                !actualOld.receiptNo.startsWith("PAYROLL-", ignoreCase = true) &&
                    !actualOld.receiptNo.startsWith("PIU-", ignoreCase = true) &&
                    !actualOld.receiptNo.startsWith("HUT-", ignoreCase = true)
            ) {
                "Transaksi payroll atau pembayaran tagihan terikat ke histori terkait dan tidak dapat diedit. Catat koreksi melalui transaksi penyesuaian."
            }
            validateTransaction(newTx)
            transactionDao.updateTransaction(newTx)
            val postBalance = accountDao.getAccountByName(newTx.account)?.let { account ->
                account.initialBalance + transactionDao.sumSettledAccountMovement(account.name)
            } ?: 0.0
            val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            auditDao.insertAuditLog(
                AuditLogEntity(
                    dateFormatted = nowStamp,
                    action = "UPDATE_TRANSACTION",
                    recordId = newTx.id,
                    details = "Perubahan transaksi ${newTx.id}: [Lama: ${actualOld.name}, Rp ${actualOld.amount.toLong()}, ${actualOld.account}] -> [Baru: ${newTx.name}, Rp ${newTx.amount.toLong()}, ${newTx.account}]",
                    user = "Admin",
                    verifiedFormulaStatus = "RECORDED",
                    balanceAfter = postBalance
                )
            )
        }
    }

    suspend fun deleteReceivable(id: String) {
        database.withTransaction {
            val current = receivableDao.getReceivableById(id)
                ?: throw IllegalArgumentException("Tagihan $id tidak ditemukan.")
            val receiptPrefix = if (current.type == "PIUTANG") "PIU-" else "HUT-"
            val category = if (current.type == "PIUTANG") "Piutang Masuk" else "Belanja Barang"
            val linkedPayments = transactionDao.countByCategoryAndReceiptNo(category, receiptPrefix + id)
            require(current.paidAmount <= 0.0 && linkedPayments == 0) {
                "Tagihan $id sudah memiliki pembayaran di ledger. Data tidak dihapus agar histori kas tetap cocok."
            }
            receivableDao.deleteReceivable(id)
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "DELETE_RECEIVABLE",
                recordId = id,
                details = "Tagihan tanpa pembayaran dihapus; pihak " + current.customerName + ", jenis " + current.type + ".",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    suspend fun saveNote(note: CashNoteEntity) {
        require(note.title.isNotBlank()) { "Judul catatan wajib diisi." }
        requireIsoDate(note.date, "Tanggal catatan")
        require(note.priority in setOf("Rendah", "Sedang", "Tinggi")) { "Prioritas catatan tidak valid." }
        require(note.status in setOf("Open", "Done", "Follow Up")) { "Status catatan tidak valid." }
        if (note.project.isNotBlank()) require(projectDao.getProjectByName(note.project)?.isActive == true) { "Proyek catatan tidak terdaftar atau nonaktif." }
        noteDao.insertNote(note.copy(project = note.project.trim()))
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
    }

    // Employee & Attendance Operations
    suspend fun saveEmployee(employee: EmployeeEntity) {
        require(employee.name.isNotBlank()) { "Nama karyawan wajib diisi." }
        require(employee.dailyRate.isFinite() && employee.dailyRate >= 0.0) { "Uang harian tidak valid." }
        require(employee.monthlySalary.isFinite() && employee.monthlySalary >= 0.0) { "Gaji bulanan tidak valid." }
        if (employee.defaultProject.isNotBlank()) require(projectDao.getProjectByName(employee.defaultProject)?.isActive == true) { "Proyek alokasi karyawan tidak terdaftar atau nonaktif." }
        require(employeeDao.getEmployeeById(employee.id) == null) { "ID karyawan sudah digunakan." }
        employeeDao.insertEmployee(employee)
    }

    suspend fun deleteEmployee(id: String): Boolean {
        return database.withTransaction {
            val employee = employeeDao.getEmployeeById(id)
                ?: throw IllegalArgumentException("Karyawan " + id + " tidak ditemukan.")
            if (!employee.isActive) return@withTransaction false

            // There is no employee-level payroll snapshot table yet; monthly salary may have
            // been included in a paid payroll without an attendance row. Never hard-delete it.
            val references = attendanceDao.countReferencesToEmployee(id)
            val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            employeeDao.updateEmployee(employee.copy(isActive = false))
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = nowStamp,
                action = "DEACTIVATE_EMPLOYEE",
                recordId = employee.id,
                details = "Karyawan " + employee.name + " dinonaktifkan untuk mempertahankan histori payroll; referensi absensi: " + references + ".",
                user = "Admin",
                verifiedFormulaStatus = "RECORDED"
            ))
            false
        }
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
        database.withTransaction {
            require(attendanceDao.getAttendanceById(attendance.id) == null) {
                "ID absensi " + attendance.id + " sudah digunakan."
            }
            require(attendance.employeeId.isNotBlank()) { "ID karyawan wajib diisi." }
            val employee = employeeDao.getEmployeeById(attendance.employeeId)
                ?: throw IllegalArgumentException("Karyawan tidak ditemukan.")
            require(employee.isActive) { "Absensi baru tidak dapat dicatat untuk karyawan nonaktif." }
            requireIsoDate(attendance.date, "Tanggal absensi")
            val paidPayroll = paidPayrollCoveringDate(attendance.date)
            require(paidPayroll == null) {
                "Absensi tanggal ${attendance.date} tidak dapat ditambahkan karena payroll ${paidPayroll?.receiptNo?.removePrefix("PAYROLL-")} sudah dicairkan."
            }
            require(attendance.status in setOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti")) {
                "Status absensi tidak valid."
            }
            require(attendance.overtimeHours.isFinite() && attendance.overtimeHours >= 0.0) {
                "Jam lembur tidak valid."
            }
            require(attendance.dailyAllowance.isFinite() && attendance.dailyAllowance >= 0.0) {
                "Uang harian tidak valid."
            }
            if (attendance.project.isNotBlank()) require(projectDao.getProjectByName(attendance.project)?.isActive == true) {
                "Proyek absensi tidak terdaftar atau nonaktif."
            }
            require(attendanceDao.getAttendanceByEmployeeAndDate(attendance.employeeId, attendance.date) == null) {
                "Absensi " + attendance.employeeId + " pada " + attendance.date + " sudah ada."
            }
            attendanceDao.insertAttendance(attendance)
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "ATTENDANCE_LOG",
                recordId = attendance.id,
                details = "Absensi " + attendance.employeeName + " (" + attendance.status + ") pada " + attendance.date,
                user = "Admin Absensi",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    suspend fun deleteAttendance(id: String) {
        database.withTransaction {
            val attendance = attendanceDao.getAttendanceById(id)
                ?: throw IllegalArgumentException("Catatan absensi $id tidak ditemukan.")
            val overlappingPayroll = paidPayrollCoveringDate(attendance.date)
            require(overlappingPayroll == null) {
                "Absensi tanggal ${attendance.date} tidak dapat dihapus karena termasuk periode payroll ${overlappingPayroll?.receiptNo?.removePrefix("PAYROLL-")} yang sudah dicairkan. Catat koreksi sebagai transaksi penyesuaian."
            }
            attendanceDao.deleteAttendance(id)
            auditDao.insertAuditLog(AuditLogEntity(
                dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                action = "DELETE_ATTENDANCE",
                recordId = id,
                details = "Catatan absensi " + attendance.employeeName + " tanggal " + attendance.date + " dihapus sebelum pencairan payroll.",
                user = "Admin Absensi",
                verifiedFormulaStatus = "RECORDED"
            ))
        }
    }

    private fun isSettledPayrollTransaction(tx: TransactionEntity): Boolean =
        tx.category.equals("Gaji", ignoreCase = true) &&
            tx.status.equals("Selesai", ignoreCase = true) &&
            tx.receiptNo.trim().startsWith("PAYROLL-", ignoreCase = true)

    private fun payrollPeriodFromReceipt(receiptNo: String): String? {
        val normalized = receiptNo.trim()
        if (!normalized.startsWith("PAYROLL-", ignoreCase = true)) return null
        return normalized.substring("PAYROLL-".length).trim().takeIf { it.isNotBlank() }
    }

    /**
     * Returns a clear conflict message when a candidate payroll period overlaps a paid
     * period in the ledger or overlaps a different period represented in the same import.
     * Multiple ledger rows sharing the exact same period are allowed as payroll allocations.
     */
    private fun payrollPeriodConflict(
        candidatePeriods: Collection<String>,
        existingTransactions: List<TransactionEntity>
    ): String? {
        val candidates = candidatePeriods.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val candidateRanges = candidates.associateWith { payrollPeriodRange(it) }

        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val a = candidateRanges.getValue(candidates[i])
                val b = candidateRanges.getValue(candidates[j])
                if (a.first <= b.second && b.first <= a.second) {
                    return "Periode payroll ${candidates[i]} bertumpang tindih dengan ${candidates[j]} dalam file impor."
                }
            }
        }

        val existingPayrolls = existingTransactions.filter(::isSettledPayrollTransaction)
        for (candidate in candidates) {
            val requested = candidateRanges.getValue(candidate)
            for (prior in existingPayrolls) {
                val priorPeriod = payrollPeriodFromReceipt(prior.receiptNo)
                    ?: return "Bukti payroll lama ${prior.receiptNo} tidak memiliki periode yang valid; audit histori payroll sebelum melanjutkan."
                val priorRange = runCatching { payrollPeriodRange(priorPeriod) }.getOrElse {
                    return "Bukti payroll lama ${prior.receiptNo} memiliki periode yang tidak valid; audit histori payroll sebelum melanjutkan."
                }
                if (requested.first <= priorRange.second && priorRange.first <= requested.second) {
                    return "Periode payroll ${candidate} sudah dicairkan atau bertumpang tindih dengan payroll ${priorPeriod} yang sudah ada. Impor/pencairan dibatalkan untuk mencegah pembayaran ganda."
                }
            }
        }
        return null
    }

    private suspend fun paidPayrollCoveringDate(date: String): TransactionEntity? {
        val payrollTransactions = (transactionDao.getAllActiveTransactions().first() + transactionDao.getArchivedTransactions().first())
            .filter(::isSettledPayrollTransaction)
        return payrollTransactions.firstOrNull { payrollTx ->
            val paidPeriod = payrollPeriodFromReceipt(payrollTx.receiptNo)
            val range = paidPeriod?.let { runCatching { payrollPeriodRange(it) }.getOrNull() }
            range != null && date >= range.first && date <= range.second
        }
    }

    private fun payrollPeriodRange(period: String): Pair<String, String> {
        val rangeMatch = Regex("""^(\d{4}-\d{2}-\d{2})-(\d{4}-\d{2}-\d{2})$""").matchEntire(period)
        if (rangeMatch != null) {
            val start = rangeMatch.groupValues[1]
            val end = rangeMatch.groupValues[2]
            requireIsoDate(start, "Tanggal mulai periode payroll")
            requireIsoDate(end, "Tanggal akhir periode payroll")
            require(end >= start) { "Tanggal akhir payroll tidak boleh lebih awal dari tanggal mulai." }
            return start to end
        }
        if (period.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            requireIsoDate(period, "Tanggal payroll")
            return period to period
        }
        if (period.matches(Regex("""\d{4}-\d{2}"""))) {
            requireIsoDate("$period-01", "Periode payroll")
            return "$period-01" to periodEndDate(period)
        }
        if (period.matches(Regex("""\d{4}"""))) {
            val start = "$period-01-01"
            val end = "$period-12-31"
            requireIsoDate(start, "Tahun payroll")
            requireIsoDate(end, "Tahun payroll")
            return start to end
        }
        throw IllegalArgumentException("Periode payroll harus berupa tanggal, YYYY-MM, YYYY, atau rentang tanggal YYYY-MM-DD-YYYY-MM-DD.")
    }

    suspend fun savePayrollDisbursement(
        period: String,
        totalAmount: Double,
        accountName: String,
        allocationsByProject: Map<String, Double> = mapOf("" to totalAmount)
    ): String {
        require(totalAmount.isFinite() && totalAmount > 0.0) { "Total payroll harus lebih besar dari Rp 0." }
        require(period.isNotBlank()) { "Periode payroll wajib diisi." }
        require(allocationsByProject.isNotEmpty() && allocationsByProject.values.all { it.isFinite() && it >= 0.0 }) {
            "Alokasi payroll per proyek tidak valid."
        }
        require(kotlin.math.abs(allocationsByProject.values.sum() - totalAmount) < 0.01) {
            "Total alokasi payroll per proyek harus sama dengan total pencairan."
        }
        return database.withTransaction {
            require(accountDao.getAccountByName(accountName)?.isActive == true) {
                "Akun pembayaran " + accountName + " tidak terdaftar atau nonaktif."
            }
            val existingLedger = (
                transactionDao.getAllActiveTransactions().first() +
                    transactionDao.getArchivedTransactions().first()
                ).distinctBy { it.id }
            val conflict = payrollPeriodConflict(listOf(period), existingLedger)
            require(conflict == null) { conflict ?: "Payroll bertumpang tindih dengan periode yang sudah dibayar." }
            val now = Date()
            var firstId = ""
            allocationsByProject.toSortedMap().forEach { (projectName, amount) ->
                if (amount > 0.0) {
                    if (projectName.isNotBlank()) require(projectDao.getProjectByName(projectName) != null) {
                        "Proyek payroll " + projectName + " tidak terdaftar."
                    }
                    val txId = generateId("KK")
                    if (firstId.isBlank()) firstId = txId
                    val title = if (projectName.isBlank()) "Pembayaran Gaji PT ($period)" else "Pembayaran Gaji - $projectName ($period)"
                    val tx = TransactionEntity(
                        id = txId,
                        type = "KELUAR",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                        time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
                        account = accountName,
                        name = title,
                        category = "Gaji",
                        description = "Pencairan payroll berdasarkan rekap absensi periode $period; alokasi " + projectName.ifBlank { "PT/Umum" },
                        amount = amount,
                        allocation = "Gaji",
                        pic = "Bendahara",
                        receiptNo = "PAYROLL-$period",
                        status = "Selesai",
                        project = projectName,
                        fundBucket = "PT"
                    )
                    insertValidatedTransaction(tx, allowSystemReceiptNo = true)
                }
            }
            require(firstId.isNotBlank()) { "Tidak ada nilai payroll positif untuk dicairkan." }
            firstId
        }
    }


    // Bank Reconciliation Operations
    suspend fun saveBankReconciliation(recon: BankReconEntity): BankReconEntity {
        require(recon.id.isNotBlank()) { "ID rekonsiliasi wajib diisi." }
        require(recon.statementBalance.isFinite() && recon.statementBalance >= 0.0) { "Saldo rekening koran tidak valid." }
        require(recon.period.matches(Regex("""\d{4}-\d{2}"""))) { "Periode rekonsiliasi harus YYYY-MM." }
        requireIsoDate("${recon.period}-01", "Periode rekonsiliasi")
        require(recon.reconciledBy.isNotBlank()) { "PIC rekonsiliasi wajib diisi." }
        return database.withTransaction {
            require(accountDao.countByName(recon.accountName) == 1) {
                "Nama akun rekonsiliasi ${recon.accountName} tidak unik; rapikan master akun terlebih dahulu."
            }
            val account = accountDao.getAccountByName(recon.accountName)
                ?: throw IllegalArgumentException("Akun rekonsiliasi ${recon.accountName} tidak terdaftar.")
            require(account.isActive) { "Akun rekonsiliasi ${account.name} nonaktif." }
            val existing = bankReconDao.getReconByAccountAndPeriod(account.name, recon.period)
            val bookBalance = account.initialBalance +
                transactionDao.sumSettledAccountMovementUntilDate(account.name, periodEndDate(recon.period))
            val difference = recon.statementBalance - bookBalance
            val normalized = recon.copy(
                id = existing?.id ?: recon.id,
                accountName = account.name,
                bookBalance = bookBalance,
                difference = difference,
                status = if (kotlin.math.abs(difference) < 1.0) "Cocok" else "Selisih",
                reconciledAt = System.currentTimeMillis()
            )
            bankReconDao.insertReconciliation(normalized)
            val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            auditDao.insertAuditLog(
                AuditLogEntity(
                    dateFormatted = nowStamp,
                    action = if (existing == null) "BANK_RECONCILE" else "BANK_RECONCILE_UPDATE",
                    recordId = normalized.id,
                    details = "Rekonsiliasi ${normalized.accountName} periode ${normalized.period}: Buku Rp ${normalized.bookBalance.toLong()} vs Bank Rp ${normalized.statementBalance.toLong()} (Selisih: Rp ${normalized.difference.toLong()} - ${normalized.status})",
                    user = normalized.reconciledBy,
                    verifiedFormulaStatus = "RECORDED"
                )
            )
            normalized
        }
    }


    suspend fun logDirectAudit(audit: AuditLogEntity) {
        auditDao.insertAuditLog(audit)
    }

    fun generateId(prefix: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val uuid = UUID.randomUUID().toString().take(6).uppercase()
        return "$prefix-$stamp-$uuid"
    }

    fun calculateAccountBalanceAtPeriod(
        account: AccountEntity,
        period: String,
        transactions: List<TransactionEntity>
    ): Double {
        require(period.matches(Regex("""\d{4}-\d{2}"""))) { "Periode harus YYYY-MM." }
        val endDate = periodEndDate(period)
        return account.initialBalance + transactions.asSequence()
            .filter { it.status == "Selesai" && it.date <= endDate }
            .sumOf { tx ->
                when {
                    tx.type == "MASUK" && tx.account.equals(account.name, ignoreCase = true) -> tx.amount
                    tx.type == "KELUAR" && tx.account.equals(account.name, ignoreCase = true) -> -tx.amount
                    tx.type == "TRANSFER" && tx.toAccount?.equals(account.name, ignoreCase = true) == true -> tx.amount
                    tx.type == "TRANSFER" && tx.account.equals(account.name, ignoreCase = true) -> -tx.amount
                    else -> 0.0
                }
            }
    }

    private fun periodEndDate(period: String): String {
        val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault()).apply { isLenient = false }
        val parsed = parser.parse(period) ?: throw IllegalArgumentException("Periode tidak valid: $period")
        val calendar = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }


    fun calculateAccountBalances(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): List<AccountWithBalance> {
        val settledTx = transactions.filter { it.status == "Selesai" }
        val totalInByAccount = mutableMapOf<String, Double>()
        val totalOutByAccount = mutableMapOf<String, Double>()

        fun key(name: String): String = name.trim().lowercase(Locale.ROOT)
        settledTx.forEach { tx ->
            when (tx.type) {
                "MASUK" -> {
                    val k = key(tx.account)
                    totalInByAccount[k] = (totalInByAccount[k] ?: 0.0) + tx.amount
                }
                "KELUAR" -> {
                    val k = key(tx.account)
                    totalOutByAccount[k] = (totalOutByAccount[k] ?: 0.0) + tx.amount
                }
                "TRANSFER" -> {
                    val from = key(tx.account)
                    val to = key(tx.toAccount.orEmpty())
                    totalOutByAccount[from] = (totalOutByAccount[from] ?: 0.0) + tx.amount
                    if (to.isNotBlank()) totalInByAccount[to] = (totalInByAccount[to] ?: 0.0) + tx.amount
                }
            }
        }

        // Ledger rows currently store account names, not foreign keys. If legacy data
        // contains duplicate normalized names, applying the same movements to every matching
        // account double-counts the consolidated dashboard balance. Attribute those ambiguous
        // movements once, deterministically (active account first, then stable ID), and let the
        // integrity audit flag the ambiguous account master for manual cleanup.
        val movementOwnerByName = accounts
            .filter { it.name.isNotBlank() }
            .groupBy { key(it.name) }
            .mapValues { (_, group) ->
                group.sortedWith(compareByDescending<AccountEntity> { it.isActive }.thenBy { it.id })
                    .first().id
            }

        return accounts.map { account ->
            val k = key(account.name)
            val ownsMovement = movementOwnerByName[k] == account.id
            val totalMasuk = if (ownsMovement) totalInByAccount[k] ?: 0.0 else 0.0
            val totalKeluar = if (ownsMovement) totalOutByAccount[k] ?: 0.0 else 0.0
            AccountWithBalance(
                account = account,
                totalMasuk = totalMasuk,
                totalKeluar = totalKeluar,
                currentBalance = account.initialBalance + totalMasuk - totalKeluar
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
                        it.fundBucket.equals(budget.fundBucket, ignoreCase = true) &&
                        (if (budget.project.isBlank()) it.project.isBlank() else it.project.equals(budget.project, ignoreCase = true)) &&
                        it.allocation.equals(budget.category, ignoreCase = true)
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

    fun calculateReportAccountTotals(
        transactions: List<TransactionEntity>,
        selectedAccount: String
    ): Pair<Double, Double> {
        val consolidated = selectedAccount.equals("Semua", ignoreCase = true)
        val totalIn = transactions.sumOf { tx ->
            when {
                tx.type == "MASUK" &&
                    (consolidated || tx.account.equals(selectedAccount, ignoreCase = true)) -> tx.amount
                !consolidated && tx.type == "TRANSFER" &&
                    tx.toAccount?.equals(selectedAccount, ignoreCase = true) == true -> tx.amount
                else -> 0.0
            }
        }
        val totalOut = transactions.sumOf { tx ->
            when {
                tx.type == "KELUAR" &&
                    (consolidated || tx.account.equals(selectedAccount, ignoreCase = true)) -> tx.amount
                !consolidated && tx.type == "TRANSFER" &&
                    tx.account.equals(selectedAccount, ignoreCase = true) -> tx.amount
                else -> 0.0
            }
        }
        return totalIn to totalOut
    }

    fun calculateCashFlowStatement(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        startDate: String,
        endDate: String
    ): CashFlowStatement {
        requireIsoDate(startDate, "Tanggal mulai laporan arus kas")
        requireIsoDate(endDate, "Tanggal akhir laporan arus kas")
        require(startDate <= endDate) { "Tanggal mulai laporan arus kas tidak boleh melewati tanggal akhir." }
        val settledTx = transactions.filter { it.status == "Selesai" }
        val periodTx = settledTx.filter { it.date >= startDate && it.date <= endDate }

        fun isCapitalCategory(category: String): Boolean =
            category.trim().equals("Modal", ignoreCase = true) ||
                category.trim().equals("Investasi", ignoreCase = true)

        val opInflow = periodTx.filter {
            it.type == "MASUK" && !isCapitalCategory(it.category)
        }.sumOf { it.amount }

        // Project attribution is separate from cash-flow classification: project wages
        // remain operating expenses unless their allocation explicitly says "Proyek".
        val investingIds = periodTx.filter {
            it.type == "KELUAR" && it.allocation.equals("Proyek", ignoreCase = true)
        }.map { it.id }.toSet()

        val invOutflow = periodTx.filter { it.id in investingIds }.sumOf { it.amount }
        val opOutflow = periodTx.filter { it.type == "KELUAR" && it.id !in investingIds }.sumOf { it.amount }

        val finInflow = periodTx.filter {
            it.type == "MASUK" && isCapitalCategory(it.category)
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
    fun selectPayrollEligibleEmployees(
        allEmployees: List<EmployeeEntity>,
        attendances: List<AttendanceEntity>
    ): List<EmployeeEntity> {
        val employeesWithAttendance = attendances.mapTo(mutableSetOf()) { it.employeeId }
        return allEmployees.filter { it.isActive || it.id in employeesWithAttendance }
    }

    fun calculatePayrollForEmployee(
        employee: EmployeeEntity,
        attendances: List<AttendanceEntity>
    ): PayrollCalculation {
        val employeeAttendances = attendances.filter { it.employeeId == employee.id }
        val hadirDays = employeeAttendances.count { it.status.equals("Hadir", ignoreCase = true) }
        val alpaDays = employeeAttendances.count { it.status.equals("Alpa", ignoreCase = true) }
        val overtimeHours = employeeAttendances.sumOf { it.overtimeHours }
        val recordedAllowance = employeeAttendances.sumOf { it.dailyAllowance }
        val expectedAllowance = hadirDays * employee.dailyRate
        val attendanceAllowance = if (recordedAllowance > 0.0) recordedAllowance else expectedAllowance
        val overtimePay = overtimeHours * (employee.dailyRate / 8.0 * 1.5)
        val baseSalary = employee.monthlySalary
        val gross = baseSalary + attendanceAllowance + overtimePay
        val absenceDeduction = alpaDays * employee.dailyRate
        val totalDeduction = absenceDeduction
        val net = (gross - totalDeduction).coerceAtLeast(0.0)
        return PayrollCalculation(employee.id, baseSalary, attendanceAllowance, overtimePay, absenceDeduction, gross, totalDeduction, net)
    }

    fun calculatePayrollTotal(
        employees: List<EmployeeEntity>,
        attendances: List<AttendanceEntity>
    ): Double = employees.sumOf { calculatePayrollForEmployee(it, attendances).net }


    /**
     * Allocates net payroll to real bookkeeping projects:
     * - monthly base salary follows the employee's default project;
     * - attendance allowance and overtime follow the attendance project;
     * - blank attendance project falls back to the employee's default project;
     * - absence deductions reduce the default-project share first, then other positive shares.
     * The result is non-negative per project and sums to the existing payslip total.
     */
    fun calculatePayrollAllocations(
        employees: List<EmployeeEntity>,
        attendances: List<AttendanceEntity>
    ): Map<String, Double> {
        val totals = linkedMapOf<String, Double>()

        employees.forEach { employee ->
            val rows = attendances.filter { it.employeeId == employee.id }
            val payroll = calculatePayrollForEmployee(employee, rows)
            if (payroll.net <= 0.0) return@forEach

            val byProject = linkedMapOf<String, Double>()
            fun addCost(project: String, amount: Double) {
                if (amount.isFinite() && amount > 0.0) {
                    byProject[project.trim()] = (byProject[project.trim()] ?: 0.0) + amount
                }
            }

            addCost(employee.defaultProject, payroll.baseSalary)

            val recordedAllowance = rows.sumOf { it.dailyAllowance }
            if (recordedAllowance > 0.0) {
                rows.forEach { attendance ->
                    addCost(attendance.project.ifBlank { employee.defaultProject }, attendance.dailyAllowance)
                }
            } else {
                rows.filter { it.status.equals("Hadir", ignoreCase = true) }.forEach { attendance ->
                    addCost(attendance.project.ifBlank { employee.defaultProject }, employee.dailyRate)
                }
            }

            rows.forEach { attendance ->
                if (attendance.overtimeHours > 0.0) {
                    addCost(
                        attendance.project.ifBlank { employee.defaultProject },
                        attendance.overtimeHours * (employee.dailyRate / 8.0 * 1.5)
                    )
                }
            }

            var remainingDeduction = payroll.absenceDeduction
            val preferredProject = employee.defaultProject.trim()
            val preferredAmount = byProject[preferredProject] ?: 0.0
            val preferredDeduction = minOf(preferredAmount, remainingDeduction)
            if (preferredDeduction > 0.0) {
                byProject[preferredProject] = preferredAmount - preferredDeduction
                remainingDeduction -= preferredDeduction
            }

            if (remainingDeduction > 0.0) {
                byProject.keys.toList().filter { it != preferredProject }.forEach { project ->
                    if (remainingDeduction > 0.0) {
                        val available = byProject[project] ?: 0.0
                        val deduction = minOf(available, remainingDeduction)
                        byProject[project] = available - deduction
                        remainingDeduction -= deduction
                    }
                }
            }

            val nonZero = byProject.filterValues { it > 0.0 }
            val allocatedNet = nonZero.values.sum()
            val difference = payroll.net - allocatedNet
            val corrected = nonZero.toMutableMap()
            if (kotlin.math.abs(difference) >= 0.005) {
                val correctionProject = preferredProject.takeIf { (corrected[it] ?: 0.0) > 0.0 }
                    ?: corrected.keys.firstOrNull()
                if (correctionProject != null) {
                    corrected[correctionProject] = ((corrected[correctionProject] ?: 0.0) + difference).coerceAtLeast(0.0)
                }
            }

            corrected.filterValues { it > 0.0 }.forEach { (project, amount) ->
                totals[project] = (totals[project] ?: 0.0) + amount
            }
        }

        return totals.filterValues { it > 0.0 }
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

    suspend fun runCurrentIntegrityAudit(): IntegrityAuditSnapshot {
        val accountsSnapshot = accountDao.getAllAccounts().first()
        val transactionsSnapshot = (
            transactionDao.getAllActiveTransactions().first() +
                transactionDao.getArchivedTransactions().first()
            ).distinctBy { it.id }
        val budgetsSnapshot = budgetDao.getAllBudgets().first()
        val receivablesSnapshot = receivableDao.getAllReceivables().first()
        val employeesSnapshot = employeeDao.getAllEmployees().first()
        val attendancesSnapshot = attendanceDao.getAllAttendances().first()
        val projectsSnapshot = projectDao.getAllProjects().first()
        val projectPlansSnapshot = projectPlanDao.getAllPlans().first()
        val housingUnitsSnapshot = housingUnitDao.getAllUnits().first()
        val notesSnapshot = noteDao.getAllNotes().first()
        val reconciliationsSnapshot = bankReconDao.getAllReconciliations().first()

        val result = runIntegrityAudit(
            accounts = accountsSnapshot,
            transactions = transactionsSnapshot,
            budgets = budgetsSnapshot,
            receivables = receivablesSnapshot,
            employees = employeesSnapshot,
            attendances = attendancesSnapshot,
            projects = projectsSnapshot,
            projectPlans = projectPlansSnapshot,
            housingUnits = housingUnitsSnapshot,
            notes = notesSnapshot,
            bankReconciliations = reconciliationsSnapshot
        )
        val totalBalance = calculateAccountBalances(accountsSnapshot, transactionsSnapshot).sumOf { it.currentBalance }
        return IntegrityAuditSnapshot(result, totalBalance, transactionsSnapshot.size)
    }

    fun runIntegrityAudit(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receivables: List<ReceivableEntity>,
        employees: List<EmployeeEntity>,
        attendances: List<AttendanceEntity>,
        projects: List<ProjectEntity>? = null,
        projectPlans: List<ProjectPlanEntity> = emptyList(),
        housingUnits: List<HousingUnitEntity> = emptyList(),
        notes: List<CashNoteEntity> = emptyList(),
        bankReconciliations: List<BankReconEntity> = emptyList()
    ): IntegrityAuditResult {
        val issues = mutableListOf<String>()
        val checkedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        fun key(value: String): String = value.trim().lowercase(Locale.ROOT)
        val validFundBuckets = setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")
        val validTypes = setOf("MASUK", "KELUAR", "TRANSFER")
        val validStatuses = setOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")
        val projectRows = projects.orEmpty()
        val projectKeys = projectRows.map { key(it.name) }
        val accountNames = accounts.map { key(it.name) }.toSet()
        val employeeIds = employees.map { it.id }.toSet()

        accounts.groupBy { key(it.name) }.filter { it.key.isNotBlank() && it.value.size > 1 }.values.forEach { duplicates ->
            issues += "Nama akun duplikat: ${duplicates.joinToString(" / ") { it.name }}. Saldo berbasis nama belum dapat dipastikan."
        }
        accounts.forEach { account ->
            if (account.id.isBlank()) issues += "Akun tanpa ID ditemukan."
            if (account.name.isBlank()) issues += "Akun ${account.id}: nama kosong."
            if (!account.initialBalance.isFinite() || account.initialBalance < 0.0) issues += "Akun ${account.id}: saldo awal tidak valid."
            if (account.type !in setOf("Kas", "Bank", "E-Wallet", "Lainnya")) issues += "Akun ${account.id}: jenis tidak valid."
        }

        if (projects != null && projectKeys.distinct().size != projectKeys.size) {
            issues += "Nama proyek duplikat; hubungan transaksi, unit, anggaran, dan kalender ambigu."
        }
        if (accounts.map { it.id }.distinct().size != accounts.size) issues += "ID akun duplikat dalam snapshot."
        if (budgets.filter { it.id != 0L }.map { it.id }.distinct().size != budgets.count { it.id != 0L }) issues += "ID anggaran duplikat dalam snapshot."
        if (notes.filter { it.id != 0L }.map { it.id }.distinct().size != notes.count { it.id != 0L }) issues += "ID catatan duplikat dalam snapshot."
        if (projectRows.map { it.id }.distinct().size != projectRows.size) issues += "ID proyek duplikat dalam snapshot."
        if (projectPlans.map { it.id }.distinct().size != projectPlans.size) issues += "ID rencana kalender duplikat dalam snapshot."
        if (housingUnits.map { it.id }.distinct().size != housingUnits.size) issues += "ID unit perumahan duplikat dalam snapshot."
        if (bankReconciliations.map { it.id }.distinct().size != bankReconciliations.size) issues += "ID rekonsiliasi duplikat dalam snapshot."
        projectRows.forEach { project ->
            if (project.id.isBlank() || project.name.isBlank()) issues += "Master proyek memiliki ID/nama kosong."
            if (project.category !in setOf("Pembebasan Tanah", "Cut & Fill", "Perumahan", "Perdagangan", "Operasional PT", "Lainnya")) {
                issues += "Proyek ${project.id}: kategori tidak valid."
            }
            if (project.businessModel !in setOf("Subsidi", "Komersial", "Tidak berlaku") ||
                (project.category != "Perumahan" && project.businessModel != "Tidak berlaku")) {
                issues += "Proyek ${project.id}: model bisnis tidak sesuai kategori."
            }
            if (!runCatching { requireIsoDate(project.startDate, "Tanggal mulai proyek") }.isSuccess ||
                !runCatching { requireIsoDate(project.targetEndDate, "Target proyek") }.isSuccess ||
                project.targetEndDate < project.startDate) {
                issues += "Proyek ${project.id}: tanggal/jadwal tidak valid."
            }
            if (!project.budgetAmount.isFinite() || project.budgetAmount < 0.0) issues += "Proyek ${project.id}: pagu tidak valid."
            if (project.status !in setOf("Berjalan", "Ditunda", "Selesai")) issues += "Proyek ${project.id}: status tidak valid."
        }

        val duplicateTransactionIds = transactions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        duplicateTransactionIds.forEach { issues += "ID transaksi duplikat dalam snapshot: $it." }
        val payrollPeriods = mutableListOf<Pair<String, Pair<String, String>>>()
        transactions.forEach { tx ->
            if (tx.id.isBlank()) issues += "Transaksi tanpa ID ditemukan."
            if (!tx.amount.isFinite() || tx.amount <= 0.0) issues += "Transaksi ${tx.id}: nominal tidak valid."
            if (tx.type !in validTypes) issues += "Transaksi ${tx.id}: tipe ${tx.type} tidak valid."
            if (tx.status !in validStatuses) issues += "Transaksi ${tx.id}: status ${tx.status} tidak valid."
            if (tx.name.isBlank() || tx.category.isBlank() || tx.allocation.isBlank() || tx.pic.isBlank() || tx.inputBy.isBlank()) {
                issues += "Transaksi ${tx.id}: nama/kategori/alokasi/PIC/input wajib tidak lengkap."
            }
            if (!runCatching { requireIsoDate(tx.date, "Tanggal transaksi") }.isSuccess) issues += "Transaksi ${tx.id}: tanggal tidak valid."
            if (!runCatching { requireIsoTime(tx.time, "Jam transaksi") }.isSuccess) issues += "Transaksi ${tx.id}: jam tidak valid."
            if (tx.inputTime <= 0L) issues += "Transaksi ${tx.id}: waktu input tidak valid."
            if (tx.fundBucket !in validFundBuckets) issues += "Transaksi ${tx.id}: kelompok dana tidak valid."
            if (tx.isArchived && (tx.archivedAt == null || tx.archivedAt <= 0L || tx.archivedBy.isNullOrBlank())) {
                issues += "Transaksi arsip ${tx.id}: metadata arsip tidak lengkap."
            }
            if (!tx.isArchived && (tx.archivedAt != null || tx.archivedBy != null)) {
                issues += "Transaksi aktif ${tx.id}: metadata arsip seharusnya kosong."
            }
            if (tx.account.trim().lowercase(Locale.ROOT) !in accountNames) issues += "Transaksi ${tx.id}: akun ${tx.account} tidak terdaftar."
            if (tx.project.isNotBlank() && projects != null && projectKeys.none { it == key(tx.project) }) {
                issues += "Transaksi ${tx.id}: proyek ${tx.project} tidak terdaftar."
            }
            if (tx.type == "MASUK" && tx.category.equals("Transfer Masuk", ignoreCase = true)) {
                issues += "Transaksi ${tx.id}: kategori Transfer Masuk tercatat sebagai pemasukan; koreksi ledger legacy."
            }
            if (tx.type == "TRANSFER") {
                val destination = tx.toAccount?.trim().orEmpty()
                if (destination.isBlank()) issues += "Transfer ${tx.id}: akun tujuan kosong."
                else if (key(destination) !in accountNames) issues += "Transfer ${tx.id}: akun tujuan ${destination} tidak terdaftar."
                else if (destination.equals(tx.account, ignoreCase = true)) issues += "Transfer ${tx.id}: akun asal sama dengan tujuan."
            } else if (tx.toAccount != null) {
                issues += "Transaksi ${tx.id}: akun tujuan terisi padahal bukan transfer."
            }

            val receipt = tx.receiptNo.trim()
            when {
                receipt.startsWith("PAYROLL-", ignoreCase = true) -> {
                    if (tx.type != "KELUAR" || !tx.category.equals("Gaji", ignoreCase = true) || tx.status != "Selesai") {
                        issues += "Transaksi payroll ${tx.id}: tipe/kategori/status harus KELUAR/Gaji/Selesai."
                    }
                    val period = payrollPeriodFromReceipt(receipt)
                    val range = period?.let { runCatching { payrollPeriodRange(it) }.getOrNull() }
                    if (range == null) issues += "Transaksi payroll ${tx.id}: periode bukti tidak valid."
                    else if (tx.type == "KELUAR" && tx.category.equals("Gaji", ignoreCase = true) && tx.status == "Selesai") {
                        payrollPeriods.add(period!! to range)
                    }
                }
                receipt.startsWith("PIU-", ignoreCase = true) -> {
                    if (tx.type != "MASUK" || !tx.category.equals("Piutang Masuk", ignoreCase = true) || tx.status != "Selesai") {
                        issues += "Pembayaran PIUTANG pada transaksi ${tx.id} tidak valid."
                    }
                }
                receipt.startsWith("HUT-", ignoreCase = true) -> {
                    if (tx.type != "KELUAR" || !tx.category.equals("Belanja Barang", ignoreCase = true) || tx.status != "Selesai") {
                        issues += "Pembayaran HUTANG pada transaksi ${tx.id} tidak valid."
                    }
                }
            }
        }
        val distinctPayrollPeriods = payrollPeriods.distinctBy { it.first.lowercase(Locale.ROOT) }
        for (i in distinctPayrollPeriods.indices) {
            for (j in i + 1 until distinctPayrollPeriods.size) {
                val currentPeriod = distinctPayrollPeriods[i]
                val otherPeriod = distinctPayrollPeriods[j]
                if (currentPeriod.first != otherPeriod.first &&
                    currentPeriod.second.first <= otherPeriod.second.second &&
                    otherPeriod.second.first <= currentPeriod.second.second
                ) {
                    issues += "Periode payroll ${currentPeriod.first} bertumpang tindih dengan ${otherPeriod.first} pada ledger."
                }
            }
        }

        val budgetScopeKeys = budgets.map {
            listOf(it.period, it.category, it.project, it.fundBucket).joinToString("|") { value -> key(value) }
        }
        if (budgetScopeKeys.distinct().size != budgetScopeKeys.size) {
            issues += "Ada cakupan anggaran duplikat (periode/kategori/proyek/kelompok dana)."
        }
        budgets.forEach { budget ->
            if (!budget.budgetAmount.isFinite() || budget.budgetAmount < 0.0) issues += "Anggaran ${budget.id}: nominal pagu tidak valid."
            if (!budget.period.equals("All", ignoreCase = true) && !runCatching { periodEndDate(budget.period) }.isSuccess) {
                issues += "Anggaran ${budget.id}: periode tidak valid."
            }
            if (budget.category.isBlank()) issues += "Anggaran ${budget.id}: kategori kosong."
            if (budget.fundBucket !in validFundBuckets) issues += "Anggaran ${budget.id}: kelompok dana tidak valid."
            if (budget.project.isNotBlank() && projects != null && projectKeys.none { it == key(budget.project) }) {
                issues += "Anggaran ${budget.id}: proyek ${budget.project} tidak terdaftar."
            }
        }

        val duplicateReceivableIds = receivables.groupBy { it.id }.filterValues { it.size > 1 }.keys
        duplicateReceivableIds.forEach { issues += "ID tagihan duplikat: $it." }
        receivables.forEach { item ->
            if (item.type !in setOf("PIUTANG", "HUTANG")) issues += "Tagihan ${item.id}: jenis tidak valid."
            if (!runCatching { requireIsoDate(item.date, "Tanggal tagihan") }.isSuccess) issues += "Tagihan ${item.id}: tanggal pencatatan tidak valid."
            if (!runCatching { requireIsoDate(item.dueDate, "Jatuh tempo") }.isSuccess) issues += "Tagihan ${item.id}: tanggal jatuh tempo tidak valid."
            if (item.status !in setOf("Belum Jatuh Tempo", "Jatuh Tempo", "Lunas")) issues += "Tagihan ${item.id}: status tidak valid."
            if (!item.totalAmount.isFinite() || item.totalAmount <= 0.0) issues += "${item.type} ${item.id}: total nominal tidak valid."
            if (!item.paidAmount.isFinite() || item.paidAmount < 0.0 || item.paidAmount > item.totalAmount) issues += "${item.type} ${item.id}: paidAmount tidak valid."
            if (key(item.targetAccount) !in accountNames) issues += "${item.type} ${item.id}: akun terkait tidak terdaftar."
            if (item.fundBucket !in validFundBuckets) issues += "${item.type} ${item.id}: kelompok dana tidak valid."
            if (item.project.isNotBlank() && projects != null && projectKeys.none { it == key(item.project) }) {
                issues += "${item.type} ${item.id}: proyek ${item.project} tidak terdaftar."
            }

            val expectedReceiptPrefix = if (item.type == "PIUTANG") "PIU-" else "HUT-"
            val expectedCategory = if (item.type == "PIUTANG") "Piutang Masuk" else "Belanja Barang"
            val expectedType = if (item.type == "PIUTANG") "MASUK" else "KELUAR"
            val linkedLedgerTotal = transactions.filter {
                it.receiptNo.equals(expectedReceiptPrefix + item.id, ignoreCase = true) &&
                    it.category.equals(expectedCategory, ignoreCase = true) &&
                    it.type == expectedType &&
                    it.status.equals("Selesai", ignoreCase = true)
            }.sumOf { it.amount }
            if (item.paidAmount.isFinite() && kotlin.math.abs(linkedLedgerTotal - item.paidAmount) > 0.01) {
                issues += "${item.type} ${item.id}: pembayaran pada master (Rp ${item.paidAmount.toLong()}) tidak sama dengan ledger tertaut (Rp ${linkedLedgerTotal.toLong()})."
            }
        }

        val duplicateEmployeeIds = employees.groupBy { it.id }.filterValues { it.size > 1 }.keys
        duplicateEmployeeIds.forEach { issues += "ID karyawan duplikat: $it." }
        employees.forEach { employee ->
            if (employee.id.isBlank() || employee.name.isBlank()) issues += "Data karyawan memiliki ID/nama kosong."
            if (!employee.dailyRate.isFinite() || employee.dailyRate < 0.0 ||
                !employee.monthlySalary.isFinite() || employee.monthlySalary < 0.0) {
                issues += "Karyawan ${employee.id}: nominal gaji tidak valid."
            }
            if (employee.defaultProject.isNotBlank() && projects != null && projectKeys.none { it == key(employee.defaultProject) }) {
                issues += "Karyawan ${employee.id}: proyek alokasi ${employee.defaultProject} tidak terdaftar."
            }
        }

        val duplicateAttendanceKeys = attendances.groupBy { key(it.employeeId) + "|" + it.date }.filterValues { it.size > 1 }.keys
        duplicateAttendanceKeys.forEach { issues += "Absensi ganda untuk karyawan/tanggal: $it." }
        attendances.forEach { att ->
            if (att.employeeId !in employeeIds) issues += "Absensi ${att.id}: karyawan ${att.employeeId} tidak terdaftar."
            if (!runCatching { requireIsoDate(att.date, "Tanggal absensi") }.isSuccess) issues += "Absensi ${att.id}: tanggal tidak valid."
            if (att.status !in setOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti")) issues += "Absensi ${att.id}: status tidak valid."
            if (!att.overtimeHours.isFinite() || att.overtimeHours < 0.0 ||
                !att.dailyAllowance.isFinite() || att.dailyAllowance < 0.0) issues += "Absensi ${att.id}: nilai lembur/tunjangan tidak valid."
            if (att.project.isNotBlank() && projects != null && projectKeys.none { it == key(att.project) }) {
                issues += "Absensi ${att.id}: proyek ${att.project} tidak terdaftar."
            }
        }

        val unitKeys = housingUnits.map { key(it.project) + "|" + key(it.unitCode) }
        if (unitKeys.distinct().size != unitKeys.size) issues += "Kode unit duplikat dalam proyek."
        housingUnits.forEach { unit ->
            val project = projectRows.firstOrNull { key(it.name) == key(unit.project) }
            if (unit.id.isBlank() || unit.project.isBlank() || unit.unitCode.isBlank()) issues += "Unit ${unit.id}: ID/proyek/kode kosong."
            if (!unit.landAreaM2.isFinite() || unit.landAreaM2 <= 0.0 ||
                !unit.buildingAreaM2.isFinite() || unit.buildingAreaM2 < 0.0 ||
                !unit.salePrice.isFinite() || unit.salePrice <= 0.0) issues += "Unit ${unit.id}: luas/harga tidak valid."
            if (unit.businessModel !in setOf("Subsidi", "Komersial")) issues += "Unit ${unit.id}: segmen tidak valid."
            if (unit.status !in setOf("Tersedia", "Booking", "Terjual", "Dibatalkan")) issues += "Unit ${unit.id}: status tidak valid."
            if (unit.status == "Terjual" && unit.buyerName.isBlank()) issues += "Unit ${unit.id}: nama pembeli kosong."
            if (projects != null && (project == null || project.category != "Perumahan" || project.businessModel != unit.businessModel)) {
                issues += "Unit ${unit.id}: proyek tidak cocok dengan kategori/segmen perumahan."
            }
        }

        projectPlans.forEach { plan ->
            if (plan.id.isBlank() || plan.title.isBlank()) issues += "Rencana kalender ${plan.id}: ID/judul kosong."
            if (!runCatching { requireIsoDate(plan.planDate, "Tanggal rencana") }.isSuccess) issues += "Rencana kalender ${plan.id}: tanggal tidak valid."
            if (!plan.estimatedAmount.isFinite() || plan.estimatedAmount < 0.0) issues += "Rencana kalender ${plan.id}: estimasi tidak valid."
            if (plan.status !in setOf("Direncanakan", "Selesai", "Batal")) issues += "Rencana kalender ${plan.id}: status tidak valid."
            if (plan.project.isNotBlank() && projects != null && projectKeys.none { it == key(plan.project) }) {
                issues += "Rencana kalender ${plan.id}: proyek ${plan.project} tidak terdaftar."
            }
        }
        notes.forEach { note ->
            if (note.title.isBlank()) issues += "Catatan ${note.id}: judul kosong."
            if (!runCatching { requireIsoDate(note.date, "Tanggal catatan") }.isSuccess) issues += "Catatan ${note.id}: tanggal tidak valid."
            if (note.priority !in setOf("Rendah", "Sedang", "Tinggi")) issues += "Catatan ${note.id}: prioritas tidak valid."
            if (note.status !in setOf("Open", "Done", "Follow Up")) issues += "Catatan ${note.id}: status tidak valid."
            if (note.project.isNotBlank() && projects != null && projectKeys.none { it == key(note.project) }) {
                issues += "Catatan ${note.id}: proyek ${note.project} tidak terdaftar."
            }
        }

        val reconciliationKeys = bankReconciliations.map { key(it.accountName) + "|" + it.period }
        if (reconciliationKeys.distinct().size != reconciliationKeys.size) issues += "Rekonsiliasi duplikat untuk akun/periode."
        bankReconciliations.forEach { recon ->
            val account = accounts.firstOrNull { key(it.name) == key(recon.accountName) }
            if (account == null) issues += "Rekonsiliasi ${recon.id}: akun ${recon.accountName} tidak terdaftar."
            if (!recon.period.matches(Regex("""\d{4}-\d{2}""")) || !runCatching { periodEndDate(recon.period) }.isSuccess) {
                issues += "Rekonsiliasi ${recon.id}: periode tidak valid."
            } else if (!recon.statementBalance.isFinite() || recon.statementBalance < 0.0 ||
                !recon.bookBalance.isFinite() || !recon.difference.isFinite()) {
                issues += "Rekonsiliasi ${recon.id}: saldo/selisih tidak valid."
            } else if (account != null) {
                val expectedBook = runCatching { calculateAccountBalanceAtPeriod(account, recon.period, transactions) }.getOrNull()
                if (expectedBook == null) {
                    issues += "Rekonsiliasi ${recon.id}: saldo buku pada periode tidak dapat dihitung."
                } else {
                    val expectedDifference = recon.statementBalance - expectedBook
                    if (kotlin.math.abs(recon.bookBalance - expectedBook) > 0.01) {
                        issues += "Rekonsiliasi ${recon.id}: saldo buku tersimpan tidak sama dengan ledger pada periode."
                    }
                    if (kotlin.math.abs(recon.difference - expectedDifference) > 0.01) {
                        issues += "Rekonsiliasi ${recon.id}: selisih tidak sama dengan saldo rekening koran dikurangi saldo buku."
                    }
                    val expectedStatus = if (kotlin.math.abs(expectedDifference) < 1.0) "Cocok" else "Selisih"
                    if (recon.status != expectedStatus) issues += "Rekonsiliasi ${recon.id}: status tidak sesuai hasil perhitungan."
                }
            }
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

    suspend fun exportDataToJson(): String = database.withTransaction {
        val transactions = transactionDao.getAllActiveTransactions().first()
        val accounts = accountDao.getAllAccounts().first()
        val budgets = budgetDao.getAllBudgets().first()
        val receivables = receivableDao.getAllReceivables().first()
        val notes = noteDao.getAllNotes().first()
        val root = JSONObject()
        root.put("app", "Sistem Kas")
        root.put("version", "6.0")
        root.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val txArray = JSONArray()
        val allTransactions = (transactions + transactionDao.getArchivedTransactions().first()).distinctBy { it.id }
        allTransactions.forEach { tx ->
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
                put("fundBucket", tx.fundBucket)
                put("note", tx.note)
                put("status", tx.status)
                put("inputTime", tx.inputTime)
                put("inputBy", tx.inputBy)
                put("isArchived", tx.isArchived)
                put("archivedAt", tx.archivedAt ?: JSONObject.NULL)
                put("archivedBy", tx.archivedBy ?: JSONObject.NULL)
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
                put("isActive", acc.isActive)
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
                put("project", budget.project)
                put("fundBucket", budget.fundBucket)
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
                put("project", item.project)
                put("fundBucket", item.fundBucket)
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
                put("project", note.project)
            })
        }
        root.put("notes", noteArray)

        val employeeArray = JSONArray()
        employeeDao.getAllEmployees().first().forEach { employee ->
            employeeArray.put(JSONObject().apply {
                put("id", employee.id)
                put("name", employee.name)
                put("position", employee.position)
                put("department", employee.department)
                put("phone", employee.phone)
                put("dailyRate", employee.dailyRate)
                put("monthlySalary", employee.monthlySalary)
                put("isActive", employee.isActive)
                put("defaultProject", employee.defaultProject)
            })
        }
        root.put("employees", employeeArray)

        val attendanceArray = JSONArray()
        attendanceDao.getAllAttendances().first().forEach { attendance ->
            attendanceArray.put(JSONObject().apply {
                put("id", attendance.id)
                put("employeeId", attendance.employeeId)
                put("employeeName", attendance.employeeName)
                put("department", attendance.department)
                put("date", attendance.date)
                put("timeIn", attendance.timeIn)
                put("timeOut", attendance.timeOut)
                put("status", attendance.status)
                put("overtimeHours", attendance.overtimeHours)
                put("dailyAllowance", attendance.dailyAllowance)
                put("notes", attendance.notes)
                put("project", attendance.project)
            })
        }
        root.put("attendances", attendanceArray)

        val auditArray = JSONArray()
        auditDao.getAllAuditLogs().forEach { audit ->
            auditArray.put(JSONObject().apply {
                put("id", audit.id)
                put("timestamp", audit.timestamp)
                put("dateFormatted", audit.dateFormatted)
                put("action", audit.action)
                put("recordId", audit.recordId)
                put("details", audit.details)
                put("user", audit.user)
                put("verifiedFormulaStatus", audit.verifiedFormulaStatus)
                put("balanceAfter", audit.balanceAfter)
            })
        }
        root.put("auditLogs", auditArray)

        val reconArray = JSONArray()
        bankReconDao.getAllReconciliations().first().forEach { recon ->
            reconArray.put(JSONObject().apply {
                put("id", recon.id)
                put("accountName", recon.accountName)
                put("period", recon.period)
                put("bookBalance", recon.bookBalance)
                put("statementBalance", recon.statementBalance)
                put("difference", recon.difference)
                put("status", recon.status)
                put("reconciledBy", recon.reconciledBy)
                put("reconciledAt", recon.reconciledAt)
                put("notes", recon.notes)
            })
        }
        root.put("bankReconciliations", reconArray)


        val projectsArray = JSONArray()
        projectDao.getAllProjects().first().forEach { project ->
            projectsArray.put(JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("category", project.category)
                put("businessModel", project.businessModel)
                put("location", project.location)
                put("startDate", project.startDate)
                put("targetEndDate", project.targetEndDate)
                put("budgetAmount", project.budgetAmount)
                put("status", project.status)
                put("notes", project.notes)
                put("isActive", project.isActive)
                put("createdAt", project.createdAt)
            })
        }
        root.put("projects", projectsArray)

        val plansArray = JSONArray()
        projectPlanDao.getAllPlans().first().forEach { plan ->
            plansArray.put(JSONObject().apply {
                put("id", plan.id)
                put("project", plan.project)
                put("planDate", plan.planDate)
                put("title", plan.title)
                put("category", plan.category)
                put("estimatedAmount", plan.estimatedAmount)
                put("status", plan.status)
                put("details", plan.details)
                put("createdAt", plan.createdAt)
            })
        }
        root.put("projectPlans", plansArray)

        val housingUnitsArray = JSONArray()
        housingUnitDao.getAllUnits().first().forEach { unit ->
            housingUnitsArray.put(JSONObject().apply {
                put("id", unit.id)
                put("project", unit.project)
                put("unitCode", unit.unitCode)
                put("block", unit.block)
                put("sitePosition", unit.sitePosition)
                put("businessModel", unit.businessModel)
                put("landAreaM2", unit.landAreaM2)
                put("buildingAreaM2", unit.buildingAreaM2)
                put("salePrice", unit.salePrice)
                put("buyerName", unit.buyerName)
                put("status", unit.status)
                put("notes", unit.notes)
                put("createdAt", unit.createdAt)
            })
        }
        root.put("housingUnits", housingUnitsArray)

        root.toString(2)
    }

    suspend fun restoreDataFromJson(jsonStr: String): Result<Int> {
        return try {
            require(jsonStr.isNotBlank()) { "File backup JSON kosong." }
            val root = JSONObject(jsonStr)
            require(root.has("accounts") && root.get("accounts") is JSONArray) {
                "Backup tidak memiliki array accounts yang valid."
            }
            require(root.has("transactions") && root.get("transactions") is JSONArray) {
                "Backup tidak memiliki array transactions yang valid."
            }

            val accounts = mutableListOf<AccountEntity>()
            if (root.has("accounts")) {
                val array = root.getJSONArray("accounts")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    accounts += AccountEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        type = o.getString("type"),
                        initialBalance = o.getDouble("initialBalance"),
                        colorHex = o.getString("colorHex"),
                        isActive = o.optBoolean("isActive", true)
                    )
                }
            }

            val budgets = mutableListOf<BudgetEntity>()
            if (root.has("budgets")) {
                val array = root.getJSONArray("budgets")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    budgets += BudgetEntity(
                        id = o.getLong("id"),
                        period = o.getString("period"),
                        category = o.getString("category"),
                        budgetAmount = o.getDouble("budgetAmount"),
                        notes = o.getString("notes"),
                        project = o.optString("project", ""),
                        fundBucket = o.optString("fundBucket", "PT")
                    )
                }
            }

            val receivables = mutableListOf<ReceivableEntity>()
            if (root.has("receivables")) {
                val array = root.getJSONArray("receivables")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    receivables += ReceivableEntity(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        date = o.getString("date"),
                        customerName = o.getString("customerName"),
                        description = o.getString("description"),
                        totalAmount = o.getDouble("totalAmount"),
                        dueDate = o.getString("dueDate"),
                        paidAmount = o.getDouble("paidAmount"),
                        targetAccount = o.getString("targetAccount"),
                        notes = o.getString("notes"),
                        status = o.getString("status"),
                        project = o.optString("project", ""),
                        fundBucket = o.optString("fundBucket", "PT")
                    )
                }
            }

            val notes = mutableListOf<CashNoteEntity>()
            if (root.has("notes")) {
                val array = root.getJSONArray("notes")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    notes += CashNoteEntity(
                        id = o.getLong("id"),
                        date = o.getString("date"),
                        title = o.getString("title"),
                        content = o.getString("content"),
                        pic = o.getString("pic"),
                        priority = o.getString("priority"),
                        status = o.getString("status"),
                        project = o.optString("project", "")
                    )
                }
            }

            val employees = mutableListOf<EmployeeEntity>()
            if (root.has("employees")) {
                val array = root.getJSONArray("employees")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    employees += EmployeeEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        position = o.getString("position"),
                        department = o.getString("department"),
                        phone = o.getString("phone"),
                        dailyRate = o.getDouble("dailyRate"),
                        monthlySalary = o.getDouble("monthlySalary"),
                        isActive = o.getBoolean("isActive"),
                        defaultProject = o.optString("defaultProject", "")
                    )
                }
            }

            val attendances = mutableListOf<AttendanceEntity>()
            if (root.has("attendances")) {
                val array = root.getJSONArray("attendances")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    attendances += AttendanceEntity(
                        id = o.getString("id"),
                        employeeId = o.getString("employeeId"),
                        employeeName = o.getString("employeeName"),
                        department = o.getString("department"),
                        date = o.getString("date"),
                        timeIn = o.getString("timeIn"),
                        timeOut = o.getString("timeOut"),
                        status = o.getString("status"),
                        overtimeHours = o.getDouble("overtimeHours"),
                        dailyAllowance = o.getDouble("dailyAllowance"),
                        notes = o.getString("notes"),
                        project = o.optString("project", "")
                    )
                }
            }

            val recons = mutableListOf<BankReconEntity>()
            if (root.has("bankReconciliations")) {
                val array = root.getJSONArray("bankReconciliations")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    recons += BankReconEntity(
                        id = o.getString("id"),
                        accountName = o.getString("accountName"),
                        period = o.getString("period"),
                        bookBalance = o.getDouble("bookBalance"),
                        statementBalance = o.getDouble("statementBalance"),
                        difference = o.getDouble("difference"),
                        status = o.getString("status"),
                        reconciledBy = o.getString("reconciledBy"),
                        reconciledAt = o.getLong("reconciledAt"),
                        notes = o.getString("notes")
                    )
                }
            }

            val audits = mutableListOf<AuditLogEntity>()
            if (root.has("auditLogs")) {
                val array = root.getJSONArray("auditLogs")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    audits += AuditLogEntity(
                        id = o.getLong("id"),
                        timestamp = o.getLong("timestamp"),
                        dateFormatted = o.getString("dateFormatted"),
                        action = o.getString("action"),
                        recordId = o.getString("recordId"),
                        details = o.getString("details"),
                        user = o.getString("user"),
                        verifiedFormulaStatus = o.getString("verifiedFormulaStatus"),
                        balanceAfter = o.getDouble("balanceAfter")
                    )
                }
            }


            val projects = mutableListOf<ProjectEntity>()
            if (root.has("projects")) {
                val array = root.getJSONArray("projects")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    projects += ProjectEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        businessModel = o.optString("businessModel", "Tidak berlaku"),
                        location = o.optString("location", ""),
                        startDate = o.getString("startDate"),
                        targetEndDate = o.getString("targetEndDate"),
                        budgetAmount = o.getDouble("budgetAmount"),
                        status = o.getString("status"),
                        notes = o.optString("notes", ""),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }

            val projectPlans = mutableListOf<ProjectPlanEntity>()
            if (root.has("projectPlans")) {
                val array = root.getJSONArray("projectPlans")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    projectPlans += ProjectPlanEntity(
                        id = o.getString("id"),
                        project = o.optString("project", ""),
                        planDate = o.getString("planDate"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        estimatedAmount = o.optDouble("estimatedAmount", 0.0),
                        status = o.getString("status"),
                        details = o.optString("details", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }

            val housingUnits = mutableListOf<HousingUnitEntity>()
            if (root.has("housingUnits")) {
                val array = root.getJSONArray("housingUnits")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    housingUnits += HousingUnitEntity(
                        id = o.getString("id"),
                        project = o.getString("project"),
                        unitCode = o.getString("unitCode"),
                        block = o.optString("block", ""),
                        sitePosition = o.optString("sitePosition", ""),
                        businessModel = o.getString("businessModel"),
                        landAreaM2 = o.getDouble("landAreaM2"),
                        buildingAreaM2 = o.optDouble("buildingAreaM2", 0.0),
                        salePrice = o.getDouble("salePrice"),
                        buyerName = o.optString("buyerName", ""),
                        status = o.getString("status"),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }

            val transactions = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val toAccount = if (o.isNull("toAccount")) null else o.getString("toAccount").takeIf { it.isNotBlank() }
                    val archivedAt = if (o.isNull("archivedAt")) null else o.getLong("archivedAt")
                    val archivedBy = if (o.isNull("archivedBy")) null else o.getString("archivedBy")
                    transactions += TransactionEntity(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        date = o.getString("date"),
                        time = o.getString("time"),
                        account = o.getString("account"),
                        toAccount = toAccount,
                        name = o.getString("name"),
                        category = o.getString("category"),
                        description = o.getString("description"),
                        amount = o.getDouble("amount"),
                        allocation = o.getString("allocation"),
                        pic = o.getString("pic"),
                        proofUrl = o.getString("proofUrl"),
                        receiptNo = o.getString("receiptNo"),
                        project = o.getString("project"),
                        note = o.getString("note"),
                        status = o.getString("status"),
                        inputTime = o.getLong("inputTime"),
                        inputBy = o.getString("inputBy"),
                        isArchived = o.getBoolean("isArchived"),
                        archivedAt = archivedAt,
                        archivedBy = archivedBy,
                        fundBucket = o.optString("fundBucket", "PT")
                    )
                }
            }

            database.withTransaction {
                val existingAccounts = accountDao.getAllAccounts().first()
                val existingTransactions = (
                    transactionDao.getAllActiveTransactions().first() +
                        transactionDao.getArchivedTransactions().first()
                    ).distinctBy { it.id }
                val existingReceivables = receivableDao.getAllReceivables().first()
                val existingEmployees = employeeDao.getAllEmployees().first()
                val existingAttendances = attendanceDao.getAllAttendances().first()
                val existingReconciliations = bankReconDao.getAllReconciliations().first()
                val existingBudgets = budgetDao.getAllBudgets().first()
                val existingNotes = noteDao.getAllNotes().first()
                val existingProjects = projectDao.getAllProjects().first()
                val existingPlans = projectPlanDao.getAllPlans().first()
                val existingUnits = housingUnitDao.getAllUnits().first()

                val existingAccountIds = existingAccounts.map { it.id }.toSet()
                val existingTransactionIds = existingTransactions.map { it.id }.toSet()
                val existingReceivableIds = existingReceivables.map { it.id }.toSet()
                val existingEmployeeIds = existingEmployees.map { it.id }.toSet()
                val existingAttendanceIds = existingAttendances.map { it.id }.toSet()
                val existingReconIds = existingReconciliations.map { it.id }.toSet()
                val existingBudgetIds = existingBudgets.map { it.id }.filter { it != 0L }.toSet()
                val existingNoteIds = existingNotes.map { it.id }.filter { it != 0L }.toSet()
                val existingProjectIds = existingProjects.map { it.id }.toSet()
                val existingPlanIds = existingPlans.map { it.id }.toSet()
                val existingUnitIds = existingUnits.map { it.id }.toSet()

                require(accounts.map { it.id }.distinct().size == accounts.size) { "Backup memiliki ID akun duplikat." }
                require(accounts.none { it.id in existingAccountIds }) { "Backup memiliki ID akun yang sudah ada di database." }
                require(transactions.map { it.id }.distinct().size == transactions.size) { "Backup memiliki ID transaksi duplikat." }
                require(transactions.none { it.id in existingTransactionIds }) { "Backup memiliki ID transaksi yang sudah ada di database." }
                require(receivables.map { it.id }.distinct().size == receivables.size) { "Backup memiliki ID tagihan duplikat." }
                require(receivables.none { it.id in existingReceivableIds }) { "Backup memiliki ID tagihan yang sudah ada di database." }
                require(employees.map { it.id }.distinct().size == employees.size) { "Backup memiliki ID karyawan duplikat." }
                require(employees.none { it.id in existingEmployeeIds }) { "Backup memiliki ID karyawan yang sudah ada di database." }
                require(attendances.map { it.id }.distinct().size == attendances.size) { "Backup memiliki ID absensi duplikat." }
                require(attendances.none { it.id in existingAttendanceIds }) { "Backup memiliki ID absensi yang sudah ada di database." }
                require(recons.map { it.id }.distinct().size == recons.size) { "Backup memiliki ID rekonsiliasi duplikat." }
                require(recons.none { it.id in existingReconIds }) { "Backup memiliki ID rekonsiliasi yang sudah ada di database." }
                require(budgets.filter { it.id != 0L }.map { it.id }.distinct().size == budgets.count { it.id != 0L }) {
                    "Backup memiliki ID anggaran duplikat."
                }
                require(budgets.all { it.id == 0L || it.id !in existingBudgetIds }) { "Backup memiliki ID anggaran yang sudah ada di database." }
                require(notes.filter { it.id != 0L }.map { it.id }.distinct().size == notes.count { it.id != 0L }) {
                    "Backup memiliki ID catatan duplikat."
                }
                require(notes.all { it.id == 0L || it.id !in existingNoteIds }) { "Backup memiliki ID catatan yang sudah ada di database." }
                require(projects.map { it.id }.distinct().size == projects.size) { "Backup memiliki ID proyek duplikat." }
                require(projects.none { it.id in existingProjectIds }) { "Backup memiliki ID proyek yang sudah ada di database." }
                require(projectPlans.map { it.id }.distinct().size == projectPlans.size) { "Backup memiliki ID rencana duplikat." }
                require(projectPlans.none { it.id in existingPlanIds }) { "Backup memiliki ID rencana yang sudah ada di database." }
                require(housingUnits.map { it.id }.distinct().size == housingUnits.size) { "Backup memiliki ID unit duplikat." }
                require(housingUnits.none { it.id in existingUnitIds }) { "Backup memiliki ID unit yang sudah ada di database." }

                val seenAccountNames = mutableSetOf<String>()
                accounts.forEach { account ->
                    require(account.name.isNotBlank()) { "Backup memiliki akun tanpa nama." }
                    require(account.type in setOf("Kas", "Bank", "E-Wallet", "Lainnya")) { "Backup memiliki jenis akun tidak valid: ${account.type}." }
                    require(account.initialBalance.isFinite() && account.initialBalance >= 0.0) { "Backup memiliki saldo awal akun tidak valid: ${account.name}." }
                    val key = account.name.trim().lowercase(Locale.ROOT)
                    require(seenAccountNames.add(key)) { "Backup memiliki nama akun duplikat: ${account.name}." }
                    val existing = accountDao.getAccountByName(account.name)
                    require(existing == null || existing.id == account.id) { "Backup bentrok dengan nama akun yang sudah dipakai: ${account.name}." }
                }

                fun normalizedKey(value: String): String = value.trim().lowercase(Locale.ROOT)
                fun budgetScope(period: String, category: String, project: String, fundBucket: String): String =
                    listOf(period, category, project, fundBucket).joinToString("|") { normalizedKey(it) }
                fun unitScope(project: String, code: String): String =
                    normalizedKey(project) + "|" + normalizedKey(code)
                fun attendanceScope(employeeId: String, date: String): String =
                    normalizedKey(employeeId) + "|" + date
                fun reconciliationScope(accountName: String, period: String): String =
                    normalizedKey(accountName) + "|" + period

                val allProjectRows = existingProjects + projects
                val allAccountRows = existingAccounts + accounts
                val allEmployeeRows = existingEmployees + employees
                val projectNameKeys = allProjectRows.map { normalizedKey(it.name) }
                require(projectNameKeys.none { it.isBlank() } && projectNameKeys.distinct().size == projectNameKeys.size) {
                    "Backup atau database tujuan memiliki nama proyek duplikat atau kosong."
                }
                require(projects.none { incoming ->
                    existingProjects.any { normalizedKey(it.name) == normalizedKey(incoming.name) }
                }) { "Backup bentrok dengan nama proyek yang sudah ada di database." }
                projects.forEach { project ->
                    require(project.name.isNotBlank()) { "Backup memiliki proyek tanpa nama." }
                    require(project.category in setOf("Pembebasan Tanah", "Cut & Fill", "Perumahan", "Perdagangan", "Operasional PT", "Lainnya")) {
                        "Jenis proyek backup tidak valid."
                    }
                    require(project.businessModel in setOf("Subsidi", "Komersial", "Tidak berlaku") &&
                        (project.category == "Perumahan" || project.businessModel == "Tidak berlaku")) {
                        "Model bisnis backup tidak sesuai jenis proyek."
                    }
                    requireIsoDate(project.startDate, "Tanggal mulai proyek backup")
                    requireIsoDate(project.targetEndDate, "Tanggal target proyek backup")
                    require(project.targetEndDate >= project.startDate) { "Target proyek backup lebih awal daripada tanggal mulai." }
                    require(project.budgetAmount.isFinite() && project.budgetAmount >= 0.0) { "Pagu proyek backup tidak valid." }
                    require(project.status in setOf("Berjalan", "Ditunda", "Selesai")) { "Status proyek backup tidak valid." }
                }

                val budgetScopes = budgets.map { budgetScope(it.period, it.category, it.project, it.fundBucket) }
                require(budgetScopes.distinct().size == budgetScopes.size) {
                    "Backup memiliki cakupan anggaran duplikat (periode/kategori/proyek/kelompok dana)."
                }
                require(budgets.none { incoming ->
                    existingBudgets.any {
                        budgetScope(it.period, it.category, it.project, it.fundBucket) ==
                            budgetScope(incoming.period, incoming.category, incoming.project, incoming.fundBucket)
                    }
                }) { "Backup bentrok dengan cakupan anggaran yang sudah ada; gabungkan atau hapus duplikasi lebih dahulu." }

                val unitScopes = housingUnits.map { unitScope(it.project, it.unitCode) }
                require(unitScopes.distinct().size == unitScopes.size) {
                    "Backup memiliki kode unit duplikat dalam proyek yang sama."
                }
                require(housingUnits.none { incoming ->
                    existingUnits.any { unitScope(it.project, it.unitCode) == unitScope(incoming.project, incoming.unitCode) }
                }) { "Backup bentrok dengan kode unit yang sudah ada di database." }

                val attendanceScopes = attendances.map { attendanceScope(it.employeeId, it.date) }
                require(attendanceScopes.distinct().size == attendanceScopes.size) {
                    "Backup memiliki lebih dari satu absensi untuk karyawan pada tanggal yang sama."
                }
                require(attendances.none { incoming ->
                    existingAttendances.any { attendanceScope(it.employeeId, it.date) == attendanceScope(incoming.employeeId, incoming.date) }
                }) { "Backup bentrok dengan absensi karyawan/tanggal yang sudah ada di database." }

                val reconciliationScopes = recons.map { reconciliationScope(it.accountName, it.period) }
                require(reconciliationScopes.distinct().size == reconciliationScopes.size) {
                    "Backup memiliki lebih dari satu rekonsiliasi untuk akun dan bulan yang sama."
                }
                require(recons.none { incoming ->
                    existingReconciliations.any { reconciliationScope(it.accountName, it.period) == reconciliationScope(incoming.accountName, incoming.period) }
                }) { "Backup bentrok dengan rekonsiliasi akun/bulan yang sudah ada." }

                val employeeIdSet = allEmployeeRows.map { it.id }.toSet()
                val projectByName = allProjectRows.associateBy { normalizedKey(it.name) }
                val receivableById = receivables.associateBy { it.id }

                housingUnits.forEach { unit ->
                    val project = projectByName[normalizedKey(unit.project)]
                        ?: throw IllegalArgumentException("Proyek unit backup tidak ditemukan: ${unit.project}")
                    require(project.category == "Perumahan") { "Unit backup harus terkait dengan master proyek perumahan." }
                    require(unit.businessModel in setOf("Subsidi", "Komersial") && unit.businessModel == project.businessModel) {
                        "Segmen unit backup berbeda dari segmen proyek."
                    }
                    require(unit.status in setOf("Tersedia", "Booking", "Terjual", "Dibatalkan")) { "Status unit backup tidak valid." }
                    require(unit.status != "Terjual" || unit.buyerName.isNotBlank()) { "Nama pembeli wajib diisi untuk unit terjual." }
                    require(unit.landAreaM2.isFinite() && unit.landAreaM2 > 0.0 &&
                        unit.buildingAreaM2.isFinite() && unit.buildingAreaM2 >= 0.0 &&
                        unit.salePrice.isFinite() && unit.salePrice > 0.0) { "Ukuran atau harga unit backup tidak valid." }
                }
                budgets.forEach { budget ->
                    require(budget.period.equals("All", ignoreCase = true) ||
                        budget.period.matches(Regex("""\d{4}-\d{2}"""))) { "Backup memiliki periode anggaran tidak valid." }
                    if (!budget.period.equals("All", ignoreCase = true)) periodEndDate(budget.period)
                    require(budget.category.isNotBlank()) { "Backup memiliki kategori anggaran kosong." }
                    require(budget.budgetAmount.isFinite() && budget.budgetAmount >= 0.0) { "Backup memiliki nominal anggaran tidak valid." }
                    require(budget.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana anggaran backup tidak valid." }
                    require(budget.project.isBlank() || normalizedKey(budget.project) in projectByName) { "Proyek anggaran backup tidak ditemukan: ${budget.project}" }
                }
                receivables.forEach { item ->
                    require(item.type in setOf("PIUTANG", "HUTANG")) { "Jenis tagihan backup tidak valid." }
                    requireIsoDate(item.date, "Tanggal tagihan backup")
                    requireIsoDate(item.dueDate, "Jatuh tempo backup")
                    require(item.totalAmount.isFinite() && item.totalAmount > 0.0 &&
                        item.paidAmount.isFinite() && item.paidAmount >= 0.0 && item.paidAmount <= item.totalAmount) {
                        "Nominal tagihan backup tidak valid: ${item.id}."
                    }
                    require(allAccountRows.count { normalizedKey(it.name) == normalizedKey(item.targetAccount) } == 1) {
                        "Akun tagihan backup tidak ditemukan atau tidak unik: ${item.targetAccount}."
                    }
                    require(item.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana tagihan backup tidak valid." }
                    require(item.project.isBlank() || normalizedKey(item.project) in projectByName) { "Proyek tagihan backup tidak ditemukan: ${item.project}" }
                }
                employees.forEach { employee ->
                    require(employee.name.isNotBlank() && employee.dailyRate.isFinite() && employee.dailyRate >= 0.0 &&
                        employee.monthlySalary.isFinite() && employee.monthlySalary >= 0.0) {
                        "Data atau nominal karyawan backup tidak valid: ${employee.id}."
                    }
                    require(employee.defaultProject.isBlank() || normalizedKey(employee.defaultProject) in projectByName) {
                        "Proyek alokasi karyawan backup tidak ditemukan: ${employee.defaultProject}"
                    }
                }
                attendances.forEach { attendance ->
                    require(attendance.employeeId in employeeIdSet) { "Absensi backup menunjuk karyawan yang tidak ada: ${attendance.employeeId}" }
                    requireIsoDate(attendance.date, "Tanggal absensi backup")
                    require(attendance.status in setOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti")) { "Status absensi backup tidak valid." }
                    require(attendance.overtimeHours.isFinite() && attendance.overtimeHours >= 0.0 &&
                        attendance.dailyAllowance.isFinite() && attendance.dailyAllowance >= 0.0) { "Nilai lembur/tunjangan backup tidak valid." }
                    require(attendance.project.isBlank() || normalizedKey(attendance.project) in projectByName) { "Proyek absensi backup tidak ditemukan: ${attendance.project}" }
                }
                projectPlans.forEach { plan ->
                    requireIsoDate(plan.planDate, "Tanggal rencana backup")
                    require(plan.title.isNotBlank() && plan.estimatedAmount.isFinite() && plan.estimatedAmount >= 0.0) {
                        "Data rencana backup tidak valid: ${plan.id}."
                    }
                    require(plan.status in setOf("Direncanakan", "Selesai", "Batal")) { "Status rencana backup tidak valid." }
                    require(plan.project.isBlank() || normalizedKey(plan.project) in projectByName) { "Proyek rencana backup tidak ditemukan: ${plan.project}" }
                }
                notes.forEach { note ->
                    requireIsoDate(note.date, "Tanggal catatan backup")
                    require(note.title.isNotBlank() && note.priority in setOf("Rendah", "Sedang", "Tinggi") &&
                        note.status in setOf("Open", "Done", "Follow Up")) { "Data catatan backup tidak valid: ${note.id}." }
                    require(note.project.isBlank() || normalizedKey(note.project) in projectByName) { "Proyek catatan backup tidak ditemukan: ${note.project}" }
                }
                recons.forEach { recon ->
                    require(recon.period.matches(Regex("""\d{4}-\d{2}"""))) { "Periode rekonsiliasi backup tidak valid: ${recon.period}." }
                    periodEndDate(recon.period)
                    require(recon.statementBalance.isFinite() && recon.statementBalance >= 0.0) { "Saldo rekening koran backup tidak valid." }
                    require(allAccountRows.count { normalizedKey(it.name) == normalizedKey(recon.accountName) } == 1) {
                        "Akun rekonsiliasi backup tidak ditemukan atau tidak unik: ${recon.accountName}."
                    }
                }
                transactions.forEach { tx ->
                    require(tx.account.isNotBlank() && allAccountRows.count { normalizedKey(it.name) == normalizedKey(tx.account) } == 1) {
                        "Akun transaksi backup tidak ditemukan atau tidak unik: ${tx.account}."
                    }
                    if (tx.type == "TRANSFER") require(tx.toAccount?.let { to -> allAccountRows.count { normalizedKey(it.name) == normalizedKey(to) } == 1 } == true) {
                        "Akun tujuan transfer backup tidak ditemukan atau tidak unik: ${tx.toAccount}."
                    }
                    require(tx.project.isBlank() || normalizedKey(tx.project) in projectByName) {
                        "Proyek transaksi backup tidak ditemukan: ${tx.project}"
                    }
                    val receipt = tx.receiptNo.trim()
                    when {
                        receipt.startsWith("PAYROLL-", ignoreCase = true) -> {
                            require(tx.type == "KELUAR" && tx.category.equals("Gaji", ignoreCase = true) &&
                                tx.status.equals("Selesai", ignoreCase = true)) {
                                "Transaksi payroll backup harus KELUAR/Gaji/Selesai."
                            }
                            payrollPeriodRange(receipt.substring("PAYROLL-".length).trim())
                        }
                        receipt.startsWith("PIU-", ignoreCase = true) -> {
                            val id = receipt.substring(4).trim()
                            val item = receivableById[id] ?: throw IllegalArgumentException("Pembayaran backup ${receipt} tidak memiliki master piutang di dalam restore.")
                            require(item.type == "PIUTANG" && tx.type == "MASUK" &&
                                tx.category.equals("Piutang Masuk", ignoreCase = true) && tx.status.equals("Selesai", ignoreCase = true)) {
                                "Relasi pembayaran PIUTANG backup tidak konsisten: ${receipt}."
                            }
                        }
                        receipt.startsWith("HUT-", ignoreCase = true) -> {
                            val id = receipt.substring(4).trim()
                            val item = receivableById[id] ?: throw IllegalArgumentException("Pembayaran backup ${receipt} tidak memiliki master hutang di dalam restore.")
                            require(item.type == "HUTANG" && tx.type == "KELUAR" &&
                                tx.category.equals("Belanja Barang", ignoreCase = true) && tx.status.equals("Selesai", ignoreCase = true)) {
                                "Relasi pembayaran HUTANG backup tidak konsisten: ${receipt}."
                            }
                        }
                    }
                }
                val importedPayrollPeriods = transactions.mapNotNull { tx ->
                    payrollPeriodFromReceipt(tx.receiptNo).takeIf { isSettledPayrollTransaction(tx) }
                }
                val payrollConflict = payrollPeriodConflict(importedPayrollPeriods, existingTransactions)
                require(payrollConflict == null) { payrollConflict ?: "Periode payroll backup bertumpang tindih." }
                receivables.forEach { item ->
                    val prefix = if (item.type == "PIUTANG") "PIU-" else "HUT-"
                    val expectedCategory = if (item.type == "PIUTANG") "Piutang Masuk" else "Belanja Barang"
                    val expectedType = if (item.type == "PIUTANG") "MASUK" else "KELUAR"
                    val linkedTotal = transactions.filter {
                        it.receiptNo.equals(prefix + item.id, ignoreCase = true) &&
                            it.category.equals(expectedCategory, ignoreCase = true) &&
                            it.type == expectedType && it.status.equals("Selesai", ignoreCase = true)
                    }.sumOf { it.amount }
                    require(kotlin.math.abs(linkedTotal - item.paidAmount) <= 0.01) {
                        "Pembayaran master ${item.id} tidak sama dengan ledger pada backup."
                    }
                }

                accounts.forEach { account -> accountDao.insertAccount(account.copy(name = account.name.trim())) }

                projects.forEach { project ->
                    require(project.name.isNotBlank()) { "Backup memiliki proyek tanpa nama." }
                    require(project.category in setOf("Pembebasan Tanah", "Cut & Fill", "Perumahan", "Perdagangan", "Operasional PT", "Lainnya")) { "Jenis proyek backup tidak valid." }
                    requireIsoDate(project.startDate, "Tanggal mulai proyek backup")
                    requireIsoDate(project.targetEndDate, "Tanggal target proyek backup")
                    require(project.targetEndDate >= project.startDate) { "Target proyek backup lebih awal daripada tanggal mulai." }
                    require(project.budgetAmount.isFinite() && project.budgetAmount >= 0.0) { "Pagu proyek backup tidak valid." }
                    require(project.status in setOf("Berjalan", "Ditunda", "Selesai")) { "Status proyek backup tidak valid." }
                    require(projectDao.countByName(project.name) == 0) { "Nama proyek backup sudah dipakai: " + project.name }
                }
                projects.forEach { projectDao.insertProject(it.copy(name = it.name.trim())) }

                housingUnits.forEach { unit ->
                    val project = projectDao.getProjectByName(unit.project)
                        ?: throw IllegalArgumentException("Proyek unit backup tidak ditemukan: " + unit.project)
                    require(project.category == "Perumahan") { "Unit backup harus terkait dengan master proyek perumahan." }
                    require(unit.businessModel == project.businessModel) { "Segmen unit backup berbeda dari segmen proyek." }
                    require(unit.unitCode.isNotBlank()) { "Kode unit backup wajib diisi." }
                    require(unit.landAreaM2.isFinite() && unit.landAreaM2 > 0.0) { "Luas tanah unit backup tidak valid." }
                    require(unit.buildingAreaM2.isFinite() && unit.buildingAreaM2 >= 0.0) { "Luas bangunan unit backup tidak valid." }
                    require(unit.salePrice.isFinite() && unit.salePrice > 0.0) { "Harga unit backup tidak valid." }
                    require(unit.status in setOf("Tersedia", "Booking", "Terjual", "Dibatalkan")) { "Status unit backup tidak valid." }
                    require(unit.status != "Terjual" || unit.buyerName.isNotBlank()) { "Nama pembeli wajib diisi untuk unit terjual." }
                    require(housingUnitDao.countCodeInProject(project.name, unit.unitCode) == 0) { "Kode unit backup sudah digunakan: " + unit.unitCode }
                }
                housingUnits.forEach { housingUnitDao.insertUnit(it) }

                budgets.forEach { budget ->
                    require(budget.period.equals("All", ignoreCase = true) || budget.period.matches(Regex("""\d{4}-\d{2}"""))) { "Backup memiliki periode anggaran tidak valid: ${budget.period}." }
                    if (!budget.period.equals("All", ignoreCase = true)) periodEndDate(budget.period)
                    require(budget.category.isNotBlank()) { "Backup memiliki kategori anggaran kosong." }
                    require(budget.budgetAmount.isFinite() && budget.budgetAmount >= 0.0) { "Backup memiliki nominal anggaran tidak valid." }
                    require(budget.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana anggaran backup tidak valid." }
                    if (budget.project.isNotBlank()) require(projectDao.getProjectByName(budget.project) != null) { "Proyek anggaran backup tidak ditemukan: " + budget.project }
                }
                if (budgets.isNotEmpty()) budgetDao.insertBudgets(budgets)

                receivables.forEach { receivable ->
                    require(receivable.type in setOf("PIUTANG", "HUTANG")) { "Backup memiliki jenis tagihan tidak valid." }
                    require(receivable.customerName.isNotBlank()) { "Backup memiliki tagihan tanpa nama pihak." }
                    requireIsoDate(receivable.date, "Tanggal tagihan ${receivable.id}")
                    requireIsoDate(receivable.dueDate, "Jatuh tempo ${receivable.id}")
                    require(receivable.totalAmount.isFinite() && receivable.totalAmount > 0.0) { "Nominal tagihan ${receivable.id} tidak valid." }
                    require(receivable.paidAmount.isFinite() && receivable.paidAmount >= 0.0 && receivable.paidAmount <= receivable.totalAmount) { "Pembayaran tagihan ${receivable.id} tidak valid." }
                    require(accountDao.getAccountByName(receivable.targetAccount) != null) { "Akun tagihan ${receivable.id} tidak terdaftar." }
                    require(receivable.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana tagihan backup tidak valid." }
                    if (receivable.project.isNotBlank()) require(projectDao.getProjectByName(receivable.project) != null) { "Proyek tagihan backup tidak ditemukan: " + receivable.project }
                }
                if (receivables.isNotEmpty()) receivableDao.insertReceivables(
                    receivables.map { r -> r.copy(status = if (r.paidAmount >= r.totalAmount) "Lunas" else r.status) }
                )

                employees.forEach { employee ->
                    require(employee.name.isNotBlank()) { "Backup memiliki karyawan tanpa nama." }
                    require(employee.dailyRate.isFinite() && employee.dailyRate >= 0.0) { "Uang harian ${employee.id} tidak valid." }
                    require(employee.monthlySalary.isFinite() && employee.monthlySalary >= 0.0) { "Gaji bulanan " + employee.id + " tidak valid." }
                    if (employee.defaultProject.isNotBlank()) require(projectDao.getProjectByName(employee.defaultProject) != null) { "Proyek alokasi karyawan backup tidak ditemukan: " + employee.defaultProject }
                }
                if (employees.isNotEmpty()) employeeDao.insertEmployees(employees)

                attendances.forEach { attendance ->
                    require(attendance.employeeId.isNotBlank()) { "Absensi ${attendance.id} tanpa employeeId." }
                    require(employeeDao.getEmployeeById(attendance.employeeId) != null) { "Absensi ${attendance.id} menunjuk karyawan yang tidak ada." }
                    requireIsoDate(attendance.date, "Tanggal absensi ${attendance.id}")
                    require(attendance.status in setOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti")) { "Status absensi ${attendance.id} tidak valid." }
                    require(attendance.overtimeHours.isFinite() && attendance.overtimeHours >= 0.0) { "Jam lembur ${attendance.id} tidak valid." }
                    require(attendance.dailyAllowance.isFinite() && attendance.dailyAllowance >= 0.0) { "Uang harian " + attendance.id + " tidak valid." }
                    if (attendance.project.isNotBlank()) require(projectDao.getProjectByName(attendance.project) != null) { "Proyek absensi backup tidak ditemukan: " + attendance.project }
                }
                if (attendances.isNotEmpty()) attendanceDao.insertAttendances(attendances)

                transactions.forEach { tx ->
                    validateTransaction(
                        tx,
                        allowInactiveAccountReferences = true,
                        allowLegacyTransferIncome = true,
                        allowSystemReceiptNo = true
                    )
                    require(tx.date.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) { "Tanggal transaksi ${tx.id} tidak valid." }
                    require(tx.time.matches(Regex("""\d{2}:\d{2}:\d{2}"""))) { "Jam transaksi ${tx.id} tidak valid." }
                    require(tx.name.isNotBlank() && tx.category.isNotBlank()) { "Transaksi " + tx.id + " tidak lengkap." }
                    require(tx.fundBucket in setOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")) { "Kelompok dana transaksi backup tidak valid." }
                    require(tx.inputTime > 0L) { "Waktu input transaksi ${tx.id} tidak valid." }
                    require(!tx.isArchived || tx.archivedAt != null) { "Transaksi arsip ${tx.id} harus memiliki waktu arsip." }
                    require(!tx.isArchived || tx.archivedBy?.isNotBlank() == true) { "Transaksi arsip ${tx.id} harus memiliki pengarsip." }
                    require(tx.isArchived || (tx.archivedAt == null && tx.archivedBy == null)) { "Transaksi aktif ${tx.id} tidak boleh memiliki metadata arsip." }
                }
                if (transactions.isNotEmpty()) transactionDao.insertTransactions(transactions)

                recons.forEach { recon ->
                    require(recon.period.matches(Regex("""\d{4}-\d{2}"""))) { "Periode rekonsiliasi ${recon.id} tidak valid." }
                    require(recon.statementBalance.isFinite() && recon.statementBalance >= 0.0) { "Saldo rekening koran ${recon.id} tidak valid." }
                    val account = accountDao.getAccountByName(recon.accountName)
                        ?: throw IllegalArgumentException("Akun rekonsiliasi ${recon.id} tidak terdaftar.")
                    val ledger = (transactionDao.getAllActiveTransactions().first() + transactionDao.getArchivedTransactions().first()).distinctBy { it.id }
                    val bookBalance = calculateAccountBalanceAtPeriod(account, recon.period, ledger)
                    val difference = recon.statementBalance - bookBalance
                    bankReconDao.insertReconciliation(
                        recon.copy(accountName = account.name, bookBalance = bookBalance, difference = difference, status = if (kotlin.math.abs(difference) < 1.0) "Cocok" else "Selisih")
                    )
                }

                projectPlans.forEach { plan ->
                    requireIsoDate(plan.planDate, "Tanggal rencana backup")
                    require(plan.title.isNotBlank()) { "Rencana backup tidak memiliki judul." }
                    require(plan.estimatedAmount.isFinite() && plan.estimatedAmount >= 0.0) { "Estimasi rencana backup tidak valid." }
                    require(plan.status in setOf("Direncanakan", "Selesai", "Batal")) { "Status rencana backup tidak valid." }
                    if (plan.project.isNotBlank()) require(projectDao.getProjectByName(plan.project) != null) { "Proyek rencana backup tidak ditemukan: " + plan.project }
                }
                projectPlans.forEach { projectPlanDao.insertPlan(it) }

                if (notes.isNotEmpty()) {
                    notes.forEach { note ->
                        requireIsoDate(note.date, "Tanggal catatan ${note.id}")
                        require(note.title.isNotBlank()) { "Catatan ${note.id} tidak memiliki judul." }
                        require(note.priority in setOf("Rendah", "Sedang", "Tinggi")) { "Prioritas catatan ${note.id} tidak valid." }
                        require(note.status in setOf("Open", "Done", "Follow Up")) { "Status catatan " + note.id + " tidak valid." }
                        if (note.project.isNotBlank()) require(projectDao.getProjectByName(note.project) != null) { "Proyek catatan backup tidak ditemukan: " + note.project }
                    }
                    noteDao.insertNotes(notes)
                }

                audits.forEach { audit ->
                    require(audit.dateFormatted.isNotBlank() && audit.action.isNotBlank() && audit.recordId.isNotBlank()) { "Backup memiliki audit log yang tidak lengkap." }
                    require(audit.balanceAfter.isFinite()) { "Audit log ${audit.recordId} memiliki balanceAfter tidak valid." }
                }
                audits.forEach { auditDao.insertAuditLog(it.copy(id = 0L)) }
            }

            Result.success(transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val transactionSpreadsheetHeaders = listOf(
        "ID", "Tipe", "Tanggal", "Jam", "Akun", "Ke Akun", "Nama Transaksi",
        "Kategori", "Keterangan", "Nominal", "Alokasi", "PIC", "Bukti", "No Bukti",
        "Proyek", "Catatan", "Status", "Waktu Input", "Input Oleh", "Diarsipkan",
        "Waktu Arsip", "Diarsipkan Oleh", "Kelompok Dana"
    )

    suspend fun exportCurrentTransactionsToCsv(): String {
        val currentTransactions = (
            transactionDao.getAllActiveTransactions().first() +
                transactionDao.getArchivedTransactions().first()
            ).distinctBy { it.id }
        return exportTransactionsToCsv(currentTransactions)
    }

    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val ordered = transactions.sortedWith(
            compareByDescending<TransactionEntity> { it.date }
                .thenByDescending { it.time }
                .thenByDescending { it.inputTime }
        )
        val sb = StringBuilder("\uFEFF")
        sb.append(transactionSpreadsheetHeaders.joinToString(separator = ",", transform = ::escapeCsvCell))
            .append('\n')

        ordered.forEach { tx ->
            val row = listOf(
                tx.id, tx.type, tx.date, tx.time, tx.account, tx.toAccount.orEmpty(),
                tx.name, tx.category, tx.description, tx.amount.toString(), tx.allocation,
                tx.pic, tx.proofUrl, tx.receiptNo, tx.project, tx.note, tx.status,
                formatSpreadsheetTimestamp(tx.inputTime), tx.inputBy,
                if (tx.isArchived) "YA" else "TIDAK",
                tx.archivedAt?.let(::formatSpreadsheetTimestamp).orEmpty(),
                tx.archivedBy.orEmpty(), tx.fundBucket
            )
            row.forEachIndexed { index, value ->
                if (index > 0) sb.append(',')
                sb.append(escapeCsvCell(value))
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    suspend fun importTransactionsFromCsv(csvContent: String): Result<Int> {
        return try {
            require(csvContent.isNotBlank()) { "File spreadsheet kosong." }

            val delimiter = detectCsvDelimiter(csvContent)
            val records = parseCsvRecords(csvContent, delimiter)
                .map { cells -> cells.map { it.trim() } }
                .filter { row -> row.any { it.isNotBlank() } }

            if (records.size < 2) return Result.success(0)

            val headerMap = records.first().mapIndexed { index, rawHeader ->
                normalizeSpreadsheetHeader(rawHeader) to index
            }.filter { (header, _) -> header.isNotBlank() }.toMap()

            fun findColumn(vararg names: String): Int? =
                names.firstNotNullOfOrNull { headerMap[normalizeSpreadsheetHeader(it)] }

            val required = mapOf(
                "ID" to findColumn("ID"),
                "Tipe" to findColumn("Tipe", "Type"),
                "Tanggal" to findColumn("Tanggal", "Date"),
                "Jam" to findColumn("Jam", "Time"),
                "Akun" to findColumn("Akun", "Account"),
                "Nama Transaksi" to findColumn("Nama Transaksi", "Nama", "Transaction Name"),
                "Kategori" to findColumn("Kategori", "Category"),
                "Nominal" to findColumn("Nominal", "Amount"),
                "Status" to findColumn("Status")
            )
            val missingHeaders = required.filterValues { it == null }.keys
            require(missingHeaders.isEmpty()) {
                "Kolom wajib spreadsheet tidak lengkap: " + missingHeaders.joinToString(", ") + "."
            }

            val existingTransactions = (
                transactionDao.getAllActiveTransactions().first() +
                    transactionDao.getArchivedTransactions().first()
                ).distinctBy { it.id }
            val existingIds = existingTransactions.map { it.id }.toSet()
            val txList = mutableListOf<TransactionEntity>()
            val seenIds = mutableSetOf<String>()
            val importedPayrollPeriods = linkedSetOf<String>()
            val linkedPaymentsByReceivable = linkedMapOf<String, Double>()
            val errors = mutableListOf<String>()

            fun cell(row: List<String>, vararg names: String): String {
                val index = findColumn(*names) ?: return ""
                return row.getOrNull(index).orEmpty()
            }

            records.drop(1).forEachIndexed { index, row ->
                val rowNumber = index + 2
                try {
                    require(row.size == records.first().size) {
                        "jumlah kolom tidak sama dengan header (" + row.size + "/" + records.first().size + ")"
                    }

                    val id = cell(row, "ID").trim()
                    require(id.isNotBlank()) { "ID wajib diisi; jangan gunakan baris tanpa identitas transaksi." }
                    require(id !in existingIds) {
                        "ID " + id + " sudah ada di database; import dibatalkan agar tidak menimpa data."
                    }
                    require(seenIds.add(id)) { "ID " + id + " muncul lebih dari sekali dalam file." }

                    val type = cell(row, "Tipe", "Type").trim().uppercase(Locale.ROOT)
                    val date = cell(row, "Tanggal", "Date").trim()
                    val time = cell(row, "Jam", "Time").trim()
                    val account = cell(row, "Akun", "Account").trim()
                    val toAccount = cell(row, "Ke Akun", "Akun Tujuan", "To Account").trim().takeIf { it.isNotBlank() }
                    val name = cell(row, "Nama Transaksi", "Nama", "Transaction Name").trim()
                    val rawCategory = cell(row, "Kategori", "Category").trim()
                    val category = when {
                        rawCategory.equals("Gaji", ignoreCase = true) -> "Gaji"
                        rawCategory.equals("Piutang Masuk", ignoreCase = true) -> "Piutang Masuk"
                        rawCategory.equals("Belanja Barang", ignoreCase = true) -> "Belanja Barang"
                        else -> rawCategory
                    }
                    val description = cell(row, "Keterangan", "Description")
                    val amount = parseSpreadsheetAmount(cell(row, "Nominal", "Amount"))
                    val allocation = cell(row, "Alokasi", "Allocation").trim()
                    val pic = cell(row, "PIC").trim()
                    val proofUrl = cell(row, "Bukti", "Proof").trim()
                    val rawReceiptNo = cell(row, "No Bukti", "Receipt No").trim()
                    val receiptNo = when {
                        rawReceiptNo.startsWith("PAYROLL-", ignoreCase = true) -> "PAYROLL-" + rawReceiptNo.substring("PAYROLL-".length).trim()
                        rawReceiptNo.startsWith("PIU-", ignoreCase = true) -> "PIU-" + rawReceiptNo.substring(4).trim()
                        rawReceiptNo.startsWith("HUT-", ignoreCase = true) -> "HUT-" + rawReceiptNo.substring(4).trim()
                        else -> rawReceiptNo
                    }
                    val project = cell(row, "Proyek", "Project").trim()
                    val note = cell(row, "Catatan", "Note")
                    val rawStatus = cell(row, "Status").trim()
                    val status = listOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")
                        .firstOrNull { it.equals(rawStatus, ignoreCase = true) } ?: rawStatus
                    val rawFundBucket = cell(row, "Kelompok Dana", "Fund Bucket").trim().ifBlank { "PT" }
                    val fundBucket = listOf("PT", "Perdagangan", "Dana Talang", "Pribadi", "Darurat")
                        .firstOrNull { it.equals(rawFundBucket, ignoreCase = true) } ?: rawFundBucket
                    val inputTimeRaw = cell(row, "Waktu Input", "Input Time")
                    val inputTime = if (inputTimeRaw.isBlank()) System.currentTimeMillis() else parseSpreadsheetTimestamp(inputTimeRaw)
                    val inputBy = cell(row, "Input Oleh", "Input By").ifBlank { "Spreadsheet Import" }
                    val isArchived = parseSpreadsheetBoolean(cell(row, "Diarsipkan", "Archived"))
                    val archivedAtRaw = cell(row, "Waktu Arsip", "Archived At")
                    val archivedAt = archivedAtRaw.takeIf { it.isNotBlank() }?.let(::parseSpreadsheetTimestamp)
                    val archivedBy = cell(row, "Diarsipkan Oleh", "Archived By").takeIf { it.isNotBlank() }

                    require(type in setOf("MASUK", "KELUAR", "TRANSFER")) { "tipe transaksi tidak valid" }
                    requireIsoDate(date, "Tanggal spreadsheet")
                    requireIsoTime(time, "Jam spreadsheet")
                    require(name.isNotBlank()) { "nama transaksi wajib diisi" }
                    require(category.isNotBlank()) { "kategori wajib diisi" }
                    require(status in setOf("Selesai", "Draft", "Pending", "Batal", "Dihapus")) { "status tidak valid" }
                    require(inputTime > 0L) { "Waktu input tidak valid" }
                    require(archivedAt == null || archivedAt > 0L) { "Waktu arsip tidak valid" }

                    if (!isArchived) {
                        require(archivedAt == null) { "Transaksi aktif tidak boleh memiliki waktu arsip." }
                        require(archivedBy == null) { "Transaksi aktif tidak boleh memiliki pengarsip." }
                    } else if (archivedAt != null) {
                        require(archivedBy != null) { "Transaksi arsip harus memiliki pengarsip." }
                    }

                    val tx = TransactionEntity(
                        id=id, type=type, date=date, time=time, account=account, toAccount=toAccount,
                        name=name, category=category, description=description, amount=amount,
                        allocation=allocation, pic=pic, proofUrl=proofUrl, receiptNo=receiptNo,
                        project=project, note=note, status=status, inputTime=inputTime, inputBy=inputBy,
                        isArchived=isArchived, archivedAt=archivedAt, archivedBy=archivedBy, fundBucket=fundBucket
                    )
                    validateTransaction(
                        tx,
                        allowInactiveAccountReferences = true,
                        allowSystemReceiptNo = true
                    )
                    when {
                        receiptNo.startsWith("PAYROLL-", ignoreCase = true) -> {
                            require(type == "KELUAR" && category.equals("Gaji", ignoreCase = true) && status.equals("Selesai", ignoreCase = true)) {
                                "Baris payroll harus bertipe KELUAR, kategori Gaji, dan status Selesai."
                            }
                            val payrollPeriod = receiptNo.substring("PAYROLL-".length).trim()
                            payrollPeriodRange(payrollPeriod)
                            importedPayrollPeriods += payrollPeriod
                        }
                        receiptNo.startsWith("PIU-", ignoreCase = true) -> {
                            require(type == "MASUK" && category.equals("Piutang Masuk", ignoreCase = true) && status == "Selesai") {
                                "Baris pembayaran PIUTANG harus bertipe MASUK, kategori Piutang Masuk, dan status Selesai."
                            }
                            val receivableId = receiptNo.substring(4)
                            val linked = receivableDao.getReceivableById(receivableId)
                                ?: throw IllegalArgumentException("Tagihan $receivableId tidak ditemukan. Pulihkan master data melalui Backup JSON sebelum mengimpor pembayaran.")
                            require(linked.type == "PIUTANG") { "Referensi $receiptNo bukan master PIUTANG." }
                            linkedPaymentsByReceivable[receivableId] =
                                (linkedPaymentsByReceivable[receivableId] ?: 0.0) + amount
                        }
                        receiptNo.startsWith("HUT-", ignoreCase = true) -> {
                            require(type == "KELUAR" && category.equals("Belanja Barang", ignoreCase = true) && status == "Selesai") {
                                "Baris pembayaran HUTANG harus bertipe KELUAR, kategori Belanja Barang, dan status Selesai."
                            }
                            val receivableId = receiptNo.substring(4)
                            val linked = receivableDao.getReceivableById(receivableId)
                                ?: throw IllegalArgumentException("Tagihan $receivableId tidak ditemukan. Pulihkan master data melalui Backup JSON sebelum mengimpor pembayaran.")
                            require(linked.type == "HUTANG") { "Referensi $receiptNo bukan master HUTANG." }
                            linkedPaymentsByReceivable[receivableId] =
                                (linkedPaymentsByReceivable[receivableId] ?: 0.0) + amount
                        }
                    }
                    txList += tx
                } catch (ex: Exception) {
                    errors += "Baris " + rowNumber + ": " + (ex.message ?: "data tidak valid")
                }
            }

            if (errors.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException(
                        errors.take(20).joinToString("; ") +
                            if (errors.size > 20) " (dan " + (errors.size - 20) + " error lain)." else ""
                    )
                )
            }

            val preflightPayrollConflict = payrollPeriodConflict(importedPayrollPeriods, existingTransactions)
            require(preflightPayrollConflict == null) { preflightPayrollConflict ?: "Periode payroll bertumpang tindih." }

            database.withTransaction {
                val transactionsBeforeImport = (
                    transactionDao.getAllActiveTransactions().first() +
                        transactionDao.getArchivedTransactions().first()
                    ).distinctBy { it.id }
                val idsNow = transactionsBeforeImport.map { it.id }.toSet()
                require(txList.none { it.id in idsNow }) {
                    "Salah satu ID transaksi sudah masuk saat proses impor berjalan. Tidak ada data yang diimpor."
                }
                val concurrentPayrollConflict = payrollPeriodConflict(importedPayrollPeriods, transactionsBeforeImport)
                require(concurrentPayrollConflict == null) {
                    concurrentPayrollConflict ?: "Periode payroll bertumpang tindih dengan periode yang sudah dibayar."
                }

                linkedPaymentsByReceivable.forEach { (receivableId, amount) ->
                    val current = receivableDao.getReceivableById(receivableId)
                        ?: throw IllegalArgumentException("Tagihan $receivableId tidak ditemukan saat impor disimpan.")
                    require(amount <= current.remainingAmount + 0.000001) {
                        "Total pembayaran impor untuk tagihan $receivableId melebihi sisa tagihan Rp ${current.remainingAmount.toLong()}."
                    }
                    val paid = (current.paidAmount + amount).coerceAtMost(current.totalAmount)
                    val status = if (paid >= current.totalAmount) "Lunas" else current.status
                    receivableDao.updateReceivable(current.copy(paidAmount = paid, status = status))
                }
                if (txList.isNotEmpty()) {
                    transactionDao.insertTransactions(txList)
                    val accountSnapshot = accountDao.getAllAccounts().first()
                    val totalBalanceAfterImport = calculateAccountBalances(
                        accountSnapshot,
                        transactionsBeforeImport + txList
                    ).sumOf { it.currentBalance }
                    auditDao.insertAuditLog(
                        AuditLogEntity(
                            dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            action = "IMPORT_TRANSACTIONS",
                            recordId = generateId("CSV"),
                            details = "Impor spreadsheet menambahkan ${txList.size} transaksi (${txList.minOf { it.date }} s/d ${txList.maxOf { it.date }}).",
                            user = "Spreadsheet Import",
                            verifiedFormulaStatus = "RECORDED",
                            balanceAfter = totalBalanceAfterImport
                        )
                    )
                }
            }
            Result.success(txList.size)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    private fun escapeCsvCell(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n')
        return if (normalized.any { it == ',' || it == ';' || it == '\t' || it == '\n' || it == '"' }) {
            "\"" + normalized.replace("\"", "\"\"") + "\""
        } else {
            normalized
        }
    }

    private fun normalizeSpreadsheetHeader(value: String): String =
        value.removePrefix("\uFEFF").trim().replace(Regex("""\s+"""), " ").lowercase(Locale.getDefault())

    private fun detectCsvDelimiter(content: String): Char {
        val headerEnd = content.indexOfFirst { it == '\n' || it == '\r' }
            .let { if (it >= 0) it else content.length }
        val header = content.substring(0, headerEnd)
        return charArrayOf(',', ';', '\t').maxByOrNull { delimiter ->
            var inQuotes = false
            var count = 0
            var index = 0
            while (index < header.length) {
                val char = header[index]
                if (char == '"') {
                    if (inQuotes && index + 1 < header.length && header[index + 1] == '"') index++
                    else inQuotes = !inQuotes
                } else if (!inQuotes && char == delimiter) {
                    count++
                }
                index++
            }
            count
        } ?: ','
    }

    private fun parseCsvRecords(content: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var field = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            val char = content[index]
            if (inQuotes) {
                when {
                    char == '"' && index + 1 < content.length && content[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }
                    char == '"' -> inQuotes = false
                    else -> field.append(char)
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        row.add(field.toString())
                        field = StringBuilder()
                    }
                    '\r' -> {
                        row.add(field.toString())
                        field = StringBuilder()
                        if (index + 1 < content.length && content[index + 1] == '\n') index++
                        if (row.any { it.isNotEmpty() }) records.add(row)
                        row = mutableListOf()
                    }
                    '\n' -> {
                        row.add(field.toString())
                        field = StringBuilder()
                        if (row.any { it.isNotEmpty() }) records.add(row)
                        row = mutableListOf()
                    }
                    else -> field.append(char)
                }
            }
            index++
        }

        require(!inQuotes) { "Format CSV tidak valid: tanda kutip tidak tertutup." }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotEmpty() }) records.add(row)
        }
        return records
    }

    private fun parseSpreadsheetAmount(raw: String): Double {
        val value = raw.trim()
            .replace(Regex("""(?i)^rp"""), "")
            .replace(Regex("""[\s\u00A0]"""), "")
        require(value.isNotBlank()) { "nominal wajib diisi" }

        val negative = value.startsWith("-")
        val signedInput = value.startsWith("-") || value.startsWith("+")
        val unsigned = if (signedInput) value.substring(1) else value
        require(unsigned.isNotBlank() && unsigned.all { it.isDigit() || it == '.' || it == ',' }) {
            "format nominal tidak valid: $raw"
        }

        fun groupedDigits(integer: String, separator: Char?): String {
            if (separator == null || !integer.contains(separator)) {
                require(integer.matches(Regex("""\d+"""))) { "format nominal tidak valid: $raw" }
                return integer
            }
            val groups = integer.split(separator)
            require(
                groups.firstOrNull()?.matches(Regex("""\d{1,3}""")) == true &&
                    groups.drop(1).all { it.matches(Regex("""\d{3}""")) }
            ) { "pemisah ribuan nominal tidak valid: $raw" }
            return groups.joinToString("")
        }

        val dotCount = unsigned.count { it == '.' }
        val commaCount = unsigned.count { it == ',' }
        val normalized = when {
            dotCount > 0 && commaCount > 0 -> {
                val decimalSeparator = if (unsigned.lastIndexOf(',') > unsigned.lastIndexOf('.')) ',' else '.'
                val groupingSeparator = if (decimalSeparator == ',') '.' else ','
                require(unsigned.count { it == decimalSeparator } == 1) {
                    "format desimal nominal tidak valid: $raw"
                }
                val decimalIndex = unsigned.lastIndexOf(decimalSeparator)
                val integerPart = unsigned.substring(0, decimalIndex)
                val fractionPart = unsigned.substring(decimalIndex + 1)
                require(fractionPart.matches(Regex("""\d{1,2}"""))) {
                    "angka desimal nominal harus 1-2 digit: $raw"
                }
                val integerDigits = groupedDigits(
                    integerPart,
                    groupingSeparator.takeIf { integerPart.contains(it) }
                )
                "$integerDigits.$fractionPart"
            }
            dotCount > 1 || commaCount > 1 -> {
                val separator = if (dotCount > 1) '.' else ','
                require(if (separator == '.') commaCount == 0 else dotCount == 0) {
                    "format nominal tidak valid: $raw"
                }
                groupedDigits(unsigned, separator)
            }
            dotCount == 1 || commaCount == 1 -> {
                val separator = if (dotCount == 1) '.' else ','
                val index = unsigned.indexOf(separator)
                val integerPart = unsigned.substring(0, index)
                val fractionPart = unsigned.substring(index + 1)
                require(integerPart.matches(Regex("""\d+""")) && fractionPart.matches(Regex("""\d+"""))) {
                    "format nominal tidak valid: $raw"
                }
                when {
                    fractionPart.length in 1..2 -> "$integerPart.$fractionPart"
                    fractionPart.length == 3 && integerPart.length <= 3 ->
                        groupedDigits(unsigned, separator)
                    else -> throw IllegalArgumentException("format nominal tidak valid: $raw")
                }
            }
            else -> {
                require(unsigned.matches(Regex("""\d+"""))) { "format nominal tidak valid: $raw" }
                unsigned
            }
        }

        val signed = if (negative) "-$normalized" else normalized
        return signed.toDoubleOrNull()
            ?.also { require(it.isFinite()) { "nominal tidak valid" } }
            ?: throw IllegalArgumentException("nominal tidak dapat dibaca: $raw")
    }

    private fun parseSpreadsheetBoolean(raw: String): Boolean =
        when (raw.trim().lowercase(Locale.getDefault())) {
            "", "0", "false", "no", "tidak", "n" -> false
            "1", "true", "yes", "ya", "y" -> true
            else -> throw IllegalArgumentException("nilai boolean tidak valid: " + raw)
        }

    private fun formatSpreadsheetTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    private fun parseSpreadsheetTimestamp(raw: String): Long {
        val value = raw.trim()
        value.toLongOrNull()?.let {
            require(it > 0L) { "timestamp harus lebih besar dari 0" }
            return it
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
            isLenient = false
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(value, position)
        require(
            parsed != null &&
                position.index == value.length &&
                formatter.format(parsed) == value
        ) { "timestamp tidak valid: $raw" }
        return parsed.time
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
