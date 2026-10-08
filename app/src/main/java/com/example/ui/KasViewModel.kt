package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BankReconEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CashNoteEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.ReceivableEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.AccountWithBalance
import com.example.data.repository.AttendanceSummary
import com.example.data.repository.BudgetRealization
import com.example.data.repository.CashFlowStatement
import com.example.data.repository.DashboardKpis
import com.example.data.repository.KasRepository
import com.example.data.repository.PayrollCalculation
import com.example.ui.components.formatRupiah
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KasRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KasRepository(
            database = database,
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            budgetDao = database.budgetDao(),
            receivableDao = database.receivableDao(),
            noteDao = database.noteDao(),
            employeeDao = database.employeeDao(),
            attendanceDao = database.attendanceDao(),
            auditDao = database.auditDao(),
            bankReconDao = database.bankReconDao()
        )
    }

    val activeTransactions: StateFlow<List<TransactionEntity>> = repository.activeTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedTransactions: StateFlow<List<TransactionEntity>> = repository.archivedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivables: StateFlow<List<ReceivableEntity>> = repository.receivables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<CashNoteEntity>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeEntity>> = repository.employees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allEmployees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendances: StateFlow<List<AttendanceEntity>> = repository.attendances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankReconciliations: StateFlow<List<BankReconEntity>> = repository.bankReconciliations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance Filter State
    val attendanceFilterMode = MutableStateFlow("Harian") // "Harian", "Bulanan", "Tahunan", "Periodik"
    val attendanceSelectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val attendanceSelectedMonth = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    val attendanceSelectedYear = MutableStateFlow(SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()))
    val attendanceStartDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(Date()))
    val attendanceEndDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

    val filteredAttendances: StateFlow<List<AttendanceEntity>> = combine(
        attendances,
        attendanceFilterMode,
        attendanceSelectedDate,
        attendanceSelectedMonth,
        combine(attendanceSelectedYear, attendanceStartDate, attendanceEndDate) { y, s, e -> Triple(y, s, e) }
    ) { list, mode, date, month, (year, start, end) ->
        when (mode) {
            "Harian" -> list.filter { it.date == date }
            "Bulanan" -> list.filter { it.date.startsWith(month) }
            "Tahunan" -> list.filter { it.date.startsWith(year) }
            "Periodik" -> list.filter { it.date in start..end }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceSummary: StateFlow<AttendanceSummary> = filteredAttendances.combine(
        filteredAttendances
    ) { list, _ ->
        repository.calculateAttendanceSummary(list)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AttendanceSummary(0, 0, 0, 0, 0, 0, 0.0, 0.0)
    )

    // Live account balances are derived from the local ledger.
    val ledgerTransactions: StateFlow<List<TransactionEntity>> = repository.ledgerTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = combine(
        accounts,
        ledgerTransactions
    ) { accList, txList ->
        repository.calculateAccountBalances(accList, txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard KPIs (Total Saldo, Masuk/Keluar Hari Ini, Net, Bulan Ini)
    val dashboardKpis: StateFlow<DashboardKpis> = combine(
        accountsWithBalance,
        ledgerTransactions
    ) { accBalances, txList ->
        repository.calculateDashboardKpis(accBalances, txList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardKpis(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    // Budget Realizations (% used, remaining, OVER alert)
    val budgetRealizations: StateFlow<List<BudgetRealization>> = combine(
        budgets,
        ledgerTransactions
    ) { bList, txList ->
        repository.calculateBudgetRealizations(bList, txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast / Event message channel
    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage

    // Master dropdown lists used by the app.
    val masterKategoriMasuk = listOf(
        "Penjualan", "Piutang Masuk", "Modal", "Pendapatan Lain", "Transfer Masuk", "Lainnya"
    )

    val masterKategoriKeluar = listOf(
        "Belanja Barang", "Gaji", "Transportasi", "Listrik/Internet", "Sewa", "Operasional", "Marketing", "Proyek", "Lainnya"
    )

    val masterAlokasi = listOf(
        "Operasional", "Gaji", "Marketing", "Proyek", "Cadangan", "Lainnya"
    )

    val masterPic = listOf(
        "Admin", "Bendahara", "Keuangan", "Manager", "PIC Operasional"
    )

    val masterStatus = listOf(
        "Selesai", "Draft", "Pending", "Batal"
    )

    // Settings state
    val companyName = MutableStateFlow("Sistem Kas")

    // Form submission methods
    fun simpanKasMasuk(
        date: String,
        account: String,
        name: String,
        category: String,
        description: String,
        amount: Double,
        allocation: String,
        pic: String,
        proofUrl: String = "",
        receiptNo: String = "",
        project: String = "",
        note: String = "",
        status: String = "Selesai",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val id = repository.generateId("KM")
            val tx = TransactionEntity(
                id = id,
                type = "MASUK",
                date = date,
                time = timeFormat.format(Date()),
                account = account,
                name = name,
                category = category,
                description = description,
                amount = amount,
                allocation = allocation,
                pic = pic,
                proofUrl = proofUrl,
                receiptNo = receiptNo,
                project = project,
                note = note,
                status = status
            )
            repository.saveTransaction(tx)
            _snackBarMessage.emit("Kas Masuk tersimpan: $id (Tercatat dalam Log Audit)")
            onSuccess()
        }
    }

    fun simpanKasKeluar(
        date: String,
        account: String,
        name: String,
        category: String,
        description: String,
        amount: Double,
        allocation: String,
        pic: String,
        proofUrl: String = "",
        receiptNo: String = "",
        project: String = "",
        note: String = "",
        status: String = "Selesai",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val id = repository.generateId("KK")
            val tx = TransactionEntity(
                id = id,
                type = "KELUAR",
                date = date,
                time = timeFormat.format(Date()),
                account = account,
                name = name,
                category = category,
                description = description,
                amount = amount,
                allocation = allocation,
                pic = pic,
                proofUrl = proofUrl,
                receiptNo = receiptNo,
                project = project,
                note = note,
                status = status
            )
            repository.saveTransaction(tx)
            _snackBarMessage.emit("Kas Keluar tersimpan: $id (Tercatat dalam Log Audit)")
            onSuccess()
        }
    }

    fun simpanTransfer(
        date: String,
        fromAccount: String,
        toAccount: String,
        amount: Double,
        description: String,
        pic: String,
        proofUrl: String = "",
        receiptNo: String = "",
        note: String = "",
        status: String = "Selesai",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (fromAccount == toAccount) {
                _snackBarMessage.emit("Akun asal dan akun tujuan tidak boleh sama.")
                return@launch
            }
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val id = repository.generateId("TR")
            val tx = TransactionEntity(
                id = id,
                type = "TRANSFER",
                date = date,
                time = timeFormat.format(Date()),
                account = fromAccount,
                toAccount = toAccount,
                name = "Transfer: $fromAccount ➔ $toAccount",
                category = "Transfer",
                description = description.ifBlank { "Transfer saldo antar akun kas" },
                amount = amount,
                allocation = "Lainnya",
                pic = pic,
                proofUrl = proofUrl,
                receiptNo = receiptNo,
                note = note,
                status = status
            )
            repository.saveTransaction(tx)
            _snackBarMessage.emit("Transfer tersimpan: $id")
            onSuccess()
        }
    }

    fun arsipkanTransaksi(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.archiveTransaction(tx.id)
            _snackBarMessage.emit("Transaksi ${tx.id} dipindahkan ke Transaksi_Dihapus (Audit Trail aktif).")
        }
    }

    fun editTransaksi(newTx: TransactionEntity, oldTx: TransactionEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTransaction(newTx, oldTx)
            _snackBarMessage.emit("Transaksi ${newTx.id} berhasil diperbarui (Tercatat dalam Log Audit).")
            onSuccess()
        }
    }

    fun pulihkanTransaksi(id: String) {
        viewModelScope.launch {
            repository.restoreTransaction(id)
            _snackBarMessage.emit("Transaksi $id berhasil dipulihkan.")
        }
    }

    fun hapusPermanen(id: String) {
        viewModelScope.launch {
            _snackBarMessage.emit("Penghapusan permanen transaksi dinonaktifkan untuk menjaga histori ledger.")
        }
    }

    fun kosongkanArsip() {
        viewModelScope.launch {
            _snackBarMessage.emit("Pengosongan arsip permanen dinonaktifkan agar histori ledger tetap utuh.")
        }
    }

    fun tambahAkun(name: String, type: String, initialBalance: Double, colorHex: String) {
        viewModelScope.launch {
            val id = repository.generateId("ACC")
            val acc = AccountEntity(id, name, type, initialBalance, colorHex)
            repository.saveAccount(acc)
            _snackBarMessage.emit("Akun $name berhasil ditambahkan.")
        }
    }

    fun hapusAkun(id: String) {
        viewModelScope.launch {
            val physicallyDeleted = repository.deleteAccount(id)
            _snackBarMessage.emit(
                if (physicallyDeleted) "Akun berhasil dihapus."
                else "Akun dinonaktifkan karena masih memiliki histori data."
            )
        }
    }

    fun tambahAnggaran(period: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.saveBudget(BudgetEntity(period = period, category = category, budgetAmount = amount, notes = notes))
            _snackBarMessage.emit("Alokasi anggaran untuk $category ($period) berhasil disimpan.")
        }
    }

    fun hapusAnggaran(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _snackBarMessage.emit("Anggaran dihapus.")
        }
    }

    fun tambahPiutang(
        customerName: String,
        description: String,
        totalAmount: Double,
        dueDate: String,
        targetAccount: String,
        notes: String
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val id = repository.generateId("PT")
            val r = ReceivableEntity(
                id = id,
                date = today,
                customerName = customerName,
                description = description,
                totalAmount = totalAmount,
                dueDate = dueDate,
                targetAccount = targetAccount,
                notes = notes,
                status = "Belum Jatuh Tempo"
            )
            repository.saveReceivable(r)
            _snackBarMessage.emit("Piutang $customerName ($id) berhasil dicatat.")
        }
    }

    fun bayarPiutang(id: String, amount: Double, targetAccount: String) {
        viewModelScope.launch {
            repository.payReceivable(id, amount, targetAccount)
            _snackBarMessage.emit("Pelunasan piutang Rp ${amount.toLong()} dicatat ke $targetAccount.")
        }
    }

    fun hapusPiutang(id: String) {
        viewModelScope.launch {
            repository.deleteReceivable(id)
            _snackBarMessage.emit("Data piutang/hutang berhasil dihapus dari basis data.")
        }
    }

    fun tambahHutang(
        supplierName: String,
        description: String,
        totalAmount: Double,
        dueDate: String,
        sourceAccount: String,
        notes: String
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val id = repository.generateId("HT")
            val r = ReceivableEntity(
                id = id,
                type = "HUTANG",
                date = today,
                customerName = supplierName,
                description = description,
                totalAmount = totalAmount,
                dueDate = dueDate,
                targetAccount = sourceAccount,
                notes = notes,
                status = "Belum Jatuh Tempo"
            )
            repository.saveReceivable(r)
            _snackBarMessage.emit("Hutang kepada $supplierName ($id) berhasil dicatat.")
        }
    }

    fun bayarHutang(id: String, amount: Double, sourceAccount: String) {
        viewModelScope.launch {
            repository.payHutang(id, amount, sourceAccount)
            _snackBarMessage.emit("Pembayaran hutang Rp ${amount.toLong()} dicatat dari $sourceAccount (Kas Keluar).")
        }
    }

    fun tambahCatatan(title: String, content: String, pic: String, priority: String, status: String) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.saveNote(CashNoteEntity(date = today, title = title, content = content, pic = pic, priority = priority, status = status))
            _snackBarMessage.emit("Catatan kas disimpan.")
        }
    }

    fun hapusCatatan(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            _snackBarMessage.emit("Catatan dihapus.")
        }
    }

    // Employee & Attendance Operations
    fun tambahKaryawan(
        name: String,
        position: String,
        department: String,
        phone: String,
        dailyRate: Double,
        monthlySalary: Double
    ) {
        viewModelScope.launch {
            val id = repository.generateId("EMP")
            val emp = EmployeeEntity(id, name, position, department, phone, dailyRate, monthlySalary)
            repository.saveEmployee(emp)
            _snackBarMessage.emit("Data karyawan $name ($id) berhasil ditambahkan.")
        }
    }

    fun catatAbsensi(
        employeeId: String,
        employeeName: String,
        department: String,
        date: String,
        timeIn: String,
        timeOut: String,
        status: String,
        overtime: Double,
        allowance: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val id = "ATT-${date.replace("-", "")}-$employeeId"
            val att = AttendanceEntity(
                id = id,
                employeeId = employeeId,
                employeeName = employeeName,
                department = department,
                date = date,
                timeIn = timeIn,
                timeOut = timeOut,
                status = status,
                overtimeHours = overtime,
                dailyAllowance = allowance,
                notes = notes
            )
            repository.saveAttendance(att)
            _snackBarMessage.emit("Absensi $employeeName ($status) tanggal $date berhasil dicatat.")
        }
    }

    fun absensiCepatSemuaHadir(date: String) {
        viewModelScope.launch {
            repository.bulkMarkAllEmployeesHadir(date)
            _snackBarMessage.emit("Seluruh karyawan berhasil diabsen Hadir untuk tanggal $date.")
        }
    }

    fun hapusAbsensi(id: String) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
            _snackBarMessage.emit("Catatan absensi berhasil dihapus.")
        }
    }

    fun jalankanAuditSistem(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.runIntegrityAudit(
                accounts = accounts.value,
                transactions = ledgerTransactions.value,
                budgets = budgets.value,
                receivables = receivables.value,
                employees = allEmployees.value,
                attendances = attendances.value
            )
            val accountsSnapshot = accountsWithBalance.value
            val totalSaldo = accountsSnapshot.sumOf { it.currentBalance }
            val report = buildString {
                appendLine("*HASIL PEMERIKSAAN INTEGRITAS SISTEM KAS*")
                appendLine("Waktu Pemeriksaan: ${result.checkedAt}")
                appendLine("Status: ${if (result.passed) "LULUS" else "GAGAL"}")
                appendLine("Total saldo ledger: ${formatRupiah(totalSaldo)}")
                appendLine("Transaksi ledger: ${ledgerTransactions.value.size}")
                if (result.issues.isEmpty()) {
                    appendLine("Tidak ditemukan ketidaksesuaian data pada pemeriksaan ini.")
                } else {
                    appendLine("Temuan:")
                    result.issues.take(50).forEach { appendLine("- $it") }
                    if (result.issues.size > 50) appendLine("- ... ${result.issues.size - 50} temuan lainnya")
                }
                appendLine("Pemeriksaan dilakukan terhadap ledger lokal aplikasi.")
            }
            repository.logDirectAudit(
                AuditLogEntity(
                    dateFormatted = result.checkedAt,
                    action = if (result.passed) "SYSTEM_AUDIT_PASS" else "SYSTEM_AUDIT_FAIL",
                    recordId = "AUDIT-" + System.currentTimeMillis().toString().takeLast(8),
                    details = "Pemeriksaan integritas: ${result.issues.size} temuan; ${ledgerTransactions.value.size} transaksi ledger; saldo ${formatRupiah(totalSaldo)}.",
                    user = "Sistem Audit",
                    verifiedFormulaStatus = if (result.passed) "AUDIT_PASS" else "AUDIT_FAIL",
                    balanceAfter = totalSaldo
                )
            )
            _snackBarMessage.emit(
                if (result.passed) "Audit selesai: tidak ditemukan ketidaksesuaian."
                else "Audit selesai: ditemukan ${result.issues.size} ketidaksesuaian. Periksa hasil audit."
            )
            onComplete(report)
        }
    }
    fun hitungPayrollKaryawan(employee: EmployeeEntity, attendances: List<AttendanceEntity>): PayrollCalculation =
        repository.calculatePayrollForEmployee(employee, attendances)

    fun hitungTotalPayroll(employees: List<EmployeeEntity>, attendances: List<AttendanceEntity>): Double =
        repository.calculatePayrollTotal(employees, attendances)

    fun cairkanGajiAbsensiKeKasKeluar(
        period: String,
        totalGaji: Double,
        accountName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val payrollTotal = repository.calculatePayrollTotal(employees.value, filteredAttendances.value)
            require(totalGaji.isFinite() && totalGaji > 0.0) { "Total payroll harus lebih besar dari Rp 0." }
            require(kotlin.math.abs(payrollTotal - totalGaji) < 0.01) {
                "Total pencairan payroll tidak sama dengan perhitungan slip; pencairan dibatalkan."
            }
            repository.savePayrollDisbursement(period, payrollTotal, accountName)
            _snackBarMessage.emit("Payroll sebesar ${formatRupiah(payrollTotal)} berhasil dicairkan ke Kas Keluar.")
            onSuccess()
        }
    }

    // Bank Reconciliation Operations
    fun hitungSaldoBukuRekonsiliasi(accountName: String, period: String): Double {
        val account = accounts.value.firstOrNull { it.name.equals(accountName, ignoreCase = true) } ?: return 0.0
        return repository.calculateAccountBalanceAtPeriod(account, period, ledgerTransactions.value)
    }

    fun simpanRekonsiliasiBank(
        accountName: String,
        period: String,
        statementBalance: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val id = "REC-${accountName.replace(" ", "_")}-${period}"
            val recon = BankReconEntity(
                id = id,
                accountName = accountName,
                period = period,
                bookBalance = 0.0,
                statementBalance = statementBalance,
                difference = 0.0,
                status = "Belum Diverifikasi",
                notes = notes
            )
            repository.saveBankReconciliation(recon)
            val saved = bankReconciliations.value.firstOrNull { it.id == id }
            val status = saved?.status ?: "Tersimpan"
            val diff = saved?.difference ?: 0.0
            _snackBarMessage.emit("Rekonsiliasi $accountName periode $period disimpan ($status - Selisih: ${formatRupiah(diff)}).")
        }
    }

    fun hitungArusKas(startDate: String, endDate: String): CashFlowStatement {
        return repository.calculateCashFlowStatement(
            transactions = ledgerTransactions.value,
            accounts = accounts.value,
            startDate = startDate,
            endDate = endDate
        )
    }

    // Send to WhatsApp Intent
    fun kirimPesanWhatsApp(context: Context, text: String, targetPhone: String = "") {
        val url = repository.formatWhatsAppText(text, targetPhone)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share intent if WhatsApp is not directly resolvable
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim via WhatsApp / Bagikan"))
        }
    }

    suspend fun exportJson(): String {
        return repository.exportDataToJson(
            transactions = activeTransactions.value,
            accounts = accounts.value,
            budgets = budgets.value,
            receivables = receivables.value,
            notes = notes.value
        )
    }

    suspend fun restoreFromJson(jsonStr: String): Result<Int> {
        return repository.restoreDataFromJson(jsonStr)
    }

    fun exportCsv(): String {
        return repository.exportTransactionsToCsv(activeTransactions.value)
    }

    suspend fun importCsv(csvContent: String): Result<Int> {
        val result = repository.importTransactionsFromCsv(csvContent)
        if (result.isSuccess) {
            _snackBarMessage.emit("Berhasil mengimpor ${result.getOrNull()} transaksi dari spreadsheet CSV.")
        }
        return result
    }

    fun generatePrintableReport(): String {
        return repository.generatePrintableSummaryText(
            companyName = companyName.value,
            accounts = accountsWithBalance.value,
            transactions = ledgerTransactions.value
        )
    }

    fun hapusKaryawan(id: String) {
        viewModelScope.launch {
            val physicallyDeleted = repository.deleteEmployee(id)
            _snackBarMessage.emit(
                if (physicallyDeleted) "Data karyawan berhasil dihapus."
                else "Karyawan dinonaktifkan karena masih memiliki histori absensi."
            )
        }
    }
}
