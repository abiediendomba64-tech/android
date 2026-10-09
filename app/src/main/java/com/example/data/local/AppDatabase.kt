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
    version = 5,
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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sistem_kas_db"
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
        }
    }
}
