package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {

    @Test
    fun migrationFromRealVersionFiveFilePreservesLedgerAndAddsReceivableScopeDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-v5-integrity-test.db"
        context.deleteDatabase(databaseName)

        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        try {
            val schema = listOf(
                """CREATE TABLE accounts (id TEXT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, initialBalance REAL NOT NULL, colorHex TEXT NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id))""",
                """CREATE TABLE transactions (id TEXT NOT NULL, type TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, account TEXT NOT NULL, toAccount TEXT, name TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, amount REAL NOT NULL, allocation TEXT NOT NULL, pic TEXT NOT NULL, proofUrl TEXT NOT NULL, receiptNo TEXT NOT NULL, project TEXT NOT NULL, note TEXT NOT NULL, status TEXT NOT NULL, inputTime INTEGER NOT NULL, inputBy TEXT NOT NULL, isArchived INTEGER NOT NULL, archivedAt INTEGER, archivedBy TEXT, fundBucket TEXT NOT NULL DEFAULT 'PT', PRIMARY KEY(id))""",
                """CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, period TEXT NOT NULL, category TEXT NOT NULL, budgetAmount REAL NOT NULL, notes TEXT NOT NULL, project TEXT NOT NULL DEFAULT '', fundBucket TEXT NOT NULL DEFAULT 'PT')""",
                """CREATE TABLE receivables (id TEXT NOT NULL, type TEXT NOT NULL, date TEXT NOT NULL, customerName TEXT NOT NULL, description TEXT NOT NULL, totalAmount REAL NOT NULL, dueDate TEXT NOT NULL, paidAmount REAL NOT NULL, targetAccount TEXT NOT NULL, notes TEXT NOT NULL, status TEXT NOT NULL, PRIMARY KEY(id))""",
                """CREATE TABLE cash_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, pic TEXT NOT NULL, priority TEXT NOT NULL, status TEXT NOT NULL, project TEXT NOT NULL DEFAULT '')""",
                """CREATE TABLE employees (id TEXT NOT NULL, name TEXT NOT NULL, position TEXT NOT NULL, department TEXT NOT NULL, phone TEXT NOT NULL, dailyRate REAL NOT NULL, monthlySalary REAL NOT NULL, isActive INTEGER NOT NULL, defaultProject TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))""",
                """CREATE TABLE attendances (id TEXT NOT NULL, employeeId TEXT NOT NULL, employeeName TEXT NOT NULL, department TEXT NOT NULL, date TEXT NOT NULL, timeIn TEXT NOT NULL, timeOut TEXT NOT NULL, status TEXT NOT NULL, overtimeHours REAL NOT NULL, dailyAllowance REAL NOT NULL, notes TEXT NOT NULL, project TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))""",
                """CREATE TABLE audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, dateFormatted TEXT NOT NULL, action TEXT NOT NULL, recordId TEXT NOT NULL, details TEXT NOT NULL, user TEXT NOT NULL, verifiedFormulaStatus TEXT NOT NULL, balanceAfter REAL NOT NULL)""",
                """CREATE TABLE bank_reconciliations (id TEXT NOT NULL, accountName TEXT NOT NULL, period TEXT NOT NULL, bookBalance REAL NOT NULL, statementBalance REAL NOT NULL, difference REAL NOT NULL, status TEXT NOT NULL, reconciledBy TEXT NOT NULL, reconciledAt INTEGER NOT NULL, notes TEXT NOT NULL, PRIMARY KEY(id))""",
                """CREATE TABLE projects (id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, businessModel TEXT NOT NULL, location TEXT NOT NULL, startDate TEXT NOT NULL, targetEndDate TEXT NOT NULL, budgetAmount REAL NOT NULL, status TEXT NOT NULL, notes TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))""",
                """CREATE TABLE project_plans (id TEXT NOT NULL, project TEXT NOT NULL, planDate TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, estimatedAmount REAL NOT NULL, status TEXT NOT NULL, details TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))""",
                """CREATE TABLE housing_units (id TEXT NOT NULL, project TEXT NOT NULL, unitCode TEXT NOT NULL, block TEXT NOT NULL, sitePosition TEXT NOT NULL, businessModel TEXT NOT NULL, landAreaM2 REAL NOT NULL, buildingAreaM2 REAL NOT NULL, salePrice REAL NOT NULL, buyerName TEXT NOT NULL, status TEXT NOT NULL, notes TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))"""
            )
            schema.forEach { legacy.execSQL(it) }

            legacy.execSQL(
                "INSERT INTO accounts (id,name,type,initialBalance,colorHex,isActive) VALUES (?,?,?,?,?,?)",
                arrayOf("acc-v5-keep", "Kas Legacy V5", "Kas", 500000.0, "#1E56A0", 1)
            )
            legacy.execSQL(
                """INSERT INTO transactions (id,type,date,time,account,toAccount,name,category,description,amount,allocation,pic,proofUrl,receiptNo,project,note,status,inputTime,inputBy,isArchived,archivedAt,archivedBy,fundBucket)
                   VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                arrayOf(
                    "TX-V5-KEEP", "KELUAR", "2026-09-30", "10:00:00", "Kas Legacy V5", null,
                    "Belanja sebelum migrasi", "Operasional", "Histori riil versi lama", 12000.0,
                    "Operasional", "Admin", "", "INV-V5", "", "", "Selesai", 1790812800000L,
                    "Legacy Migration Test", 0, null, null, "PT"
                )
            )
            legacy.execSQL(
                """INSERT INTO receivables (id,type,date,customerName,description,totalAmount,dueDate,paidAmount,targetAccount,notes,status)
                   VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                arrayOf(
                    "PIUT-V5-KEEP", "PIUTANG", "2026-09-25", "Klien Migrasi", "Tagihan historis",
                    50000.0, "2026-10-30", 0.0, "Kas Legacy V5", "", "Belum Jatuh Tempo"
                )
            )
            legacy.execSQL("PRAGMA user_version = 5")
        } finally {
            legacy.close()
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(6, migrated.openHelper.writableDatabase.version)
            val account = migrated.accountDao().getAccountById("acc-v5-keep")
            val transaction = migrated.transactionDao().getTransactionById("TX-V5-KEEP")
            val receivable = migrated.receivableDao().getReceivableById("PIUT-V5-KEEP")

            assertNotNull(account)
            assertNotNull(transaction)
            assertNotNull(receivable)
            assertEquals(500000.0, account?.initialBalance ?: 0.0, 0.01)
            assertEquals(12000.0, transaction?.amount ?: 0.0, 0.01)
            assertEquals("Kas Legacy V5", transaction?.account)
            assertEquals("", receivable?.project)
            assertEquals("PT", receivable?.fundBucket)
            assertEquals(50000.0, receivable?.totalAmount ?: 0.0, 0.01)
            assertEquals(1, migrated.accountDao().getAllAccounts().first().size)
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }
}
