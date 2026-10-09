package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectPlanEntity
import com.example.data.model.HousingUnitEntity

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
        BankReconEntity::class,
        ProjectEntity::class,
        ProjectPlanEntity::class,
        HousingUnitEntity::class
    ],
    version = 6,
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
    abstract fun projectDao(): ProjectDao
    abstract fun projectPlanDao(): ProjectPlanDao
    abstract fun housingUnitDao(): HousingUnitDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN fundBucket TEXT NOT NULL DEFAULT 'PT'")
                database.execSQL("ALTER TABLE budgets ADD COLUMN project TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE budgets ADD COLUMN fundBucket TEXT NOT NULL DEFAULT 'PT'")
                database.execSQL("ALTER TABLE employees ADD COLUMN defaultProject TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE attendances ADD COLUMN project TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE cash_notes ADD COLUMN project TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE TABLE IF NOT EXISTS projects (id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, businessModel TEXT NOT NULL, location TEXT NOT NULL, startDate TEXT NOT NULL, targetEndDate TEXT NOT NULL, budgetAmount REAL NOT NULL, status TEXT NOT NULL, notes TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                database.execSQL("CREATE TABLE IF NOT EXISTS project_plans (id TEXT NOT NULL, project TEXT NOT NULL, planDate TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, estimatedAmount REAL NOT NULL, status TEXT NOT NULL, details TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS housing_units (id TEXT NOT NULL, project TEXT NOT NULL, unitCode TEXT NOT NULL, block TEXT NOT NULL, sitePosition TEXT NOT NULL, businessModel TEXT NOT NULL, landAreaM2 REAL NOT NULL, buildingAreaM2 REAL NOT NULL, salePrice REAL NOT NULL, buyerName TEXT NOT NULL, status TEXT NOT NULL, notes TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE receivables ADD COLUMN project TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE receivables ADD COLUMN fundBucket TEXT NOT NULL DEFAULT 'PT'")
                cleanupLegacySeedRows(database)
            }
        }

        /**
         * Removes only rows matching the exact bootstrap records from the old v3
         * DatabaseCallback. Existing transactions and records derived from actual
         * work are retained; referenced seeded accounts have their fictitious opening
         * balance zeroed rather than being deleted.
         */
        internal fun cleanupLegacySeedRows(database: SupportSQLiteDatabase) {
            val hasLegacyBootstrapMarker = database.compileStatement(
                "SELECT COUNT(*) FROM audit_logs WHERE action='INITIALIZE_SYSTEM' AND recordId='SYS-SETUP'"
            ).simpleQueryForLong() > 0L
            if (!hasLegacyBootstrapMarker) return

            database.execSQL(
                "DELETE FROM attendances WHERE " +
                    "(employeeId='EMP-001' AND employeeName='Ahmad Fauzi' AND department='Keuangan' AND " +
                    "timeIn='07:55' AND timeOut='17:05' AND status='Hadir' AND overtimeHours=1.0 AND dailyAllowance=150000.0 AND notes='Tepat waktu' AND id GLOB 'ATT-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-001') OR " +
                    "(employeeId='EMP-002' AND employeeName='Siti Rahmawati' AND department='Keuangan' AND " +
                    "timeIn='07:50' AND timeOut='17:15' AND status='Hadir' AND overtimeHours=0.5 AND dailyAllowance=175000.0 AND notes='Rekonsiliasi bank' AND id GLOB 'ATT-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-002') OR " +
                    "(employeeId='EMP-003' AND employeeName='Budi Santoso' AND department='Operasional' AND " +
                    "timeIn='08:10' AND timeOut='17:00' AND status='Hadir' AND overtimeHours=0.0 AND dailyAllowance=160000.0 AND notes='Kunjungan gudang' AND id GLOB 'ATT-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-003') OR " +
                    "(employeeId='EMP-004' AND employeeName='Dewi Lestari' AND department='Umum' AND " +
                    "timeIn='08:00' AND timeOut='17:00' AND status='Hadir' AND overtimeHours=0.0 AND dailyAllowance=140000.0 AND notes='Piket kantor' AND id GLOB 'ATT-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-004') OR " +
                    "(employeeId='EMP-005' AND employeeName='Rian Pratama' AND department='Proyek' AND " +
                    "timeIn='-' AND timeOut='-' AND status='Izin' AND overtimeHours=0.0 AND dailyAllowance=0.0 AND notes='Izin keperluan dinas luar' AND id GLOB 'ATT-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-005')"
            )

            database.execSQL(
                "DELETE FROM budgets WHERE project='' AND fundBucket='PT' AND (" +
                    "(category='Operasional' AND budgetAmount=6000000.0 AND notes='Kebutuhan operasional kantor harian') OR " +
                    "(category='Gaji' AND budgetAmount=18000000.0 AND notes='Gaji & tunjangan kehadiran karyawan') OR " +
                    "(category='Marketing' AND budgetAmount=3500000.0 AND notes='Promosi dan iklan digital') OR " +
                    "(category='Proyek' AND budgetAmount=12000000.0 AND notes='Alokasi material & pengerjaan proyek') OR " +
                    "(category='Cadangan' AND budgetAmount=3000000.0 AND notes='Dana tak terduga perusahaan'))"
            )

            database.execSQL("DELETE FROM audit_logs WHERE action='INITIALIZE_SYSTEM' AND recordId='SYS-SETUP'")

            database.execSQL(
                "DELETE FROM employees WHERE isActive=1 AND defaultProject='' AND (" +
                    "(id='EMP-001' AND name='Ahmad Fauzi' AND position='Staff Kasir & Keuangan' AND department='Keuangan' AND phone='6281234567890' AND dailyRate=150000.0 AND monthlySalary=3800000.0) OR " +
                    "(id='EMP-002' AND name='Siti Rahmawati' AND position='Bendahara Kas' AND department='Keuangan' AND phone='6281987654321' AND dailyRate=175000.0 AND monthlySalary=4500000.0) OR " +
                    "(id='EMP-003' AND name='Budi Santoso' AND position='Koordinator Operasional' AND department='Operasional' AND phone='6282112233445' AND dailyRate=160000.0 AND monthlySalary=4200000.0) OR " +
                    "(id='EMP-004' AND name='Dewi Lestari' AND position='Staff Administrasi & HR' AND department='Umum' AND phone='6285778899001' AND dailyRate=140000.0 AND monthlySalary=3600000.0) OR " +
                    "(id='EMP-005' AND name='Rian Pratama' AND position='Staff Logistik & Proyek' AND department='Proyek' AND phone='6287890123456' AND dailyRate=150000.0 AND monthlySalary=3800000.0)" +
                    ") AND NOT EXISTS (SELECT 1 FROM attendances WHERE attendances.employeeId=employees.id)"
            )

            val seedAccountPredicate =
                "(id='acc_tunai' AND name='Kas Tunai' AND initialBalance=2500000.0) OR " +
                "(id='acc_bca' AND name='Bank BCA' AND initialBalance=12500000.0) OR " +
                "(id='acc_bri' AND name='Bank BRI' AND initialBalance=7500000.0) OR " +
                "(id='acc_mandiri' AND name='Bank Mandiri' AND initialBalance=8000000.0) OR " +
                "(id='acc_besar' AND name='Kas Besar' AND initialBalance=15000000.0)"
            database.execSQL(
                "DELETE FROM accounts WHERE (" + seedAccountPredicate + ") " +
                    "AND NOT EXISTS (SELECT 1 FROM transactions t WHERE " +
                        "LOWER(TRIM(t.account))=LOWER(TRIM(accounts.name)) OR " +
                        "LOWER(TRIM(COALESCE(t.toAccount,'')))=LOWER(TRIM(accounts.name))) " +
                    "AND NOT EXISTS (SELECT 1 FROM receivables r WHERE LOWER(TRIM(r.targetAccount))=LOWER(TRIM(accounts.name))) " +
                    "AND NOT EXISTS (SELECT 1 FROM bank_reconciliations b WHERE LOWER(TRIM(b.accountName))=LOWER(TRIM(accounts.name)))"
            )
            database.execSQL("UPDATE accounts SET initialBalance=0.0 WHERE (" + seedAccountPredicate + ")")
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sistem_kas_db"
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { INSTANCE = it }
            }
        }
    }
}
