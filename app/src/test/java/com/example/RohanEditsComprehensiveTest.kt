package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupExportManager
import com.example.data.backup.ExportTarget
import com.example.data.backup.toSnapshotValidated
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.CalendarStatus
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PlatformType
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.ThemeMode
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
import com.example.ui.navigation.RohanEditsAppShell
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateProvider
import com.example.util.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RohanEditsComprehensiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefs: PreferencesDataStore
    private lateinit var repository: CareerRepository
    private lateinit var backupManager: BackupExportManager
    private lateinit var viewModel: MainViewModel

    private val fixedToday = LocalDate.of(2026, 11, 19)
    private val fixedDateProvider = object : DateProvider {
        override fun today(): LocalDate = fixedToday
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        prefs = PreferencesDataStore(context)
        repository = CareerRepository(db, prefs)
        backupManager = BackupExportManager()
        runBlocking { repository.ensureDefaultsExist() }
        viewModel = MainViewModel(repository, backupManager, fixedDateProvider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun scenarios_1_through_16_career_finances_calendar_backup_and_analytics() {
        runBlocking {
        // Initial defaults check (6 July 2026 start date, 0% consistency, zero fake data)
        val initialProfile = repository.editorProfile.first()
        assertEquals("Rohan", initialProfile.name)
        assertEquals("Rohan Edits", initialProfile.brandName)
        assertEquals(LocalDate.of(2026, 7, 6).toEpochDay(), initialProfile.officialStartDateEpochDay)
        assertEquals(0.0, Formatters.calculateConsistencyPercent(0, 0), 0.001)

        // 1. Add a client and verify count
        val clientId = repository.saveClient(
            ClientEntity(
                name = "TechPulse SaaS",
                platform = PlatformType.YOUTUBE.name,
                clientType = ClientType.NEW.name,
                perVideoPrice = 500.0
            )
        )
        assertEquals(1, repository.clients.first().size)

        // 2. Edit the client and verify updated info
        val loadedClient = repository.observeClient(clientId).first()!!
        repository.saveClient(loadedClient.copy(clientType = ClientType.REGULAR.name, preferredEditingStyle = "Clean Motion"))
        val updatedClient = repository.observeClient(clientId).first()!!
        assertEquals(ClientType.REGULAR.name, updatedClient.clientType)
        assertEquals("Clean Motion", updatedClient.preferredEditingStyle)

        // 3. Add a commissioned project (₹500)
        val commissionedId = repository.saveProject(
            ProjectEntity(
                clientId = clientId,
                category = VideoCategory.CLIENT_VIDEO.name,
                title = "SaaS Launch Explainer",
                projectType = ProjectType.SAAS_ANIMATION.name,
                priority = ProjectPriority.HIGH.name,
                projectDateEpochDay = fixedToday.toEpochDay(),
                deadlineEpochDay = fixedToday.plusDays(3).toEpochDay(),
                projectAmount = 500.0,
                paymentStatus = PaymentStatus.UNPAID.name,
                projectStatus = ProjectStatus.IN_PROGRESS.name
            )
        )

        // 4. Add a personal project without a client
        val personalId = repository.saveProject(
            ProjectEntity(
                clientId = null,
                category = VideoCategory.PERSONAL_VIDEO.name,
                title = "Motion Reel Study",
                projectType = ProjectType.MOTION_GRAPHICS.name,
                priority = ProjectPriority.NORMAL.name,
                projectDateEpochDay = fixedToday.toEpochDay(),
                projectAmount = 0.0,
                paymentStatus = PaymentStatus.UNPAID.name,
                projectStatus = ProjectStatus.IN_PROGRESS.name
            )
        )
        assertEquals(2, repository.projects.first().size)

        // 5 & 9. Complete a project -> Completed video count = 1, AND verify NO income is auto-created!
        val commissionedBeforeComplete = repository.observeProject(commissionedId).first()!!
        repository.saveProject(commissionedBeforeComplete.copy(projectStatus = ProjectStatus.COMPLETED.name))
        assertEquals(1, repository.projects.first().count { it.projectStatus == ProjectStatus.COMPLETED.name })
        assertEquals(0.0, repository.incomeTransactions.first().sumOf { it.amount }, 0.001)

        // 6. Record first partial payment = ₹200 -> Remaining ₹300, Partial, Income ₹200
        repository.recordProjectPayment(commissionedId, 200.0, fixedToday.toEpochDay(), PaymentMethod.UPI, "Advance")
        var projAfter1 = repository.observeProject(commissionedId).first()!!
        assertEquals(200.0, projAfter1.amountPaid, 0.001)
        assertEquals(PaymentStatus.PARTIAL.name, projAfter1.paymentStatus)
        assertEquals(200.0, repository.incomeTransactions.first().sumOf { it.amount }, 0.001)

        // 7 & 8. Record second payment = ₹150 -> Remaining ₹150, Partial, Income ₹350
        repository.recordProjectPayment(commissionedId, 150.0, fixedToday.toEpochDay(), PaymentMethod.BANK_TRANSFER, "Milestone 2")
        var projAfter2 = repository.observeProject(commissionedId).first()!!
        assertEquals(350.0, projAfter2.amountPaid, 0.001)
        assertEquals(PaymentStatus.PARTIAL.name, projAfter2.paymentStatus)
        assertEquals(150.0, projAfter2.projectAmount - projAfter2.amountPaid, 0.001)
        assertEquals(350.0, repository.incomeTransactions.first().sumOf { it.amount }, 0.001)

        // Record final ₹150 -> Paid, Remaining ₹0, Income ₹500
        repository.recordProjectPayment(commissionedId, 150.0, fixedToday.toEpochDay(), PaymentMethod.UPI, "Final")
        val projAfter3 = repository.observeProject(commissionedId).first()!!
        assertEquals(500.0, projAfter3.amountPaid, 0.001)
        assertEquals(PaymentStatus.PAID.name, projAfter3.paymentStatus)
        assertEquals(500.0, repository.incomeTransactions.first().sumOf { it.amount }, 0.001)

        // 10. Add an expense (₹120) and verify net balance (500 - 120 = 380)
        repository.saveExpense(
            ExpenseTransactionEntity(
                category = ExpenseCategory.SUBSCRIPTIONS.name,
                description = "Plugin Pack",
                amount = 120.0,
                dateEpochDay = fixedToday.toEpochDay()
            )
        )
        val totalInc = repository.incomeTransactions.first().sumOf { it.amount }
        val totalExp = repository.expenseTransactions.first().sumOf { it.amount }
        assertEquals(380.0, totalInc - totalExp, 0.001)

        // 11, 12, 13. Mark Done, Missed, and Future Done -> verify consistency excludes future & not marked
        repository.setCalendarDayStatus(fixedToday.minusDays(2).toEpochDay(), CalendarStatus.DONE, "Edited reel")
        repository.setCalendarDayStatus(fixedToday.minusDays(1).toEpochDay(), CalendarStatus.MISSED, "Rest")
        // Future date should be excluded from past consistency calculation
        repository.setCalendarDayStatus(fixedToday.plusDays(2).toEpochDay(), CalendarStatus.DONE, "Planned")

        val validPast = repository.calendarDays.first().filter { it.dateEpochDay <= fixedToday.toEpochDay() }
        val done = validPast.count { it.status == CalendarStatus.DONE.name }
        val missed = validPast.count { it.status == CalendarStatus.MISSED.name }
        assertEquals(1, done)
        assertEquals(1, missed)
        assertEquals(50.0, Formatters.calculateConsistencyPercent(done, missed), 0.001)

        // 15. Export JSON backup, validate, and restore into a fresh DB
        val snapshot = repository.getAllSnapshotForBackup()
        val json = backupManager.serializeBackup(snapshot)
        val parsedDto = backupManager.parseAndValidateBackup(json).getOrThrow()

        val freshDb = AppDatabase.createInMemory(context)
        val freshRepo = CareerRepository(freshDb, prefs)
        freshRepo.restoreFromSnapshot(parsedDto.toSnapshotValidated())
        assertEquals(1, freshRepo.clients.first().size)
        assertEquals(2, freshRepo.projects.first().size)
        assertEquals(3, freshRepo.incomeTransactions.first().size)
        assertEquals(1, freshRepo.expenseTransactions.first().size)
        assertEquals(3, freshRepo.calendarDays.first().size)

        // Verify CSV export contains real records
        val csv = backupManager.generateCsv(ExportTarget.PROJECT_LIST, snapshot, fixedToday, DateFormatOption.DD_MMM_YYYY)
        assertTrue(csv.contains("SaaS Launch Explainer"))
        assertTrue(csv.contains("Motion Reel Study"))
        freshDb.close()

        // 16. Verify Analytics computation
        val currentState = viewModel.uiState.first { !it.isLoading && it.home.totalClients == 1 }
        val finSeries = viewModel.computeFinancialSeries("Monthly", currentState)
        assertEquals(500.0, finSeries.last().income, 0.001)
        assertEquals(120.0, finSeries.last().expense, 0.001)
        val vidSeries = viewModel.computeVideoOutputSeries(VideoCategory.CLIENT_VIDEO, currentState)
        assertEquals(1, vidSeries.last().completedCount)
        assertTrue(viewModel.computeAverageVideosPerMonth(currentState) > 0.0)
        assertEquals(1, currentState.home.totalProjects - currentState.home.commissionedProjectsCount)
        assertEquals(personalId, currentState.projectsWithClient.first { it.project.clientId == null }.project.id)
        }
    }

    @Test
    fun scenario_14_disk_persistence_across_database_reopen() {
        runBlocking {
        val dbName = "rohan_persistence_test_${System.currentTimeMillis()}.db"
        val diskDb1 = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo1 = CareerRepository(diskDb1, prefs)
        repo1.ensureDefaultsExist()
        repo1.saveClient(
            ClientEntity(
                name = "Persisted Studio",
                platform = PlatformType.INSTAGRAM.name,
                clientType = ClientType.OLD.name,
                perVideoPrice = 750.0
            )
        )
        diskDb1.close()

        // Reopen DB from disk (simulates app restart)
        val diskDb2 = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo2 = CareerRepository(diskDb2, prefs)
        val clients = repo2.clients.first()
        assertEquals(1, clients.size)
        assertEquals("Persisted Studio", clients.first().name)
        diskDb2.close()
        context.deleteDatabase(dbName)
        }
    }

    @Test
    fun scenarios_17_and_18_dark_mode_and_all_bottom_navigation_tabs() {
        runBlocking {
            val current = repository.settings.first()
            repository.updateSettings(current.copy(themeMode = ThemeMode.DARK.name))
        }

        composeTestRule.setContent {
            RohanEditsAppShell(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Home screen displayed
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // Navigate through all 5 bottom navigation tabs
        composeTestRule.onNodeWithTag("nav_clients").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("clients_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_calendar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_budget").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("budget_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_more").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("more_screen").assertIsDisplayed()

        // Open Editor Info from More and verify Rohan's default profile
        composeTestRule.onNodeWithTag("more_editor_info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_info_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rohan").assertIsDisplayed()

        // Return Home
        composeTestRule.onNodeWithTag("topbar_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }
}
