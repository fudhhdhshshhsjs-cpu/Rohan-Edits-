package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupExportManager
import com.example.data.backup.BackupFileInspection
import com.example.data.backup.BackupPayloadDto
import com.example.data.backup.ExportTarget
import com.example.data.backup.RestoreSummary
import com.example.data.backup.toSnapshotValidated
import com.example.data.local.ClientFormDraft
import com.example.data.local.DraftManager
import com.example.data.local.NoteFormDraft
import com.example.data.local.ProjectFormDraft
import com.example.data.local.dao.ClientPickerOption
import com.example.data.local.dao.ClientSummaryRow
import com.example.data.local.dao.ClientTypeBreakdownAggregate
import com.example.data.local.dao.HomeMetricsAggregate
import com.example.data.local.dao.ProjectPickerOption
import com.example.data.local.dao.ProjectSummaryRow
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.CalendarStatus
import com.example.data.model.ClientSortOption
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.FinancialDateFilter
import com.example.data.model.GoalStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectSortOption
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.RestoreMode
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
import com.example.ui.state.BudgetPagedState
import com.example.ui.state.CalendarOverviewState
import com.example.ui.state.ClientAnalyticsSummary
import com.example.ui.state.ClientDetailState
import com.example.ui.state.ClientWithStats
import com.example.ui.state.ClientsPagedState
import com.example.ui.state.FinancialPeriodPoint
import com.example.ui.state.GlobalSearchPagedState
import com.example.ui.state.GlobalSearchResults
import com.example.ui.state.GoalsPagedState
import com.example.ui.state.HomeDashboardState
import com.example.ui.state.MainUiState
import com.example.ui.state.MonthActivityStats
import com.example.ui.state.NotesPagedState
import com.example.ui.state.ProjectDetailState
import com.example.ui.state.ProjectWithClient
import com.example.ui.state.ProjectsPagedState
import com.example.ui.state.StorageManagementState
import com.example.ui.state.VideoOutputPeriodPoint
import com.example.ui.state.YearActivityStats
import com.example.util.DateProvider
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

sealed interface UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: CareerRepository,
    private val backupExportManager: BackupExportManager,
    private val dateProvider: DateProvider,
    private val draftManager: DraftManager? = null
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 50

        fun factory(
            repository: CareerRepository,
            backupExportManager: BackupExportManager,
            dateProvider: DateProvider,
            draftManager: DraftManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, backupExportManager, dateProvider, draftManager) as T
            }
        }
    }

    private val todayFlow = MutableStateFlow(dateProvider.today())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    val storageError: StateFlow<String?> = repository.lastStorageError

    fun clearStorageError() = repository.clearStorageError()

    // --- Paginated Screen States (only active window loaded into RAM) ---

    private val _clientsListState = MutableStateFlow(ClientsPagedState(isLoading = true))
    val clientsListState: StateFlow<ClientsPagedState> = _clientsListState.asStateFlow()

    private val _projectsListState = MutableStateFlow(ProjectsPagedState(isLoading = true))
    val projectsListState: StateFlow<ProjectsPagedState> = _projectsListState.asStateFlow()

    private val _budgetListState = MutableStateFlow(BudgetPagedState(isLoading = true))
    val budgetListState: StateFlow<BudgetPagedState> = _budgetListState.asStateFlow()

    private val _notesListState = MutableStateFlow(NotesPagedState(isLoading = true))
    val notesListState: StateFlow<NotesPagedState> = _notesListState.asStateFlow()

    private val _goalsListState = MutableStateFlow(GoalsPagedState(isLoading = true))
    val goalsListState: StateFlow<GoalsPagedState> = _goalsListState.asStateFlow()

    private val _globalSearchState = MutableStateFlow(GlobalSearchPagedState())
    val globalSearchState: StateFlow<GlobalSearchPagedState> = _globalSearchState.asStateFlow()

    private val _storageState = MutableStateFlow(StorageManagementState())
    val storageState: StateFlow<StorageManagementState> = _storageState.asStateFlow()

    private var clientsJob: Job? = null
    private var projectsJob: Job? = null
    private var budgetJob: Job? = null
    private var notesJob: Job? = null
    private var goalsJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultsExist()
            reloadAllPagedLists()
        }
    }

    fun refreshDate() {
        todayFlow.value = dateProvider.today()
    }

    private data class CoreHeader(
        val today: LocalDate,
        val profile: EditorProfileEntity,
        val settings: AppSettingsEntity,
        val clientBreakdown: ClientTypeBreakdownAggregate,
        val topClients: List<ClientSummaryRow>
    )

    private val coreHeaderFlow = combine(
        todayFlow,
        repository.editorProfile,
        repository.settings,
        repository.observeClientTypeBreakdown(),
        repository.observeTopActiveClients(5)
    ) { today, profile, settings, breakdown, topClients ->
        CoreHeader(today, profile, settings, breakdown, topClients)
    }

    private val homeMetricsFlow = combine(
        todayFlow,
        repository.editorProfile
    ) { today, profile ->
        today to LocalDate.ofEpochDay(profile.officialStartDateEpochDay)
    }
        .distinctUntilChanged()
        .flatMapLatest { (today, startDate) ->
            repository.observeHomeMetrics(today, startDate)
        }

    val uiState: StateFlow<MainUiState> = combine(
        coreHeaderFlow,
        homeMetricsFlow,
        repository.observeRecentActiveProjects(5),
        repository.observeRecentProjects(50),
        repository.calendarDays
    ) { core, metrics, activeRows, recentRows, calendarDays ->
        buildMainUiState(core, metrics, activeRows, recentRows, calendarDays)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(today = dateProvider.today(), isLoading = true)
        )

    private suspend fun buildMainUiState(
        core: CoreHeader,
        metrics: HomeMetricsAggregate,
        activeRows: List<ProjectSummaryRow>,
        recentRows: List<ProjectSummaryRow>,
        calendarDays: List<CalendarDayEntity>
    ): MainUiState {
        val today = core.today
        val todayEpoch = today.toEpochDay()
        val startDate = LocalDate.ofEpochDay(core.profile.officialStartDateEpochDay)
        val experience = Formatters.calculateExperience(startDate, today)

        val overallConsistency = Formatters.calculateConsistencyPercent(
            metrics.totalEditingDays,
            metrics.totalMissedDays
        )
        val monthConsistency = Formatters.calculateConsistencyPercent(
            metrics.monthDoneDays,
            metrics.monthMissedDays
        )

        val totalRangeDays = if (!today.isBefore(startDate)) {
            ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        } else {
            0
        }
        val totalNotMarked = (totalRangeDays - metrics.markedSinceStart).coerceAtLeast(0)

        val activeProjects = activeRows.map { it.toProjectWithClient(todayEpoch) }
        val recentProjects = recentRows.map { it.toProjectWithClient(todayEpoch) }
        val topClients = core.topClients.map { it.toClientWithStats() }

        val homeState = HomeDashboardState(
            editorName = core.profile.name,
            brandName = core.profile.brandName,
            officialStartDate = startDate,
            experience = experience,
            totalEditingDays = metrics.totalEditingDays,
            totalMissedDays = metrics.totalMissedDays,
            consistencyPercent = overallConsistency,
            totalClients = metrics.totalClients,
            totalVideosCompleted = metrics.totalVideosCompleted,
            totalProjects = metrics.totalProjects,
            commissionedProjectsCount = metrics.commissionedProjectsCount,
            totalEarnings = metrics.totalEarnings,
            totalExpenses = metrics.totalExpenses,
            netBalance = metrics.totalEarnings - metrics.totalExpenses,
            pendingClientPayments = metrics.pendingClientPayments,
            currentMonthCompletedVideos = metrics.monthCompletedVideos,
            currentMonthEarnings = metrics.monthEarnings,
            currentMonthExpenses = metrics.monthExpenses,
            currentMonthEditingDays = metrics.monthDoneDays,
            currentMonthConsistency = monthConsistency,
            overdueProjectsCount = metrics.overdueProjectsCount,
            activeProjects = activeProjects
        )

        val daysByEpoch = calendarDays.associateBy { it.dateEpochDay }
        val calendarOverview = CalendarOverviewState(
            totalEditingDays = metrics.totalEditingDays,
            totalMissedDays = metrics.totalMissedDays,
            totalNotMarkedDays = totalNotMarked,
            consistencyPercent = overallConsistency,
            daysByEpoch = daysByEpoch,
            officialStartDate = startDate
        )

        // Compute SQL-backed Analytics Series (fast O(log N) range queries, zero full-table loads)
        val monthlyFin = repository.getMonthlyFinancialSeries(today)
        val yearlyFin = repository.getYearlyFinancialSeries(today, startDate)
        val vidAll = repository.getMonthlyVideoOutputSeries(today, null)
        val vidClient = repository.getMonthlyVideoOutputSeries(today, VideoCategory.CLIENT_VIDEO)
        val vidPersonal = repository.getMonthlyVideoOutputSeries(today, VideoCategory.PERSONAL_VIDEO)
        val vidPortfolio = repository.getMonthlyVideoOutputSeries(today, VideoCategory.PORTFOLIO_VIDEO)
        val vidPractice = repository.getMonthlyVideoOutputSeries(today, VideoCategory.PRACTICE_VIDEO)

        return MainUiState(
            today = today,
            editorProfile = core.profile,
            settings = core.settings,
            home = homeState,
            clientsWithStats = topClients,
            projectsWithClient = recentProjects,
            calendarOverview = calendarOverview,
            incomeTransactions = emptyList(),
            expenseTransactions = emptyList(),
            clientAnalytics = ClientAnalyticsSummary(
                total = core.clientBreakdown.total,
                newCount = core.clientBreakdown.newCount,
                oldCount = core.clientBreakdown.oldCount,
                regularCount = core.clientBreakdown.regularCount,
                topClients = topClients
            ),
            monthlyFinancialSeries = monthlyFin,
            yearlyFinancialSeries = yearlyFin,
            monthlyVideoSeriesAll = vidAll,
            monthlyVideoSeriesClient = vidClient,
            monthlyVideoSeriesPersonal = vidPersonal,
            monthlyVideoSeriesPortfolio = vidPortfolio,
            monthlyVideoSeriesPractice = vidPractice,
            isLoading = false
        )
    }

    // --- Row Projection Mappers ---

    private fun ClientSummaryRow.toClientWithStats(): ClientWithStats = ClientWithStats(
        client = ClientEntity(
            id = id,
            name = name,
            platform = platform,
            clientType = clientType,
            perVideoPrice = perVideoPrice,
            updatedAtMillis = updatedAtMillis
        ),
        totalProjects = totalProjects,
        completedVideos = completedVideos,
        totalEarningsReceived = totalEarningsReceived,
        pendingPayments = pendingPayments,
        lastActivityMillis = lastActivityMillis
    )

    private fun ProjectSummaryRow.toProjectWithClient(todayEpoch: Long): ProjectWithClient {
        val status = ProjectStatus.fromName(projectStatus)
        val remaining = if (status == ProjectStatus.CANCELLED) {
            0.0
        } else {
            (projectAmount - amountPaid).coerceAtLeast(0.0)
        }
        return ProjectWithClient(
            project = ProjectEntity(
                id = id,
                clientId = clientId,
                category = category,
                title = title,
                projectType = projectType,
                priority = priority,
                videoDuration = videoDuration,
                projectDateEpochDay = projectDateEpochDay,
                deadlineEpochDay = deadlineEpochDay,
                projectAmount = projectAmount,
                amountPaid = amountPaid,
                paymentStatus = paymentStatus,
                projectStatus = projectStatus,
                clientRating = clientRating,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            ),
            clientName = clientName,
            remainingAmount = remaining,
            isOverdue = CareerRepository.isProjectOverdue(projectStatus, deadlineEpochDay, todayEpoch)
        )
    }

    // --- Paginated Controllers ---

    fun reloadAllPagedLists() {
        reloadClientsPage(reset = true)
        reloadProjectsPage(reset = true)
        reloadBudgetPage(reset = true)
        reloadNotesPage(reset = true)
        reloadGoalsPage(reset = true)
        if (_globalSearchState.value.query.isNotBlank()) {
            updateGlobalSearch(_globalSearchState.value.query)
        }
    }

    fun updateClientFilter(
        query: String = _clientsListState.value.query,
        clientType: ClientType? = _clientsListState.value.clientType,
        sortOption: ClientSortOption = _clientsListState.value.sortOption
    ) {
        _clientsListState.update {
            it.copy(query = query, clientType = clientType, sortOption = sortOption)
        }
        reloadClientsPage(reset = true)
    }

    fun loadMoreClients() {
        val s = _clientsListState.value
        if (s.isLoading || !s.hasMore) return
        reloadClientsPage(reset = false)
    }

    fun reloadClientsPage(reset: Boolean = true) {
        clientsJob?.cancel()
        clientsJob = viewModelScope.launch {
            val current = _clientsListState.value
            val offset = if (reset) 0 else current.items.size
            _clientsListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getClientsPage(
                    query = current.query,
                    clientType = current.clientType,
                    sortOption = current.sortOption,
                    limit = PAGE_SIZE,
                    offset = offset
                )
            }.onSuccess { page ->
                val mapped = page.items.map { it.toClientWithStats() }
                _clientsListState.update { state ->
                    val merged = if (reset) mapped else (state.items + mapped).distinctBy { it.client.id }
                    state.copy(
                        items = merged,
                        totalCount = page.totalCount,
                        hasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _clientsListState.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Failed to load clients", isError = true))
            }
        }
    }

    fun updateProjectFilter(
        query: String = _projectsListState.value.query,
        clientId: Long? = _projectsListState.value.clientId,
        category: VideoCategory? = _projectsListState.value.category,
        projectType: ProjectType? = _projectsListState.value.projectType,
        priority: ProjectPriority? = _projectsListState.value.priority,
        paymentStatus: PaymentStatus? = _projectsListState.value.paymentStatus,
        projectStatus: ProjectStatus? = _projectsListState.value.projectStatus,
        exactDate: Long? = _projectsListState.value.exactDate,
        deadlineBefore: Long? = _projectsListState.value.deadlineBefore,
        onlyOverdue: Boolean = _projectsListState.value.onlyOverdue,
        sortOption: ProjectSortOption = _projectsListState.value.sortOption
    ) {
        _projectsListState.update {
            it.copy(
                query = query,
                clientId = clientId,
                category = category,
                projectType = projectType,
                priority = priority,
                paymentStatus = paymentStatus,
                projectStatus = projectStatus,
                exactDate = exactDate,
                deadlineBefore = deadlineBefore,
                onlyOverdue = onlyOverdue,
                sortOption = sortOption
            )
        }
        reloadProjectsPage(reset = true)
    }

    fun loadMoreProjects() {
        val s = _projectsListState.value
        if (s.isLoading || !s.hasMore) return
        reloadProjectsPage(reset = false)
    }

    fun reloadProjectsPage(reset: Boolean = true) {
        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            val current = _projectsListState.value
            val offset = if (reset) 0 else current.items.size
            val todayEpoch = todayFlow.value.toEpochDay()
            _projectsListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getProjectsPage(
                    query = current.query,
                    clientId = current.clientId,
                    category = current.category,
                    projectType = current.projectType,
                    priority = current.priority,
                    paymentStatus = current.paymentStatus,
                    projectStatus = current.projectStatus,
                    exactDate = current.exactDate,
                    deadlineBefore = current.deadlineBefore,
                    onlyOverdue = current.onlyOverdue,
                    todayEpoch = todayEpoch,
                    sortOption = current.sortOption,
                    limit = PAGE_SIZE,
                    offset = offset
                )
            }.onSuccess { page ->
                val mapped = page.items.map { it.toProjectWithClient(todayEpoch) }
                _projectsListState.update { state ->
                    val merged = if (reset) mapped else (state.items + mapped).distinctBy { it.project.id }
                    state.copy(
                        items = merged,
                        totalCount = page.totalCount,
                        hasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _projectsListState.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Failed to load projects", isError = true))
            }
        }
    }

    private fun computeBudgetDateRange(
        filter: FinancialDateFilter,
        today: LocalDate,
        customStart: Long?,
        customEnd: Long?
    ): Pair<Long?, Long?> {
        return when (filter) {
            FinancialDateFilter.THIS_MONTH -> {
                val ym = YearMonth.from(today)
                ym.atDay(1).toEpochDay() to ym.atEndOfMonth().toEpochDay()
            }
            FinancialDateFilter.PREVIOUS_MONTH -> {
                val ym = YearMonth.from(today).minusMonths(1)
                ym.atDay(1).toEpochDay() to ym.atEndOfMonth().toEpochDay()
            }
            FinancialDateFilter.THIS_YEAR -> {
                LocalDate.of(today.year, 1, 1).toEpochDay() to LocalDate.of(today.year, 12, 31).toEpochDay()
            }
            FinancialDateFilter.ALL_TIME -> null to null
            FinancialDateFilter.CUSTOM -> {
                val s = customStart ?: today.withDayOfMonth(1).toEpochDay()
                val e = customEnd ?: today.toEpochDay()
                minOf(s, e) to maxOf(s, e)
            }
        }
    }

    fun updateBudgetFilter(
        dateFilter: FinancialDateFilter = _budgetListState.value.dateFilter,
        customStartEpoch: Long? = _budgetListState.value.customStartEpoch,
        customEndEpoch: Long? = _budgetListState.value.customEndEpoch,
        query: String = _budgetListState.value.query
    ) {
        _budgetListState.update {
            it.copy(
                dateFilter = dateFilter,
                customStartEpoch = customStartEpoch,
                customEndEpoch = customEndEpoch,
                query = query
            )
        }
        reloadBudgetPage(reset = true)
    }

    fun loadMoreBudgetIncome() {
        val s = _budgetListState.value
        if (s.isLoading || !s.incomeHasMore) return
        viewModelScope.launch {
            val today = todayFlow.value
            val (startEpoch, endEpoch) = computeBudgetDateRange(s.dateFilter, today, s.customStartEpoch, s.customEndEpoch)
            _budgetListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getIncomePage(s.query, startEpoch, endEpoch, PAGE_SIZE, s.incomeItems.size)
            }.onSuccess { page ->
                _budgetListState.update { state ->
                    state.copy(
                        incomeItems = (state.incomeItems + page.items).distinctBy { it.id },
                        incomeTotalCount = page.totalCount,
                        incomeHasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure {
                _budgetListState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadMoreBudgetExpenses() {
        val s = _budgetListState.value
        if (s.isLoading || !s.expenseHasMore) return
        viewModelScope.launch {
            val today = todayFlow.value
            val (startEpoch, endEpoch) = computeBudgetDateRange(s.dateFilter, today, s.customStartEpoch, s.customEndEpoch)
            _budgetListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getExpensesPage(s.query, startEpoch, endEpoch, PAGE_SIZE, s.expenseItems.size)
            }.onSuccess { page ->
                _budgetListState.update { state ->
                    state.copy(
                        expenseItems = (state.expenseItems + page.items).distinctBy { it.id },
                        expenseTotalCount = page.totalCount,
                        expenseHasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure {
                _budgetListState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun reloadBudgetPage(reset: Boolean = true) {
        budgetJob?.cancel()
        budgetJob = viewModelScope.launch {
            val current = _budgetListState.value
            val today = todayFlow.value
            val (startEpoch, endEpoch) = computeBudgetDateRange(
                current.dateFilter, today, current.customStartEpoch, current.customEndEpoch
            )
            val yearStart = LocalDate.of(today.year, 1, 1).toEpochDay()
            val yearEnd = LocalDate.of(today.year, 12, 31).toEpochDay()

            _budgetListState.update { it.copy(isLoading = true) }
            runCatching {
                val incPage = repository.getIncomePage(current.query, startEpoch, endEpoch, PAGE_SIZE, 0)
                val expPage = repository.getExpensesPage(current.query, startEpoch, endEpoch, PAGE_SIZE, 0)
                val (periodInc, periodExp) = repository.getFinancialPeriodTotals(startEpoch, endEpoch)
                val (yearInc, yearExp) = repository.getFinancialPeriodTotals(yearStart, yearEnd)
                Triple(incPage to expPage, periodInc to periodExp, yearInc to yearExp)
            }.onSuccess { (pages, periodTotals, yearTotals) ->
                val (incPage, expPage) = pages
                _budgetListState.update { state ->
                    state.copy(
                        incomeItems = incPage.items,
                        incomeTotalCount = incPage.totalCount,
                        incomeHasMore = incPage.hasMore,
                        expenseItems = expPage.items,
                        expenseTotalCount = expPage.totalCount,
                        expenseHasMore = expPage.hasMore,
                        periodIncome = periodTotals.first,
                        periodExpenses = periodTotals.second,
                        yearlyIncome = yearTotals.first,
                        yearlyExpenses = yearTotals.second,
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _budgetListState.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Failed to load budget ledger", isError = true))
            }
        }
    }

    fun updateNotesFilter(
        query: String = _notesListState.value.query,
        category: String? = _notesListState.value.category
    ) {
        _notesListState.update { it.copy(query = query, category = category) }
        reloadNotesPage(reset = true)
    }

    fun loadMoreNotes() {
        val s = _notesListState.value
        if (s.isLoading || !s.hasMore) return
        reloadNotesPage(reset = false)
    }

    fun reloadNotesPage(reset: Boolean = true) {
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            val current = _notesListState.value
            val offset = if (reset) 0 else current.items.size
            _notesListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getNotesPage(current.query, current.category, null, null, PAGE_SIZE, offset)
            }.onSuccess { page ->
                _notesListState.update { state ->
                    val merged = if (reset) page.items else (state.items + page.items).distinctBy { it.id }
                    state.copy(
                        items = merged,
                        totalCount = page.totalCount,
                        hasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure {
                _notesListState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateGoalsFilter(
        query: String = _goalsListState.value.query,
        status: GoalStatus? = _goalsListState.value.status
    ) {
        _goalsListState.update { it.copy(query = query, status = status) }
        reloadGoalsPage(reset = true)
    }

    fun loadMoreGoals() {
        val s = _goalsListState.value
        if (s.isLoading || !s.hasMore) return
        reloadGoalsPage(reset = false)
    }

    fun reloadGoalsPage(reset: Boolean = true) {
        goalsJob?.cancel()
        goalsJob = viewModelScope.launch {
            val current = _goalsListState.value
            val offset = if (reset) 0 else current.items.size
            _goalsListState.update { it.copy(isLoading = true) }
            runCatching {
                repository.getGoalsPage(current.query, current.status, PAGE_SIZE, offset)
            }.onSuccess { page ->
                _goalsListState.update { state ->
                    val merged = if (reset) page.items else (state.items + page.items).distinctBy { it.id }
                    state.copy(
                        items = merged,
                        totalCount = page.totalCount,
                        hasMore = page.hasMore,
                        isLoading = false
                    )
                }
            }.onFailure {
                _goalsListState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateGlobalSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            searchJob?.cancel()
            _globalSearchState.value = GlobalSearchPagedState(query = query)
            return
        }
        _globalSearchState.update { it.copy(query = query, isSearching = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val todayEpoch = todayFlow.value.toEpochDay()
            runCatching {
                val clients = repository.getClientsPage(trimmed, null, ClientSortOption.RECENT_ACTIVITY, 25, 0)
                val projects = repository.getProjectsPage(
                    query = trimmed,
                    todayEpoch = todayEpoch,
                    limit = 25,
                    offset = 0
                )
                val income = repository.getIncomePage(trimmed, null, null, 25, 0)
                val expenses = repository.getExpensesPage(trimmed, null, null, 25, 0)
                val notes = repository.getNotesPage(trimmed, null, null, null, 25, 0)
                val goals = repository.getGoalsPage(trimmed, null, 25, 0)

                GlobalSearchPagedState(
                    query = query,
                    isSearching = false,
                    clients = clients.items.map { it.toClientWithStats() },
                    clientsTotal = clients.totalCount,
                    projects = projects.items.map { it.toProjectWithClient(todayEpoch) },
                    projectsTotal = projects.totalCount,
                    income = income.items,
                    incomeTotal = income.totalCount,
                    expenses = expenses.items,
                    expensesTotal = expenses.totalCount,
                    notes = notes.items,
                    notesTotal = notes.totalCount,
                    goals = goals.items,
                    goalsTotal = goals.totalCount
                )
            }.onSuccess { res ->
                _globalSearchState.value = res
            }.onFailure { err ->
                _globalSearchState.update { it.copy(isSearching = false) }
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Search failed", isError = true))
            }
        }
    }

    // --- Safe Full-Record Readers, Detail Loaders & Entity Pickers ---

    suspend fun getClientFullSafe(clientId: Long): ClientEntity? =
        repository.getClientFullSafe(clientId)

    suspend fun getProjectFullSafe(projectId: Long): ProjectEntity? =
        repository.getProjectFullSafe(projectId)

    suspend fun getNoteFullSafe(noteId: Long): NoteEntity? =
        repository.getNoteFullSafe(noteId)

    suspend fun loadClientDetail(clientId: Long): ClientDetailState = withContext(Dispatchers.IO) {
        val todayEpoch = todayFlow.value.toEpochDay()
        val client = repository.getClientFullSafe(clientId)
        if (client == null) {
            return@withContext ClientDetailState(isLoading = false, client = null)
        }
        val summary = repository.observeClientSummary(clientId).first()
        val projects = repository.observeProjectsForClient(clientId, 50).first().map { proj ->
            val st = ProjectStatus.fromName(proj.projectStatus)
            val rem = if (st == ProjectStatus.CANCELLED) 0.0 else (proj.projectAmount - proj.amountPaid).coerceAtLeast(0.0)
            ProjectWithClient(
                project = proj,
                clientName = client.name,
                remainingAmount = rem,
                isOverdue = CareerRepository.isProjectOverdue(proj, todayEpoch)
            )
        }
        val payments = repository.observeIncomeForClient(clientId, 50).first()
        ClientDetailState(
            isLoading = false,
            client = client,
            totalProjects = summary?.totalProjects ?: projects.size,
            completedVideos = summary?.completedVideos ?: 0,
            totalEarningsReceived = summary?.totalEarningsReceived ?: 0.0,
            pendingPayments = summary?.pendingPayments ?: 0.0,
            projects = projects,
            payments = payments
        )
    }

    suspend fun loadProjectDetail(projectId: Long): ProjectDetailState = withContext(Dispatchers.IO) {
        val todayEpoch = todayFlow.value.toEpochDay()
        val project = repository.getProjectFullSafe(projectId)
        if (project == null) {
            return@withContext ProjectDetailState(isLoading = false, project = null)
        }
        val client = project.clientId?.let { repository.getClientFullSafe(it) }
        val st = ProjectStatus.fromName(project.projectStatus)
        val remaining = if (st == ProjectStatus.CANCELLED) 0.0 else (project.projectAmount - project.amountPaid).coerceAtLeast(0.0)
        val payments = repository.observeIncomeForProject(projectId, 50).first()
        val revPage = repository.getRevisionsPage(projectId, 50, 0)
        ProjectDetailState(
            isLoading = false,
            project = project,
            clientName = client?.name,
            remainingAmount = remaining,
            isOverdue = CareerRepository.isProjectOverdue(project, todayEpoch),
            payments = payments,
            revisions = revPage.items,
            revisionsTotalCount = revPage.totalCount,
            revisionsHasMore = revPage.hasMore
        )
    }

    suspend fun searchClientsForPicker(query: String, limit: Int = 50): List<ClientPickerOption> =
        repository.searchClientsForPicker(query, limit)

    suspend fun searchProjectsForPicker(
        query: String,
        clientId: Long? = null,
        limit: Int = 50
    ): List<ProjectPickerOption> = repository.searchProjectsForPicker(query, clientId, limit)

    suspend fun getIncomeByIdOnce(incomeId: Long): IncomeTransactionEntity? =
        withContext(Dispatchers.IO) { repository.db.incomeDao().getIncomeByIdOnce(incomeId) }

    suspend fun getExpenseByIdOnce(expenseId: Long): ExpenseTransactionEntity? =
        withContext(Dispatchers.IO) { repository.db.expenseDao().getExpenseByIdOnce(expenseId) }

    suspend fun getDueOrOverdueProjectsForReminder(today: LocalDate): List<ProjectEntity> =
        repository.getDueOrOverdueProjectsForReminder(today)

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

    // --- Analytics series computations (SQL-backed via uiState) ---

    fun computeFinancialSeries(mode: String, state: MainUiState): List<FinancialPeriodPoint> {
        return when (mode) {
            "Yearly" -> state.yearlyFinancialSeries
            "All Time" -> listOf(
                FinancialPeriodPoint(
                    label = "All Time",
                    income = state.home.totalEarnings,
                    expense = state.home.totalExpenses
                )
            )
            else -> state.monthlyFinancialSeries
        }
    }

    fun computeVideoOutputSeries(categoryFilter: VideoCategory?, state: MainUiState): List<VideoOutputPeriodPoint> {
        return when (categoryFilter) {
            null -> state.monthlyVideoSeriesAll
            VideoCategory.CLIENT_VIDEO -> state.monthlyVideoSeriesClient
            VideoCategory.PERSONAL_VIDEO -> state.monthlyVideoSeriesPersonal
            VideoCategory.PORTFOLIO_VIDEO -> state.monthlyVideoSeriesPortfolio
            VideoCategory.PRACTICE_VIDEO -> state.monthlyVideoSeriesPractice
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
            income = emptyList(),
            expenses = emptyList()
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
                reloadClientsPage(reset = true)
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
                reloadClientsPage(reset = true)
                reloadProjectsPage(reset = true)
                reloadBudgetPage(reset = true)
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
                reloadProjectsPage(reset = true)
                reloadClientsPage(reset = true)
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
                reloadProjectsPage(reset = true)
                reloadClientsPage(reset = true)
                reloadBudgetPage(reset = true)
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
                reloadBudgetPage(reset = true)
                reloadProjectsPage(reset = true)
                reloadClientsPage(reset = true)
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
                reloadBudgetPage(reset = true)
                reloadProjectsPage(reset = true)
                reloadClientsPage(reset = true)
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
                reloadBudgetPage(reset = true)
                reloadProjectsPage(reset = true)
                reloadClientsPage(reset = true)
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
                reloadBudgetPage(reset = true)
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
                reloadBudgetPage(reset = true)
                _events.tryEmit(UiEvent.ShowSnackbar("Expense entry deleted"))
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete expense", isError = true))
            }
        }
    }

    fun saveNote(note: NoteEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveNote(note)
            }.onSuccess { id ->
                reloadNotesPage(reset = true)
                _events.tryEmit(UiEvent.ShowSnackbar("Note saved"))
                onSaved(id)
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save note", isError = true))
            }
        }
    }

    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch {
            runCatching {
                val note = repository.getNoteFullSafe(noteId) ?: return@runCatching
                repository.saveNote(note.copy(isPinned = !note.isPinned))
            }.onSuccess {
                reloadNotesPage(reset = true)
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to update note", isError = true))
            }
        }
    }

    fun deleteNote(noteId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.deleteNote(noteId)
            }.onSuccess {
                reloadNotesPage(reset = true)
                _events.tryEmit(UiEvent.ShowSnackbar("Note deleted"))
                onDeleted()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete note", isError = true))
            }
        }
    }

    fun addProjectRevision(
        projectId: Long,
        summary: String,
        instructionsSnapshot: String = "",
        statusSnapshot: String = "",
        onAdded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addProjectRevision(projectId, summary, instructionsSnapshot, statusSnapshot)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Revision logged"))
                onAdded()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to add revision", isError = true))
            }
        }
    }

    fun deleteProjectRevision(revisionId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProjectRevision(revisionId)
            }.onSuccess {
                _events.tryEmit(UiEvent.ShowSnackbar("Revision removed"))
                onDeleted()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to remove revision", isError = true))
            }
        }
    }

    fun saveGoal(goal: CareerGoalEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.saveGoal(goal)
            }.onSuccess { id ->
                reloadGoalsPage(reset = true)
                _events.tryEmit(UiEvent.ShowSnackbar("Career goal saved"))
                onSaved(id)
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to save goal", isError = true))
            }
        }
    }

    fun deleteGoal(goalId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.deleteGoal(goalId)
            }.onSuccess {
                reloadGoalsPage(reset = true)
                _events.tryEmit(UiEvent.ShowSnackbar("Goal deleted"))
                onDeleted()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Failed to delete goal", isError = true))
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

    fun autoSaveEditorProfile(profile: EditorProfileEntity, onAutoSaved: () -> Unit = {}) {
        if (profile.name.isBlank() || profile.brandName.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.saveEditorProfile(profile)
            }.onSuccess {
                onAutoSaved()
            }
        }
    }

    // --- Drafts ---

    fun saveClientDraft(draft: ClientFormDraft) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.saveClientDraft(draft) }
    }

    suspend fun getClientDraft(clientId: Long?): ClientFormDraft? =
        draftManager?.getClientDraft(clientId)

    fun clearClientDraft(clientId: Long?) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.clearClientDraft(clientId) }
    }

    fun saveProjectDraft(draft: ProjectFormDraft) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.saveProjectDraft(draft) }
    }

    suspend fun getProjectDraft(projectId: Long?): ProjectFormDraft? =
        draftManager?.getProjectDraft(projectId)

    fun clearProjectDraft(projectId: Long?) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.clearProjectDraft(projectId) }
    }

    fun saveNoteDraft(draft: NoteFormDraft) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.saveNoteDraft(draft) }
    }

    suspend fun getNoteDraft(noteId: Long?): NoteFormDraft? =
        draftManager?.getNoteDraft(noteId)

    fun clearNoteDraft(noteId: Long?) {
        val dm = draftManager ?: return
        viewModelScope.launch { dm.clearNoteDraft(noteId) }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    // --- Storage Diagnostics & Data Management ---

    fun refreshStorageDiagnostics(context: Context, runIntegrity: Boolean = false) {
        viewModelScope.launch {
            _storageState.update {
                it.copy(
                    isLoading = !runIntegrity,
                    isIntegrityChecking = runIntegrity
                )
            }
            runCatching {
                repository.getStorageDiagnostics(context, runIntegrity)
            }.onSuccess { diag ->
                _storageState.value = StorageManagementState(
                    isLoading = false,
                    isIntegrityChecking = false,
                    diagnostics = diag
                )
                if (runIntegrity) {
                    _events.tryEmit(
                        UiEvent.ShowSnackbar(
                            message = diag.integrityDetail,
                            isError = diag.integrityOk == false
                        )
                    )
                }
            }.onFailure { err ->
                _storageState.update { it.copy(isLoading = false, isIntegrityChecking = false) }
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Storage check failed", isError = true))
            }
        }
    }

    fun clearCacheOnly(context: Context) {
        viewModelScope.launch {
            runCatching {
                repository.clearCacheOnly(context)
            }.onSuccess { cleared ->
                refreshStorageDiagnostics(context, runIntegrity = false)
                _events.tryEmit(UiEvent.ShowSnackbar("Cleared ${Formatters.formatFileSize(cleared)} of cache"))
            }.onFailure { err ->
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Failed to clear cache", isError = true))
            }
        }
    }

    fun clearAllApplicationData(context: Context, onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.clearAllApplicationData(context, draftManager)
            }.onSuccess {
                reloadAllPagedLists()
                refreshStorageDiagnostics(context, runIntegrity = false)
                _events.tryEmit(UiEvent.ShowSnackbar("All application data cleared cleanly"))
                onCleared()
            }.onFailure { err ->
                _events.tryEmit(UiEvent.ShowSnackbar(err.message ?: "Failed to clear data", isError = true))
            }
        }
    }

    // --- Streaming Backup / Restore / CSV Export ---

    suspend fun writeBackupStreaming(outputStream: OutputStream) {
        backupExportManager.writeBackupJsonStreaming(outputStream, repository)
    }

    suspend fun inspectBackupStreaming(inputStream: InputStream): Result<BackupFileInspection> {
        return backupExportManager.inspectBackupStreaming(inputStream)
    }

    suspend fun restoreBackupStreaming(
        inputStream: InputStream,
        mode: RestoreMode
    ): RestoreSummary {
        val summary = backupExportManager.restoreBackupStreaming(inputStream, repository, mode)
        reloadAllPagedLists()
        return summary
    }

    suspend fun writeCsvStreaming(target: ExportTarget, outputStream: OutputStream) {
        val formatName = uiState.value.settings.dateFormat
        val format = DateFormatOption.fromName(formatName)
        backupExportManager.writeCsvStreaming(target, outputStream, repository, dateProvider.today(), format)
    }

    // --- Legacy snapshot helpers (retained for unit tests) ---

    suspend fun buildBackupJson(): String = withContext(Dispatchers.Default) {
        val snapshot = repository.getAllSnapshotForBackup()
        backupExportManager.serializeBackup(snapshot)
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
                reloadAllPagedLists()
                _events.tryEmit(UiEvent.ShowSnackbar("Backup restored successfully"))
                onDone()
            }.onFailure {
                _events.tryEmit(UiEvent.ShowSnackbar(it.message ?: "Restore failed", isError = true))
            }
        }
    }

    suspend fun buildCsvExport(target: ExportTarget): String = withContext(Dispatchers.Default) {
        val snapshot = repository.getAllSnapshotForBackup()
        val format = DateFormatOption.fromName(snapshot.appSettings.dateFormat)
        backupExportManager.generateCsv(target, snapshot, dateProvider.today(), format)
    }

    fun notifyMessage(message: String, isError: Boolean = false) {
        _events.tryEmit(UiEvent.ShowSnackbar(message, isError))
    }
}
