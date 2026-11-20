package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupExportManager
import com.example.data.backup.ExportTarget
import com.example.data.local.AppDatabase
import com.example.data.local.DraftManager
import com.example.data.local.NoteFormDraft
import com.example.data.local.PreferencesDataStore
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.CalendarStatus
import com.example.data.model.ClientSortOption
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.GoalMetric
import com.example.data.model.GoalStatus
import com.example.data.model.NoteCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PlatformType
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectSortOption
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.RestoreMode
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RohanEditsScaleStressTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesDataStore
    private lateinit var backupManager: BackupExportManager

    private val createdDbNames = mutableListOf<String>()
    private val createdFiles = mutableListOf<File>()
    private val fixedToday = LocalDate.of(2026, 11, 20)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = PreferencesDataStore(context)
        backupManager = BackupExportManager()
    }

    @After
    fun tearDown() {
        createdFiles.forEach { file ->
            runCatching { if (file.exists()) file.deleteRecursively() }
        }
        createdDbNames.forEach { dbName ->
            runCatching { context.deleteDatabase(dbName) }
        }
    }

    private fun openDiskDb(dbName: String): AppDatabase {
        createdDbNames.add(dbName)
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun migration_v1_to_v2_preserves_all_existing_rows_and_enables_new_tables() {
        val dbName = "rohan_migration_v1_v2_${System.currentTimeMillis()}.db"
        createdDbNames.add(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Construct an exact schema v1 SQLite database on disk and populate all 7 v1 tables
        val rawV1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawV1.beginTransaction()
        try {
            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS editor_profile (
                    id INTEGER NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    brandName TEXT NOT NULL,
                    profession TEXT NOT NULL,
                    officialStartDateEpochDay INTEGER NOT NULL,
                    professionalExperience TEXT NOT NULL,
                    skills TEXT NOT NULL,
                    editingSoftware TEXT NOT NULL,
                    specializations TEXT NOT NULL,
                    portfolioLink TEXT NOT NULL,
                    personalNotes TEXT NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS clients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    clientType TEXT NOT NULL,
                    mobileNumber TEXT NOT NULL,
                    firstProjectDateEpochDay INTEGER,
                    instructions TEXT NOT NULL,
                    preferredEditingStyle TEXT NOT NULL,
                    perVideoPrice REAL NOT NULL,
                    additionalNotes TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_clients_name ON clients(name)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_clients_clientType ON clients(clientType)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_clients_updatedAtMillis ON clients(updatedAtMillis)")

            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    clientId INTEGER,
                    category TEXT NOT NULL,
                    title TEXT NOT NULL,
                    projectType TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    videoDuration TEXT NOT NULL,
                    projectDateEpochDay INTEGER NOT NULL,
                    deadlineEpochDay INTEGER,
                    projectAmount REAL NOT NULL,
                    amountPaid REAL NOT NULL,
                    paymentStatus TEXT NOT NULL,
                    projectStatus TEXT NOT NULL,
                    clientRating INTEGER,
                    clientFeedback TEXT NOT NULL,
                    additionalNotes TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(clientId) REFERENCES clients(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_projects_clientId ON projects(clientId)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_projects_projectStatus ON projects(projectStatus)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_projects_paymentStatus ON projects(paymentStatus)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_projects_projectDateEpochDay ON projects(projectDateEpochDay)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_projects_deadlineEpochDay ON projects(deadlineEpochDay)")

            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS calendar_days (
                    dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                    status TEXT NOT NULL,
                    note TEXT NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS income_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    source TEXT NOT NULL,
                    clientId INTEGER,
                    projectId INTEGER,
                    clientNameSnapshot TEXT NOT NULL,
                    projectTitleSnapshot TEXT NOT NULL,
                    amount REAL NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(clientId) REFERENCES clients(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_clientId ON income_transactions(clientId)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_projectId ON income_transactions(projectId)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_income_transactions_dateEpochDay ON income_transactions(dateEpochDay)")

            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS expense_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amount REAL NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_expense_transactions_category ON expense_transactions(category)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS index_expense_transactions_dateEpochDay ON expense_transactions(dateEpochDay)")

            rawV1.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_settings (
                    id INTEGER NOT NULL PRIMARY KEY,
                    themeMode TEXT NOT NULL,
                    dateFormat TEXT NOT NULL,
                    currencySymbol TEXT NOT NULL,
                    confirmBeforeDelete INTEGER NOT NULL,
                    deadlineRemindersEnabled INTEGER NOT NULL
                )
                """.trimIndent()
            )

            val now = System.currentTimeMillis()
            val todayEpoch = fixedToday.toEpochDay()
            rawV1.execSQL(
                "INSERT INTO editor_profile VALUES (1, 'Rohan', 'Rohan Edits', 'Video Editor', 20640, '3 yrs', 'After Effects', 'AE, Premiere', 'SaaS', 'https://rohan.edits', 'v1 notes', $now)"
            )
            rawV1.execSQL(
                "INSERT INTO app_settings VALUES (1, 'DARK', 'DD_MMM_YYYY', '₹', 1, 1)"
            )
            rawV1.execSQL(
                "INSERT INTO clients VALUES (10, 'Legacy v1 Agency', 'YOUTUBE', 'REGULAR', '9876543210', $todayEpoch, 'Keep pacing crisp', 'Motion', 2500.0, 'VIP', $now, $now)"
            )
            rawV1.execSQL(
                "INSERT INTO projects VALUES (20, 10, 'CLIENT_VIDEO', 'v1 Flagship Ad', 'SAAS_ANIMATION', 'HIGH', '01:15', $todayEpoch, ${todayEpoch + 5}, 2500.0, 1500.0, 'PARTIAL', 'IN_PROGRESS', 9, 'Great', 'v1 proj note', $now, $now)"
            )
            rawV1.execSQL(
                "INSERT INTO calendar_days VALUES ($todayEpoch, 'DONE', 'Completed 2 cuts', $now)"
            )
            rawV1.execSQL(
                "INSERT INTO income_transactions VALUES (30, 'Payment: v1 Flagship Ad', 10, 20, 'Legacy v1 Agency', 'v1 Flagship Ad', 1500.0, $todayEpoch, 'UPI', 'Milestone 1', $now)"
            )
            rawV1.execSQL(
                "INSERT INTO expense_transactions VALUES (40, 'SOFTWARE', 'Premiere CC', 1600.0, $todayEpoch, 'Monthly', $now)"
            )
            rawV1.version = 1
            rawV1.setTransactionSuccessful()
        } finally {
            rawV1.endTransaction()
            rawV1.close()
        }

        // Open with Room v2 + MIGRATION_1_2 and validate schema, data preservation, and new tables
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val repo = CareerRepository(migratedDb, prefs)
            val profile = repo.getEditorProfileFullSafe()
            assertEquals("Rohan Edits", profile.brandName)
            assertEquals("v1 notes", profile.personalNotes)

            val client = repo.getClientFullSafe(10L)
            assertNotNull(client)
            assertEquals("Legacy v1 Agency", client!!.name)

            val project = repo.getProjectFullSafe(20L)
            assertNotNull(project)
            assertEquals("v1 Flagship Ad", project!!.title)
            assertEquals(1500.0, project.amountPaid, 0.001)

            // Verify new v2 tables work seamlessly alongside migrated v1 rows
            val noteId = repo.saveNote(
                NoteEntity(
                    title = "Post-Migration Script",
                    content = "Shot list for v2 workflow",
                    category = NoteCategory.SCRIPT.name,
                    clientId = 10L,
                    projectId = 20L
                )
            )
            assertTrue(noteId > 0L)

            val revId = repo.addProjectRevision(
                projectId = 20L,
                summary = "Updated intro transition",
                statusSnapshot = ProjectStatus.IN_PROGRESS.name
            )
            assertTrue(revId > 0L)

            val goalId = repo.saveGoal(
                CareerGoalEntity(
                    title = "Reach 50 Clients",
                    targetMetric = GoalMetric.CLIENTS.name,
                    currentValue = 1.0,
                    targetValue = 50.0,
                    status = GoalStatus.ACTIVE.name
                )
            )
            assertTrue(goalId > 0L)

            val (integrityOk, detail) = migratedDb.runIntegrityCheck()
            assertTrue("Integrity check after v1->v2 migration failed: $detail", integrityOk)
        }
        migratedDb.close()
    }

    @Test
    fun high_scale_stress_1000_clients_100000_projects_1000000_transactions_pagination_search_and_analytics() {
        val dbName = "rohan_stress_1m_${System.currentTimeMillis()}.db"
        var db = openDiskDb(dbName)
        val sqlite = db.openHelper.writableDatabase
        val now = System.currentTimeMillis()
        val todayEpoch = fixedToday.toEpochDay()

        val clientCountTarget = 1_200
        val projectCountTarget = 100_500
        val incomeCountTarget = 700_500
        val expenseCountTarget = 300_000

        val seedElapsedMs = measureTimeMillis {
            sqlite.beginTransaction()
            try {
                // Seed editor profile & settings so foreign keys / defaults exist
                sqlite.execSQL(
                    "INSERT OR IGNORE INTO editor_profile VALUES (1, 'Rohan', 'Rohan Edits', 'Video Editor', ${todayEpoch - 120}, '', '', '', '', '', '', $now)"
                )
                sqlite.execSQL(
                    "INSERT OR IGNORE INTO app_settings VALUES (1, 'DARK', 'DD_MMM_YYYY', '₹', 1, 0)"
                )

                val clientStmt = sqlite.compileStatement(
                    "INSERT INTO clients (id, name, platform, clientType, mobileNumber, firstProjectDateEpochDay, instructions, preferredEditingStyle, perVideoPrice, additionalNotes, createdAtMillis, updatedAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..clientCountTarget) {
                    clientStmt.bindLong(1, i.toLong())
                    clientStmt.bindString(2, "Scale Client #${i.toString().padStart(4, '0')}")
                    clientStmt.bindString(3, if (i % 2 == 0) PlatformType.YOUTUBE.name else PlatformType.INSTAGRAM.name)
                    clientStmt.bindString(4, if (i % 3 == 0) ClientType.REGULAR.name else ClientType.NEW.name)
                    clientStmt.bindString(5, "90000${i.toString().padStart(5, '0')}")
                    clientStmt.bindLong(6, todayEpoch - (i % 180))
                    clientStmt.bindString(7, "Style brief #$i")
                    clientStmt.bindString(8, "Kinetic Motion")
                    clientStmt.bindDouble(9, 1000.0 + (i % 5) * 500.0)
                    clientStmt.bindString(10, "Notes $i")
                    clientStmt.bindLong(11, now - i * 100L)
                    clientStmt.bindLong(12, now - i * 10L)
                    clientStmt.executeInsert()
                    clientStmt.clearBindings()
                }
                clientStmt.close()

                val projectStmt = sqlite.compileStatement(
                    "INSERT INTO projects (id, clientId, category, title, projectType, priority, videoDuration, projectDateEpochDay, deadlineEpochDay, projectAmount, amountPaid, paymentStatus, projectStatus, clientRating, clientFeedback, additionalNotes, createdAtMillis, updatedAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..projectCountTarget) {
                    val cid = ((i - 1) % clientCountTarget + 1).toLong()
                    val status = if (i % 4 == 0) ProjectStatus.IN_PROGRESS.name else ProjectStatus.COMPLETED.name
                    val payStatus = if (i % 4 == 0) PaymentStatus.PARTIAL.name else PaymentStatus.PAID.name
                    val dateEpoch = todayEpoch - (i % 365)
                    projectStmt.bindLong(1, i.toLong())
                    projectStmt.bindLong(2, cid)
                    projectStmt.bindString(3, VideoCategory.CLIENT_VIDEO.name)
                    projectStmt.bindString(4, "Project Deliverable #$i")
                    projectStmt.bindString(5, ProjectType.SAAS_ANIMATION.name)
                    projectStmt.bindString(6, if (i % 10 == 0) ProjectPriority.HIGH.name else ProjectPriority.NORMAL.name)
                    projectStmt.bindString(7, "01:30")
                    projectStmt.bindLong(8, dateEpoch)
                    projectStmt.bindLong(9, dateEpoch + 3)
                    projectStmt.bindDouble(10, 1000.0)
                    projectStmt.bindDouble(11, if (i % 4 == 0) 400.0 else 1000.0)
                    projectStmt.bindString(12, payStatus)
                    projectStmt.bindString(13, status)
                    projectStmt.bindNull(14)
                    projectStmt.bindString(15, "")
                    projectStmt.bindString(16, "")
                    projectStmt.bindLong(17, now - i)
                    projectStmt.bindLong(18, now - i)
                    projectStmt.executeInsert()
                    projectStmt.clearBindings()
                }
                projectStmt.close()

                val incomeStmt = sqlite.compileStatement(
                    "INSERT INTO income_transactions (id, source, clientId, projectId, clientNameSnapshot, projectTitleSnapshot, amount, dateEpochDay, paymentMethod, notes, createdAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..incomeCountTarget) {
                    val pid = ((i - 1) % projectCountTarget + 1).toLong()
                    val cid = ((pid - 1) % clientCountTarget + 1)
                    incomeStmt.bindLong(1, i.toLong())
                    incomeStmt.bindString(2, "Invoice #$i")
                    incomeStmt.bindLong(3, cid)
                    incomeStmt.bindLong(4, pid)
                    incomeStmt.bindString(5, "Client $cid")
                    incomeStmt.bindString(6, "Project Deliverable #$pid")
                    incomeStmt.bindDouble(7, 500.0)
                    incomeStmt.bindLong(8, todayEpoch - (i % 365))
                    incomeStmt.bindString(9, PaymentMethod.UPI.name)
                    incomeStmt.bindString(10, "")
                    incomeStmt.bindLong(11, now - i)
                    incomeStmt.executeInsert()
                    incomeStmt.clearBindings()
                }
                incomeStmt.close()

                val expenseStmt = sqlite.compileStatement(
                    "INSERT INTO expense_transactions (id, category, description, amount, dateEpochDay, notes, createdAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..expenseCountTarget) {
                    expenseStmt.bindLong(1, i.toLong())
                    expenseStmt.bindString(2, ExpenseCategory.EDITING_ASSETS.name)
                    expenseStmt.bindString(3, "Asset Pack #$i")
                    expenseStmt.bindDouble(4, 100.0)
                    expenseStmt.bindLong(5, todayEpoch - (i % 365))
                    expenseStmt.bindString(6, "")
                    expenseStmt.bindLong(7, now - i)
                    expenseStmt.executeInsert()
                    expenseStmt.clearBindings()
                }
                expenseStmt.close()

                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }

        // Force GC baseline before measuring query memory footprint
        Runtime.getRuntime().gc()
        Thread.sleep(50)
        val heapBeforeQueries = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        runBlocking {
            val repository = CareerRepository(db, prefs)

            val counts = db.dashboardAggregateDao().getStorageRecordCounts()
            assertEquals(clientCountTarget.toLong(), counts.clients)
            assertEquals(projectCountTarget.toLong(), counts.projects)
            assertEquals(incomeCountTarget.toLong(), counts.incomeTransactions)
            assertEquals(expenseCountTarget.toLong(), counts.expenseTransactions)
            assertEquals(1_000_500L, counts.incomeTransactions + counts.expenseTransactions)

            val metricsMs = measureTimeMillis {
                val home = repository.observeHomeMetrics(fixedToday, fixedToday.minusDays(120)).first()
                assertEquals(clientCountTarget, home.totalClients)
                assertEquals(projectCountTarget, home.totalProjects)
                assertEquals(incomeCountTarget * 500.0, home.totalEarnings, 0.01)
                assertEquals(expenseCountTarget * 100.0, home.totalExpenses, 0.01)
            }

            var clientsPageMs = 0L
            clientsPageMs = measureTimeMillis {
                val page = repository.getClientsPage(
                    query = "099",
                    clientType = null,
                    sortOption = ClientSortOption.RECENT_ACTIVITY,
                    limit = 50,
                    offset = 0
                )
                assertTrue(page.items.isNotEmpty())
                assertTrue(page.items.size <= 50)
            }

            var projectsPageMs = 0L
            projectsPageMs = measureTimeMillis {
                val page = repository.getProjectsPage(
                    query = "Deliverable #999",
                    todayEpoch = todayEpoch,
                    sortOption = ProjectSortOption.DATE_DESC,
                    limit = 50,
                    offset = 0
                )
                assertTrue(page.items.isNotEmpty())
                assertTrue(page.items.size <= 50)
            }

            val incomePageMs = measureTimeMillis {
                val page = repository.getIncomePage(query = "", limit = 50, offset = 0)
                assertEquals(50, page.items.size)
                assertEquals(incomeCountTarget, page.totalCount)
                assertTrue(page.hasMore)
            }

            val seriesMs = measureTimeMillis {
                val monthly = repository.getMonthlyFinancialSeries(fixedToday)
                assertEquals(6, monthly.size)
                assertTrue(monthly.sumOf { it.income } > 0.0)
            }

            val pickerMs = measureTimeMillis {
                val pickedClients = repository.searchClientsForPicker("Client #11", limit = 50)
                assertTrue(pickedClients.isNotEmpty())
                val pickedProjects = repository.searchProjectsForPicker("Deliverable #5000", limit = 50)
                assertTrue(pickedProjects.isNotEmpty())
            }

            Runtime.getRuntime().gc()
            val heapAfterQueries = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val heapDeltaMb = (heapAfterQueries - heapBeforeQueries).coerceAtLeast(0L) / (1024.0 * 1024.0)

            val dbFileSizeBytes = AppDatabase.getDatabaseTotalSizeBytes(context, dbName)
            val (integrityOk, integrityMsg) = db.runIntegrityCheck()
            assertTrue("Integrity check on 1.1M+ DB failed: $integrityMsg", integrityOk)

            println(
                "[BENCHMARK_1M] seed=${seedElapsedMs}ms | homeMetrics=${metricsMs}ms | " +
                    "clientsPage=${clientsPageMs}ms | projectsPage=${projectsPageMs}ms | " +
                    "incomePage=${incomePageMs}ms | monthlySeries=${seriesMs}ms | picker=${pickerMs}ms | " +
                    "dbSize=${dbFileSizeBytes / (1024 * 1024)}MB | queryHeapDelta=${String.format("%.2f", heapDeltaMb)}MB"
            )

            // Assert practical responsiveness & bounded memory under >1.1M total rows
            assertTrue("Heap delta spiked too high ($heapDeltaMb MB)", heapDeltaMb < 48.0)
            assertTrue("Home metrics took too long: ${metricsMs}ms", metricsMs < 10_000L)
            assertTrue("Paged query took too long: ${projectsPageMs}ms", projectsPageMs < 5_000L)
        }

        // Verify disk persistence after closing and reopening DB
        db.close()
        db = openDiskDb(dbName)
        runBlocking {
            val countsAfterReopen = db.dashboardAggregateDao().getStorageRecordCounts()
            assertEquals(clientCountTarget.toLong(), countsAfterReopen.clients)
            assertEquals(projectCountTarget.toLong(), countsAfterReopen.projects)
            assertEquals(incomeCountTarget.toLong(), countsAfterReopen.incomeTransactions)
            assertEquals(expenseCountTarget.toLong(), countsAfterReopen.expenseTransactions)
        }
        db.close()
    }

    @Test
    fun huge_text_over_2_5mb_chunked_read_and_persistence_without_cursor_window_crash() {
        val dbName = "rohan_huge_text_${System.currentTimeMillis()}.db"
        var db = openDiskDb(dbName)

        // Generate > 2.6 MB string (exceeds Android CursorWindow 2MB single-row limit)
        val unit = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-KeyframeColorGradeLUT-\n"
        val repeatCount = (2_650_000 / unit.length) + 1
        val hugeText = unit.repeat(repeatCount)
        assertTrue("Expected >2.6MB text, got ${hugeText.length}", hugeText.length > 2_600_000)

        var noteId = 0L
        var clientId = 0L
        var projectId = 0L

        runBlocking {
            val repo = CareerRepository(db, prefs)
            repo.ensureDefaultsExist()
            clientId = repo.saveClient(
                ClientEntity(
                    name = "Mega Brief Studio",
                    platform = PlatformType.YOUTUBE.name,
                    clientType = ClientType.REGULAR.name,
                    instructions = hugeText
                )
            )
            projectId = repo.saveProject(
                ProjectEntity(
                    clientId = clientId,
                    category = VideoCategory.CLIENT_VIDEO.name,
                    title = "Documentary Master Cut",
                    projectType = ProjectType.MOTION_GRAPHICS.name,
                    priority = ProjectPriority.HIGH.name,
                    projectDateEpochDay = fixedToday.toEpochDay(),
                    paymentStatus = PaymentStatus.UNPAID.name,
                    projectStatus = ProjectStatus.IN_PROGRESS.name,
                    additionalNotes = hugeText
                )
            )
            noteId = repo.saveNote(
                NoteEntity(
                    title = "2.6MB Full Transcript & Edit Cue Sheet",
                    content = hugeText,
                    category = NoteCategory.SCRIPT.name,
                    clientId = clientId,
                    projectId = projectId,
                    isPinned = true
                )
            )

            // Verify DraftManager saves & recovers large form drafts cleanly via SQLite form_drafts
            val draftManager = DraftManager(context, db.formDraftDao())
            val largeDraft = hugeText.take(350_000)
            draftManager.saveNoteDraft(
                NoteFormDraft(
                    targetNoteId = noteId,
                    title = "2.6MB Draft",
                    content = largeDraft,
                    category = NoteCategory.SCRIPT.name,
                    clientId = clientId,
                    projectId = projectId,
                    isPinned = true
                )
            )
            val restoredDraft = draftManager.getNoteDraft(noteId)
            assertNotNull(restoredDraft)
            assertEquals(largeDraft.length, restoredDraft!!.content.length)
            assertEquals(largeDraft, restoredDraft.content)
        }

        // Reopen DB from disk and verify safe readers + SQLite SUBSTR chunked slices
        db.close()
        db = openDiskDb(dbName)
        runBlocking {
            val repo = CareerRepository(db, prefs)

            val loadedNote = repo.getNoteFullSafe(noteId)
            assertNotNull(loadedNote)
            assertEquals(hugeText.length, loadedNote!!.content.length)
            assertEquals(hugeText, loadedNote.content)

            val loadedProject = repo.getProjectFullSafe(projectId)
            assertNotNull(loadedProject)
            assertEquals(hugeText.length, loadedProject!!.additionalNotes.length)

            val loadedClient = repo.getClientFullSafe(clientId)
            assertNotNull(loadedClient)
            assertEquals(hugeText.length, loadedClient!!.instructions.length)

            // Directly exercise SQLite LENGTH + SUBSTR 250KB chunked slicer (1-based) across all slices
            val totalLen = db.noteDao().getNoteContentLength(noteId) ?: 0
            assertEquals(hugeText.length, totalLen)
            val chunked = StringBuilder(totalLen)
            var pos = 1
            val sliceSize = 250_000
            while (pos <= totalLen) {
                val len = minOf(sliceSize, totalLen - pos + 1)
                chunked.append(db.noteDao().getNoteContentChunk(noteId, pos, len).orEmpty())
                pos += len
            }
            assertEquals(hugeText, chunked.toString())
        }
        db.close()
    }

    @Test
    fun streaming_backup_inspect_duplicate_merge_and_corruption_atomic_rollback() {
        val dbName = "rohan_backup_stream_${System.currentTimeMillis()}.db"
        val db = openDiskDb(dbName)
        val backupFile = File(context.cacheDir, "test_backup_${System.currentTimeMillis()}.json")
        val corruptFile = File(context.cacheDir, "test_corrupt_${System.currentTimeMillis()}.json")
        val csvProjectsFile = File(context.cacheDir, "test_projects_${System.currentTimeMillis()}.csv")
        val csvIncomeFile = File(context.cacheDir, "test_income_${System.currentTimeMillis()}.csv")
        createdFiles.addAll(listOf(backupFile, corruptFile, csvProjectsFile, csvIncomeFile))

        runBlocking {
            val repo = CareerRepository(db, prefs)
            repo.ensureDefaultsExist()

            val cid = repo.saveClient(
                ClientEntity(
                    name = "Vanguard Tech",
                    platform = PlatformType.YOUTUBE.name,
                    clientType = ClientType.REGULAR.name,
                    perVideoPrice = 3000.0
                )
            )
            val pid = repo.saveProject(
                ProjectEntity(
                    clientId = cid,
                    category = VideoCategory.CLIENT_VIDEO.name,
                    title = "Product Launch Film",
                    projectType = ProjectType.SAAS_ANIMATION.name,
                    priority = ProjectPriority.URGENT.name,
                    projectDateEpochDay = fixedToday.toEpochDay(),
                    projectAmount = 3000.0,
                    paymentStatus = PaymentStatus.UNPAID.name,
                    projectStatus = ProjectStatus.IN_PROGRESS.name
                )
            )
            repo.recordProjectPayment(pid, 1500.0, fixedToday.toEpochDay(), PaymentMethod.UPI, "Deposit")
            repo.saveExpense(
                ExpenseTransactionEntity(
                    category = ExpenseCategory.SOFTWARE.name,
                    description = "Motion Suite",
                    amount = 800.0,
                    dateEpochDay = fixedToday.toEpochDay()
                )
            )
            repo.setCalendarDayStatus(fixedToday.toEpochDay(), CalendarStatus.DONE, "Shipped cut")
            repo.saveNote(NoteEntity(title = "Voiceover Notes", content = "Warm tone", category = NoteCategory.PROJECT.name, clientId = cid, projectId = pid))
            repo.addProjectRevision(pid, "Adjusted color LUT", statusSnapshot = ProjectStatus.IN_PROGRESS.name)
            repo.saveGoal(CareerGoalEntity(title = "Earn ₹1L", targetMetric = GoalMetric.INCOME.name, currentValue = 1500.0, targetValue = 100_000.0))

            // 1. Write streaming backup JSON
            FileOutputStream(backupFile).use { os ->
                backupManager.writeBackupJsonStreaming(os, repo)
            }
            assertTrue(backupFile.length() > 200L)

            // 2. Inspect streaming backup
            val inspection = FileInputStream(backupFile).use { ins ->
                backupManager.inspectBackupStreaming(ins).getOrThrow()
            }
            assertEquals(2, inspection.schemaVersion)
            assertEquals(1, inspection.clientCount)
            assertEquals(1, inspection.projectCount)
            assertEquals(1, inspection.incomeCount)
            assertEquals(1, inspection.expenseCount)
            assertEquals(1, inspection.calendarDayCount)
            assertEquals(1, inspection.noteCount)
            assertEquals(1, inspection.revisionCount)
            assertEquals(1, inspection.goalCount)

            // 3. Corruption mid-stream atomic rollback test (with REPLACE_ALL)
            val validText = backupFile.readText(Charsets.UTF_8)
            val truncatedText = validText.substring(0, (validText.length * 0.65).toInt()) + "\n  {\"corrupt_broken"
            corruptFile.writeText(truncatedText, Charsets.UTF_8)

            val corruptResult = runCatching {
                FileInputStream(corruptFile).use { ins ->
                    backupManager.restoreBackupStreaming(ins, repo, RestoreMode.REPLACE_ALL)
                }
            }
            assertTrue("Corrupt backup must fail", corruptResult.isFailure)

            // Verify atomic transaction rollback left existing records 100% intact
            val countsAfterRollback = db.dashboardAggregateDao().getStorageRecordCounts()
            assertEquals(1L, countsAfterRollback.clients)
            assertEquals(1L, countsAfterRollback.projects)
            assertEquals(1L, countsAfterRollback.incomeTransactions)
            assertEquals(1L, countsAfterRollback.expenseTransactions)
            assertEquals(1L, countsAfterRollback.notes)
            assertEquals(1L, countsAfterRollback.revisions)
            assertEquals(1L, countsAfterRollback.careerGoals)

            // 4. Duplicate-skipping MERGE restore test
            val mergeSummary = FileInputStream(backupFile).use { ins ->
                backupManager.restoreBackupStreaming(ins, repo, RestoreMode.MERGE_SKIP_DUPLICATES)
            }
            assertEquals(RestoreMode.MERGE_SKIP_DUPLICATES, mergeSummary.mode)
            assertTrue("Expected duplicates skipped > 0", mergeSummary.skippedDuplicatesCount >= 6)
            assertEquals(0, mergeSummary.insertedCount)

            val countsAfterMerge = db.dashboardAggregateDao().getStorageRecordCounts()
            assertEquals(1L, countsAfterMerge.clients)
            assertEquals(1L, countsAfterMerge.projects)
            assertEquals(1L, countsAfterMerge.incomeTransactions)

            // 5. Large streaming CSV export benchmark (50,000 projects + 50,000 income rows in batch)
            val sqlite = db.openHelper.writableDatabase
            val now = System.currentTimeMillis()
            val todayEpoch = fixedToday.toEpochDay()
            sqlite.beginTransaction()
            try {
                val pStmt = sqlite.compileStatement(
                    "INSERT INTO projects (clientId, category, title, projectType, priority, videoDuration, projectDateEpochDay, deadlineEpochDay, projectAmount, amountPaid, paymentStatus, projectStatus, clientRating, clientFeedback, additionalNotes, createdAtMillis, updatedAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..25_000) {
                    pStmt.bindLong(1, cid)
                    pStmt.bindString(2, VideoCategory.CLIENT_VIDEO.name)
                    pStmt.bindString(3, "CSV Row Project #$i")
                    pStmt.bindString(4, ProjectType.REELS_SHORTS.name)
                    pStmt.bindString(5, ProjectPriority.NORMAL.name)
                    pStmt.bindString(6, "00:45")
                    pStmt.bindLong(7, todayEpoch)
                    pStmt.bindNull(8)
                    pStmt.bindDouble(9, 1000.0)
                    pStmt.bindDouble(10, 1000.0)
                    pStmt.bindString(11, PaymentStatus.PAID.name)
                    pStmt.bindString(12, ProjectStatus.COMPLETED.name)
                    pStmt.bindNull(13)
                    pStmt.bindString(14, "")
                    pStmt.bindString(15, "")
                    pStmt.bindLong(16, now)
                    pStmt.bindLong(17, now)
                    pStmt.executeInsert()
                    pStmt.clearBindings()
                }
                pStmt.close()

                val iStmt = sqlite.compileStatement(
                    "INSERT INTO income_transactions (source, clientId, projectId, clientNameSnapshot, projectTitleSnapshot, amount, dateEpochDay, paymentMethod, notes, createdAtMillis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                for (i in 1..25_000) {
                    iStmt.bindString(1, "CSV Batch #$i")
                    iStmt.bindLong(2, cid)
                    iStmt.bindLong(3, pid)
                    iStmt.bindString(4, "Vanguard Tech")
                    iStmt.bindString(5, "Product Launch Film")
                    iStmt.bindDouble(6, 250.0)
                    iStmt.bindLong(7, todayEpoch)
                    iStmt.bindString(8, PaymentMethod.UPI.name)
                    iStmt.bindString(9, "")
                    iStmt.bindLong(10, now)
                    iStmt.executeInsert()
                    iStmt.clearBindings()
                }
                iStmt.close()
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }

            val csvExportMs = measureTimeMillis {
                FileOutputStream(csvProjectsFile).use { os ->
                    backupManager.writeCsvStreaming(ExportTarget.PROJECT_LIST, os, repo, fixedToday, DateFormatOption.DD_MMM_YYYY)
                }
                FileOutputStream(csvIncomeFile).use { os ->
                    backupManager.writeCsvStreaming(ExportTarget.INCOME_RECORDS, os, repo, fixedToday, DateFormatOption.DD_MMM_YYYY)
                }
            }
            val projLines = csvProjectsFile.useLines { it.count() }
            val incLines = csvIncomeFile.useLines { it.count() }
            assertEquals(25_002, projLines) // 1 header + 1 initial + 25,000 batch
            assertEquals(25_002, incLines)  // 1 header + 1 initial + 25,000 batch
            println("[BENCHMARK_STREAM_EXPORT] 50k CSV rows exported in ${csvExportMs}ms (projects=${csvProjectsFile.length() / 1024}KB, income=${csvIncomeFile.length() / 1024}KB)")
        }
        db.close()
    }

    @Test
    fun storage_management_clear_cache_vs_clear_all_data_and_integrity_check() {
        val dbName = "rohan_storage_mgmt_${System.currentTimeMillis()}.db"
        val db = openDiskDb(dbName)
        val draftManager = DraftManager(context, db.formDraftDao())
        val dummyCacheFile = File(context.cacheDir, "dummy_temp_render_cache.tmp").apply {
            writeText("cache-bytes-".repeat(500))
        }
        createdFiles.add(dummyCacheFile)

        runBlocking {
            val repo = CareerRepository(db, prefs)
            repo.ensureDefaultsExist()
            val cid = repo.saveClient(
                ClientEntity(name = "Cache Check Studio", platform = PlatformType.YOUTUBE.name, clientType = ClientType.NEW.name)
            )
            draftManager.saveNoteDraft(NoteFormDraft(title = "Temporary Draft", content = "Unsaved"))

            val diag = repo.getStorageDiagnostics(context, runIntegrity = true)
            assertEquals(true, diag.integrityOk)
            assertEquals(1L, diag.counts.clients)
            assertTrue(diag.cacheSizeBytes > 0L)
            assertTrue(diag.availableDeviceBytes > 0L)

            // 1. Clear Cache Only -> DB records must stay untouched!
            val clearedBytes = repo.clearCacheOnly(context)
            assertTrue(clearedBytes > 0L)
            assertFalse(dummyCacheFile.exists())
            assertEquals(1L, db.dashboardAggregateDao().getStorageRecordCounts().clients)

            // 2. Clear All Application Data -> wipes all records & drafts, restores defaults & integrity
            repo.clearAllApplicationData(context, draftManager)
            val afterClearCounts = db.dashboardAggregateDao().getStorageRecordCounts()
            assertEquals(0L, afterClearCounts.totalRecords)
            val profileAfter = repo.getEditorProfileFullSafe()
            assertEquals("Rohan", profileAfter.name)
            val (okAfterClear, detailAfterClear) = db.runIntegrityCheck()
            assertTrue("Integrity check after clear all failed: $detailAfterClear", okAfterClear)
        }
        db.close()
    }
}
