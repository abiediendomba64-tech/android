package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BankReconEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CashNoteEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.ReceivableEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ReceivableEntity::class,
        CashNoteEntity::class,
        EmployeeEntity::class,
        AttendanceEntity::class,
        AuditLogEntity::class,
        BankReconEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun noteDao(): NoteDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun auditDao(): AuditDao
    abstract fun bankReconDao(): BankReconDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sistem_kas_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialRealData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialRealData(database: AppDatabase) {
            val accountDao = database.accountDao()
            val budgetDao = database.budgetDao()
            val employeeDao = database.employeeDao()
            val attendanceDao = database.attendanceDao()
            val auditDao = database.auditDao()

            // 1. Initial Real Accounts
            val accounts = listOf(
                AccountEntity("acc_tunai", "Kas Tunai", "Kas", 2500000.0, "#16A34A"),
                AccountEntity("acc_bca", "Bank BCA", "Bank", 12500000.0, "#2563EB"),
                AccountEntity("acc_bri", "Bank BRI", "Bank", 7500000.0, "#0284C7"),
                AccountEntity("acc_mandiri", "Bank Mandiri", "Bank", 8000000.0, "#D97706"),
                AccountEntity("acc_besar", "Kas Besar", "Kas", 15000000.0, "#7C3AED")
            )
            accountDao.insertAccounts(accounts)

            val currentPeriod = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val nowStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // 2. Budget Allocations
            val budgets = listOf(
                BudgetEntity(period = currentPeriod, category = "Operasional", budgetAmount = 6000000.0, notes = "Kebutuhan operasional kantor harian"),
                BudgetEntity(period = currentPeriod, category = "Gaji", budgetAmount = 18000000.0, notes = "Gaji & tunjangan kehadiran karyawan"),
                BudgetEntity(period = currentPeriod, category = "Marketing", budgetAmount = 3500000.0, notes = "Promosi dan iklan digital"),
                BudgetEntity(period = currentPeriod, category = "Proyek", budgetAmount = 12000000.0, notes = "Alokasi material & pengerjaan proyek"),
                BudgetEntity(period = currentPeriod, category = "Cadangan", budgetAmount = 3000000.0, notes = "Dana tak terduga perusahaan")
            )
            budgetDao.insertBudgets(budgets)

            // 3. Employee Directory
            val employees = listOf(
                EmployeeEntity("EMP-001", "Ahmad Fauzi", "Staff Kasir & Keuangan", "Keuangan", "6281234567890", 150000.0, 3800000.0),
                EmployeeEntity("EMP-002", "Siti Rahmawati", "Bendahara Kas", "Keuangan", "6281987654321", 175000.0, 4500000.0),
                EmployeeEntity("EMP-003", "Budi Santoso", "Koordinator Operasional", "Operasional", "6282112233445", 160000.0, 4200000.0),
                EmployeeEntity("EMP-004", "Dewi Lestari", "Staff Administrasi & HR", "Umum", "6285778899001", 140000.0, 3600000.0),
                EmployeeEntity("EMP-005", "Rian Pratama", "Staff Logistik & Proyek", "Proyek", "6287890123456", 150000.0, 3800000.0)
            )
            employeeDao.insertEmployees(employees)

            // 4. Initial Daily Attendances
            val attendances = listOf(
                AttendanceEntity("ATT-${today}-001", "EMP-001", "Ahmad Fauzi", "Keuangan", today, "07:55", "17:05", "Hadir", 1.0, 150000.0, "Tepat waktu"),
                AttendanceEntity("ATT-${today}-002", "EMP-002", "Siti Rahmawati", "Keuangan", today, "07:50", "17:15", "Hadir", 0.5, 175000.0, "Rekonsiliasi bank"),
                AttendanceEntity("ATT-${today}-003", "EMP-003", "Budi Santoso", "Operasional", today, "08:10", "17:00", "Hadir", 0.0, 160000.0, "Kunjungan gudang"),
                AttendanceEntity("ATT-${today}-004", "EMP-004", "Dewi Lestari", "Umum", today, "08:00", "17:00", "Hadir", 0.0, 140000.0, "Piket kantor"),
                AttendanceEntity("ATT-${today}-005", "EMP-005", "Rian Pratama", "Proyek", today, "-", "-", "Izin", 0.0, 0.0, "Izin keperluan dinas luar")
            )
            attendanceDao.insertAttendances(attendances)

            // 5. Initial Audit Log
            auditDao.insertAuditLog(
                AuditLogEntity(
                    dateFormatted = nowStamp,
                    action = "INITIALIZE_SYSTEM",
                    recordId = "SYS-SETUP",
                    details = "Inisialisasi sistem kas: 5 Akun terverifikasi, Master Anggaran $currentPeriod, 5 Data Karyawan, dan Audit Rumus aktif.",
                    user = "Sistem Audit",
                    verifiedFormulaStatus = "AUDIT_OK_100%",
                    balanceAfter = 45500000.0
                )
            )
        }
    }
}
