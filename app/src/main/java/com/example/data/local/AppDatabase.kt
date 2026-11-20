package com.example.data.local

import android.content.Context
import android.os.StatFs
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.dao.CalendarDayDao
import com.example.data.local.dao.CareerGoalDao
import com.example.data.local.dao.ClientDao
import com.example.data.local.dao.DashboardAggregateDao
import com.example.data.local.dao.EditorProfileDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.FormDraftDao
import com.example.data.local.dao.IncomeDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.ProjectRevisionDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.FormDraftEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectRevisionEntity
import com.example.util.DEFAULT_JOURNEY_START_DATE
import java.io.File

@Database(
    entities = [
        EditorProfileEntity::class,
        ClientEntity::class,
        ProjectEntity::class,
        CalendarDayEntity::class,
        IncomeTransactionEntity::class,
        ExpenseTransactionEntity::class,
        AppSettingsEntity::class,
        NoteEntity::class,
        ProjectRevisionEntity::class,
        CareerGoalEntity::class,
        FormDraftEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun editorProfileDao(): EditorProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun calendarDayDao(): CalendarDayDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun noteDao(): NoteDao
    abstract fun projectRevisionDao(): ProjectRevisionDao
    abstract fun careerGoalDao(): CareerGoalDao
    abstract fun formDraftDao(): FormDraftDao
    abstract fun dashboardAggregateDao(): DashboardAggregateDao

    fun runIntegrityCheck(): Pair<Boolean, String> {
        return runCatching {
            val db = openHelper.writableDatabase
            val results = mutableListOf<String>()
            db.query("PRAGMA quick_check").use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursor.getString(0))
                }
            }
            val ok = results.size == 1 && results[0].equals("ok", ignoreCase = true)
            ok to if (ok) "SQLite integrity check passed (OK)" else results.joinToString("; ")
        }.getOrElse { err ->
            false to (err.localizedMessage ?: "Integrity check failed")
        }
    }

    fun walCheckpoint() {
        runCatching {
            openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }
    }

    companion object {
        const val DB_NAME = "rohan_edits_career.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Covering indexes on existing tables (zero data loss)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_projectStatus_projectDateEpochDay ON projects(projectStatus, projectDateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_projectStatus_projectAmount_amountPaid ON projects(projectStatus, projectAmount, amountPaid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_clientId_projectStatus ON projects(clientId, projectStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_category_projectStatus_projectDateEpochDay ON projects(category, projectStatus, projectDateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_title ON projects(title)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_days_dateEpochDay_status ON calendar_days(dateEpochDay, status)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_dateEpochDay_amount ON income_transactions(dateEpochDay, amount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_clientId_amount ON income_transactions(clientId, amount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_projectId_amount ON income_transactions(projectId, amount)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_transactions_dateEpochDay_amount ON expense_transactions(dateEpochDay, amount)")

                // Notes table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL,
                        clientId INTEGER,
                        projectId INTEGER,
                        isPinned INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(clientId) REFERENCES clients(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isPinned_updatedAtMillis ON notes(isPinned, updatedAtMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_category ON notes(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_clientId ON notes(clientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_projectId ON notes(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_title ON notes(title)")

                // Project Revisions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS project_revisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        versionNumber INTEGER NOT NULL,
                        summary TEXT NOT NULL,
                        instructionsSnapshot TEXT NOT NULL,
                        statusSnapshot TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_revisions_projectId_versionNumber ON project_revisions(projectId, versionNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_revisions_projectId_createdAtMillis ON project_revisions(projectId, createdAtMillis)")

                // Career Goals table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS career_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        targetMetric TEXT NOT NULL,
                        currentValue REAL NOT NULL,
                        targetValue REAL NOT NULL,
                        deadlineEpochDay INTEGER,
                        status TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_career_goals_status_deadlineEpochDay ON career_goals(status, deadlineEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_career_goals_updatedAtMillis ON career_goals(updatedAtMillis)")

                // Form Drafts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS form_drafts (
                        draftKey TEXT NOT NULL PRIMARY KEY,
                        payloadJson TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(SeedAndPragmaCallback())
                .build()
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2)
                .addCallback(SeedAndPragmaCallback())
                .build()
        }

        fun getDatabaseTotalSizeBytes(context: Context, dbName: String = DB_NAME): Long {
            val dbFile = context.getDatabasePath(dbName)
            val walFile = File("${dbFile.path}-wal")
            val shmFile = File("${dbFile.path}-shm")
            return listOf(dbFile, walFile, shmFile).sumOf { if (it.exists()) it.length() else 0L }
        }

        fun getDirectorySizeBytes(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }

        fun getAvailableStorageBytes(context: Context): Long {
            return runCatching {
                val statBytes = StatFs(context.filesDir.absolutePath).availableBytes
                if (statBytes > 0L) statBytes else context.filesDir.usableSpace
            }.getOrElse {
                runCatching { context.filesDir.usableSpace }.getOrDefault(0L)
            }
        }

        private class SeedAndPragmaCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDefaults(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                runCatching {
                    db.execSQL("PRAGMA foreign_keys = ON")
                    db.execSQL("PRAGMA busy_timeout = 5000")
                    db.execSQL("PRAGMA temp_store = MEMORY")
                    db.query("PRAGMA synchronous = NORMAL").use { it.moveToFirst() }
                }
            }

            private fun seedDefaults(db: SupportSQLiteDatabase) {
                val startEpochDay = DEFAULT_JOURNEY_START_DATE.toEpochDay()
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO editor_profile (
                        id, name, brandName, profession, officialStartDateEpochDay,
                        professionalExperience, skills, editingSoftware, specializations,
                        portfolioLink, personalNotes, updatedAtMillis
                    ) VALUES (
                        1, 'Rohan', 'Rohan Edits', 'Video Editor & Motion Graphics Designer',
                        $startEpochDay, '', '', '', '', '', '', $now
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO app_settings (
                        id, themeMode, dateFormat, currencySymbol, confirmBeforeDelete, deadlineRemindersEnabled
                    ) VALUES (
                        1, 'DARK', 'DD_MMM_YYYY', '₹', 1, 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
