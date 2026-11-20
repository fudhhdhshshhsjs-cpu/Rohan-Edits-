package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupExportManager
import com.example.data.backup.BackupPayloadDto
import com.example.data.backup.ExportTarget
import com.example.data.backup.toSnapshotValidated
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.CalendarStatus
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentMethod
import com.example.data.model.ProjectStatus
import com.example.data.model.ThemeMode
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
import com.example.data.repository.DatabaseSnapshot
import com.example.ui.state.CalendarOverviewState
import com.example.ui.state.ClientWithStats
import com.example.ui.state.FinancialPeriodPoint
import com.example.ui.state.GlobalSearchResults
import com.example.ui.state.HomeDashboardState
import com.example.ui.state.MainUiState
import com.example.ui.state.MonthActivityStats
import com.example.ui.state.ProjectWithClient
import com.example.ui.state.VideoOutputPeriodPoint
import com.example.ui.state.YearActivityStats
import com.example.util.DateProvider
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

sealed interface UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent
}

class MainViewModel(
    private val repository: CareerRepository,
    private val backupExportManager: BackupExportManager,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val todayFlow = MutableStateFlow(dateProvider.today())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsExist()
        }
    }

    fun refreshDate() {
        todayFlow.value = dateProvider.today()
    }

    private data class CoreSnapshot(
        val today: LocalDate,
        val profile: EditorProfileEntity,
        val settings: AppSettingsEntity,
        val clients: List<ClientEntity>,
        val projects: List<ProjectEntity>
    )

    private val coreSnapshotFlow = combine(
        todayFlow,
        repository.editorProfile,
        repository.settings,
        repository.clients,
        repository.projects
    ) { today, profile, settings, clients, projects ->
        CoreSnapshot(today, profile, settings, clients, projects)
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreSnapshotFlow,
        repository.calendarDays,
        repository.incomeTransactions,
        repository.expenseTransactions
    ) { core, calendarDays, incomeList, expenseList ->
        buildMainUiState(core, calendarDays, incomeList, expenseList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(today = dateProvider.today(), isLoading = true)
    )

    private fun buildMainUiState(
        core: CoreSnapshot,
        calendarDays: List<CalendarDayEntity>,
        incomeList: List<IncomeTransactionEntity>,
        expenseList: List<ExpenseTransactionEntity>
    ): MainUiState {
        val today = core.today
        val todayEpoch = today.toEpochDay()
        val currentMonth = YearMonth.from(today)
        val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
        val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

        val clientMap = core.clients.associateBy { it.id }

        val projectsWithClient = core.projects.map { project ->
            val status = ProjectStatus.fromName(project.projectStatus)
            val remaining = if (status == ProjectStatus.CANCELLED) {
                0.0
            } else {
                (project.projectAmount - project.amountPaid).coerceAtLeast(0.0)
            }
            ProjectWithClient(
                project = project,
                clientName = project.clientId?.let { clientMap[it]?.name },
                remainingAmount = remaining,
                isOverdue = CareerRepository.isProjectOverdue(project, todayEpoch)
            )
        }

        val projectsByClient = projectsWithClient.groupBy { it.project.clientId }
        val incomeByClient = incomeList.groupBy { it.clientId }

        val clientsWithStats = core.clients.map { client ->
            val cProjects = projectsByClient[client.id].orEmpty()
            val cIncome = incomeByClient[client.id].orEmpty()
            val latestProjectActivity = cProjects.maxOfOrNull { it.project.updatedAtMillis } ?: 0L
            val latestIncomeActivity = cIncome.maxOfOrNull { it.createdAtMillis } ?: 0L
            val lastActivity = maxOf(client.updatedAtMillis, latestProjectActivity, latestIncomeActivity)

            ClientWithStats(
                client = client,
                totalProjects = cProjects.size,
                completedVideos = cProjects.count {
                    it.project.projectStatus == ProjectStatus.COMPLETED.name
                },
                totalEarningsReceived = cIncome.sumOf { it.amount },
                pendingPayments = cProjects.sumOf { it.remainingAmount },
                lastActivityMillis = lastActivity
            )
        }

        val startDate = LocalDate.ofEpochDay(core.profile.officialStartDateEpochDay)
        val experience = Formatters.calculateExperience(startDate, today)

        val daysByEpoch = calendarDays.associateBy { it.dateEpochDay }
        val validPastDays = calendarDays.filter { it.dateEpochDay <= todayEpoch }
        val totalEditingDays = validPastDays.count { it.status == CalendarStatus.DONE.name }
        val totalMissedDays = validPastDays.count { it.status == CalendarStatus.MISSED.name }
        val overallConsistency = Formatters.calculateConsistencyPercent(totalEditingDays, totalMissedDays)

        val totalRangeDays = if (!today.isBefore(startDate)) {
            ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        } else {
            0
        }
        val markedSinceStart = validPastDays.count {
            it.dateEpochDay >= startDate.toEpochDay() &&
                (it.status == CalendarStatus.DONE.name || it.status == CalendarStatus.MISSED.name)
        }
        val totalNotMarked = (totalRangeDays - markedSinceStart).coerceAtLeast(0)

        val monthCalendarDays = validPastDays.filter {
            it.dateEpochDay in monthStartEpoch..monthEndEpoch
        }
        val monthDoneDays = monthCalendarDays.count { it.status == CalendarStatus.DONE.name }
        val monthMissedDays = monthCalendarDays.count { it.status == CalendarStatus.MISSED.name }
        val monthConsistency = Formatters.calculateConsistencyPercent(monthDoneDays, monthMissedDays)

        val totalEarnings = incomeList.sumOf { it.amount }
        val totalExpenses = expenseList.sumOf { it.amount }
        val netBalance = totalEarnings - totalExpenses
        val pendingPayments = projectsWithClient.sumOf { it.remainingAmount }

        val completedProjects = projectsWithClient.filter {
            it.project.projectStatus == ProjectStatus.COMPLETED.name
        }
        val commissionedProjectsCount = projectsWithClient.count {
            it.project.category == VideoCategory.CLIENT_VIDEO.name
        }
        val monthCompletedVideos = completedProjects.count {
            it.project.projectDateEpochDay in monthStartEpoch..monthEndEpoch
        }
        val monthEarnings = incomeList
            .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
            .sumOf { it.amount }
        val monthExpenses = expenseList
            .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
            .sumOf { it.amount }

        val activeProjects = projectsWithClient.filter {
            val s = ProjectStatus.fromName(it.project.projectStatus)
            s == ProjectStatus.PENDING || s == ProjectStatus.IN_PROGRESS
        }
        val overdueCount = activeProjects.count { it.isOverdue }

        val homeState = HomeDashboardState(
            editorName = core.profile.name,
            brandName = core.profile.brandName,
            officialStartDate = startDate,
            experience = experience,
            totalEditingDays = totalEditingDays,
            totalMissedDays = totalMissedDays,
            consistencyPercent = overallConsistency,
            totalClients = core.clients.size,
            totalVideosCompleted = completedProjects.size,
            totalProjects = core.projects.size,
            commissionedProjectsCount = commissionedProjectsCount,
            totalEarnings = totalEarnings,
            totalExpenses = totalExpenses,
            netBalance = netBalance,
            pendingClientPayments = pendingPayments,
            currentMonthCompletedVideos = monthCompletedVideos,
            currentMonthEarnings = monthEarnings,
            currentMonthExpenses = monthExpenses,
            currentMonthEditingDays = monthDoneDays,
            currentMonthConsistency = monthConsistency,
            overdueProjectsCount = overdueCount,
            activeProjects = activeProjects.take(5)
        )

        val calendarOverview = CalendarOverviewState(
            totalEditingDays = totalEditingDays,
            totalMissedDays = totalMissedDays,
            totalNotMarkedDays = totalNotMarked,
            consistencyPercent = overallConsistency,
            daysByEpoch = daysByEpoch,
            officialStartDate = startDate
        )

        return MainUiState(
            today = today,
            editorProfile = core.profile,
            settings = core.settings,
            home = homeState,
            clientsWithStats = clientsWithStats,
            projectsWithClient = projectsWithClient,
            calendarOverview = calendarOverview,
            incomeTransactions = incomeList,
            expenseTransactions = expenseList,
            isLoading = false
        )
    }

    // --- Calendar computations ---

    fun getMonthActivityStats(yearMonth: YearMonth, state: MainUiState): MonthActivityStats {
        val todayEpoch = state.today.toEpochDay()
        val daysInMonth = yearMonth.lengthOfMonth()
        var done = 0
        var missed = 0
        var notMarked = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val epoch = date.toEpochDay()
            if (epoch > todayEpoch) {
                notMarked++
                continue
            }
            val status = state.calendarOverview.daysByEpoch[epoch]?.status?.let {
                CalendarStatus.fromName(it)
            } ?: CalendarStatus.NOT_MARKED
            when (status) {
                CalendarStatus.DONE -> done++
                CalendarStatus.MISSED -> missed++
                CalendarStatus.NOT_MARKED -> notMarked++
            }
        }
        return MonthActivityStats(
            yearMonth = yearMonth,
            doneDays = done,
            missedDays = missed,
            notMarkedDays = notMarked,
            consistencyPercent = Formatters.calculateConsistencyPercent(done, missed)
        )
    }

    fun getYearActivityStats(year: Int, state: MainUiState): YearActivityStats {
        var done = 0
        var missed = 0
        var notMarked = 0
        for (m in 1..12) {
            val ms = getMonthActivityStats(YearMonth.of(year, m), state)
            done += ms.doneDays
            missed += ms.missedDays
            notMarked += ms.notMarkedDays
        }
        return YearActivityStats(
            year = year,
            doneDays = done,
            missedDays = missed,
            notMarkedDays = notMarked,
            consistencyPercent = Formatters.calculateConsistencyPercent(done, missed)
        )
    }

    // --- Analytics series computations ---

    fun computeFinancialSeries(mode: String, state: MainUiState): List<FinancialPeriodPoint> {
        val today = state.today
        return when (mode) {
            "Yearly" -> {
                val years = (state.incomeTransactions.map { LocalDate.ofEpochDay(it.dateEpochDay).year } +
                    state.expenseTransactions.map { LocalDate.ofEpochDay(it.dateEpochDay).year } +
                    listOf(today.year, state.home.officialStartDate.year))
                    .distinct()
                    .sorted()
                years.map { year ->
                    val start = LocalDate.of(year, 1, 1).toEpochDay()
                    val end = LocalDate.of(year, 12, 31).toEpochDay()
                    val inc = state.incomeTransactions.filter { it.dateEpochDay in start..end }.sumOf { it.amount }
                    val exp = state.expenseTransactions.filter { it.dateEpochDay in start..end }.sumOf { it.amount }
                    FinancialPeriodPoint(label = year.toString(), income = inc, expense = exp)
                }
            }
            "All Time" -> {
                listOf(
                    FinancialPeriodPoint(
                        label = "All Time",
                        income = state.home.totalEarnings,
                        expense = state.home.totalExpenses
                    )
                )
            }
            else -> {
                // Monthly: last 6 months ending at today's month
                val currentMonth = YearMonth.from(today)
                (5 downTo 0).map { offset ->
                    val ym = currentMonth.minusMonths(offset.toLong())
                    val start = ym.atDay(1).toEpochDay()
                    val end = ym.atEndOfMonth().toEpochDay()
                    val inc = state.incomeTransactions.filter { it.dateEpochDay in start..end }.sumOf { it.amount }
                    val exp = state.expenseTransactions.filter { it.dateEpochDay in start..end }.sumOf { it.amount }
                    FinancialPeriodPoint(
                        label = Formatters.formatShortMonth(ym.year, ym.monthValue),
                        income = inc,
                        expense = exp
                    )
                }
            }
        }
    }

    fun computeVideoOutputSeries(categoryFilter: VideoCategory?, state: MainUiState): List<VideoOutputPeriodPoint> {
        val currentMonth = YearMonth.from(state.today)
        val completed = state.projectsWithClient.filter {
            it.project.projectStatus == ProjectStatus.COMPLETED.name &&
                (categoryFilter == null || it.project.category == categoryFilter.name)
        }
        return (5 downTo 0).map { offset ->
            val ym = currentMonth.minusMonths(offset.toLong())
            val start = ym.atDay(1).toEpochDay()
            val end = ym.atEndOfMonth().toEpochDay()
            val count = completed.count { it.project.projectDateEpochDay in start..end }
            VideoOutputPeriodPoint(
                label = Formatters.formatShortMonth(ym.year, ym.monthValue),
                completedCount = count
            )
        }
    }

    fun computeAverageVideosPerMonth(state: MainUiState): Double {
        val completedCount = state.home.totalVideosCompleted
        if (completedCount == 0) return 0.0
        val startYm = YearMonth.from(state.home.officialStartDate)
        val nowYm = YearMonth.from(state.today)
        val monthsElapsed = if (nowYm.isBefore(startYm)) {
            1
        } else {
            (ChronoUnit.MONTHS.between(startYm, nowYm).toInt() + 1).coerceAtLeast(1)
        }
        return completedCount.toDouble() / monthsElapsed.toDouble()
    }

    fun searchAll(query: String, state: MainUiState): GlobalSearchResults {
        val q = query.trim().lowercase()
        if (q.isBlank()) return GlobalSearchResults()
        return GlobalSearchResults(
            clients = state.clientsWithStats.filter {
                it.client.name.lowercase().contains(q) ||
                    it.client.preferredEditingStyle.lowercase().contains(q) ||
                    it.client.platform.lowercase().contains(q)
            },
            projects = state.projectsWithClient.filter {
                it.project.title.lowercase().contains(q) ||
                    (it.clientName?.lowercase()?.contains(q) == true) ||
                    it.project.projectType.lowercase().contains(q)
            },
            income = state.incomeTransactions.filter {
                it.source.lowercase().contains(q) ||
                    it.clientNameSnapshot.lowercase().contains(q) ||
                    it.projectTitleSnapshot.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q)
            },
            expenses = state.expenseTransactions.filter {
                it.description.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q)
            }
        )
    }

    // --- Mutations ---

    fun setCalendarDayStatus(epochDay: Long, status: CalendarStatus, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.setCalendarDayStatus(epochDay, status, note)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Calendar updated: ${status.label}"))
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to update calendar", isError = true))
            }
        }
    }

    fun saveClient(client: ClientEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveClient(client)
            }.onSuccess { id ->
                _events.tryEmit(UiEvent.ShowSnackbar("Client saved successfully"))
                onSaved(id)
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save client", isError = true))
            }
        }
    }

    fun deleteClient(clientId: Long, deleteAssociatedProjects: Boolean = false, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.deleteClientSafe(clientId, deleteAssociatedProjects)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Client deleted (financial history preserved)"))
                onDeleted()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete client", isError = true))
            }
        }
    }

    fun saveProject(project: ProjectEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveProject(project)
            }.onSuccess { id ->
                _events.tryEmit(UiEvent.ShowSnackbar("Project saved successfully"))
                onSaved(id)
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save project", isError = true))
            }
        }
    }

    fun deleteProject(projectId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProjectSafe(projectId)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Project deleted"))
                onDeleted()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete project", isError = true))
            }
        }
    }

    fun recordProjectPayment(
        projectId: Long,
        amount: Double,
        dateEpochDay: Long,
        paymentMethod: PaymentMethod,
        notes: String = "",
        onRecorded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordProjectPayment(projectId, amount, dateEpochDay, paymentMethod, notes)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Payment recorded in ledger"))
                onRecorded()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to record payment", isError = true))
            }
        }
    }

    fun saveIncome(income: IncomeTransactionEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveIncome(income)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Income entry saved"))
                onSaved()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save income", isError = true))
            }
        }
    }

    fun deleteIncome(incomeId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteIncome(incomeId)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Income entry deleted"))
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete income", isError = true))
            }
        }
    }

    fun saveExpense(expense: ExpenseTransactionEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveExpense(expense)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Expense entry saved"))
                onSaved()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save expense", isError = true))
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteExpense(expenseId)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Expense entry deleted"))
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete expense", isError = true))
            }
        }
    }

    fun saveEditorProfile(profile: EditorProfileEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveEditorProfile(profile)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Editor profile updated"))
                onSaved()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to update profile", isError = true))
            }
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    suspend fun buildBackupJson(): String {
        val snapshot = repository.getAllSnapshotForBackup()
        return backupExportManager.serializeBackup(snapshot)
    }

    fun parseBackupJson(json: String): Result<BackupPayloadDto> {
        return backupExportManager.parseAndValidateBackup(json)
    }

    fun restoreBackup(dto: BackupPayloadDto, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val snapshot = dto.toSnapshotValidated()
                repository.restoreFromSnapshot(snapshot)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Backup restored successfully"))
                onDone()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Restore failed", isError = true))
            }
        }
    }

    suspend fun buildCsvExport(target: ExportTarget): String {
        val snapshot = repository.getAllSnapshotForBackup()
        val format = DateFormatOption.fromName(snapshot.appSettings.dateFormat)
        return backupExportManager.generateCsv(target, snapshot, dateProvider.today(), format)
    }

    fun notifyMessage(message: String, isError: Boolean = false) {
        _events.tryEmit(UiEvent.ShowSnackbar(message, isError))
    }

    companion object {
        fun factory(
            repository: CareerRepository,
            backupExportManager: BackupExportManager,
            dateProvider: DateProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, backupExportManager, dateProvider) as T
            }
        }
    }
}
