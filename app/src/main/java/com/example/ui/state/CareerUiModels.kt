package com.example.ui.state

import com.example.data.local.dao.ExpenseSummaryRow
import com.example.data.local.dao.IncomeSummaryRow
import com.example.data.local.dao.NoteSummaryRow
import com.example.data.local.dao.StorageDiagnostics
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectRevisionEntity
import com.example.data.model.ClientSortOption
import com.example.data.model.ClientType
import com.example.data.model.FinancialDateFilter
import com.example.data.model.GoalStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectSortOption
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.VideoCategory
import com.example.util.ExperienceBreakdown
import java.time.LocalDate
import java.time.YearMonth

data class ClientWithStats(
    val client: ClientEntity,
    val totalProjects: Int,
    val completedVideos: Int,
    val totalEarningsReceived: Double,
    val pendingPayments: Double,
    val lastActivityMillis: Long
)

data class ProjectWithClient(
    val project: ProjectEntity,
    val clientName: String?,
    val remainingAmount: Double,
    val isOverdue: Boolean
)

data class MonthActivityStats(
    val yearMonth: YearMonth,
    val doneDays: Int,
    val missedDays: Int,
    val notMarkedDays: Int,
    val consistencyPercent: Double
)

data class YearActivityStats(
    val year: Int,
    val doneDays: Int,
    val missedDays: Int,
    val notMarkedDays: Int,
    val consistencyPercent: Double
)

data class FinancialPeriodPoint(
    val label: String,
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}

data class VideoOutputPeriodPoint(
    val label: String,
    val completedCount: Int
)

data class ClientAnalyticsSummary(
    val total: Int = 0,
    val newCount: Int = 0,
    val oldCount: Int = 0,
    val regularCount: Int = 0,
    val topClients: List<ClientWithStats> = emptyList()
)

data class HomeDashboardState(
    val editorName: String = "Rohan",
    val brandName: String = "Rohan Edits",
    val officialStartDate: LocalDate = LocalDate.of(2026, 7, 6),
    val experience: ExperienceBreakdown = ExperienceBreakdown(0, 0, 0, 0, 0),
    val totalEditingDays: Int = 0,
    val totalMissedDays: Int = 0,
    val consistencyPercent: Double = 0.0,
    val totalClients: Int = 0,
    val totalVideosCompleted: Int = 0,
    val totalProjects: Int = 0,
    val commissionedProjectsCount: Int = 0,
    val totalEarnings: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val pendingClientPayments: Double = 0.0,
    val currentMonthCompletedVideos: Int = 0,
    val currentMonthEarnings: Double = 0.0,
    val currentMonthExpenses: Double = 0.0,
    val currentMonthEditingDays: Int = 0,
    val currentMonthConsistency: Double = 0.0,
    val overdueProjectsCount: Int = 0,
    val activeProjects: List<ProjectWithClient> = emptyList()
)

data class CalendarOverviewState(
    val totalEditingDays: Int = 0,
    val totalMissedDays: Int = 0,
    val totalNotMarkedDays: Int = 0,
    val consistencyPercent: Double = 0.0,
    val daysByEpoch: Map<Long, CalendarDayEntity> = emptyMap(),
    val officialStartDate: LocalDate = LocalDate.of(2026, 7, 6)
)

data class GlobalSearchResults(
    val clients: List<ClientWithStats> = emptyList(),
    val projects: List<ProjectWithClient> = emptyList(),
    val income: List<IncomeTransactionEntity> = emptyList(),
    val expenses: List<ExpenseTransactionEntity> = emptyList()
)

data class ClientsPagedState(
    val items: List<ClientWithStats> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val clientType: ClientType? = null,
    val sortOption: ClientSortOption = ClientSortOption.RECENT_ACTIVITY
)

data class ProjectsPagedState(
    val items: List<ProjectWithClient> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val clientId: Long? = null,
    val category: VideoCategory? = null,
    val projectType: ProjectType? = null,
    val priority: ProjectPriority? = null,
    val paymentStatus: PaymentStatus? = null,
    val projectStatus: ProjectStatus? = null,
    val exactDate: Long? = null,
    val deadlineBefore: Long? = null,
    val onlyOverdue: Boolean = false,
    val sortOption: ProjectSortOption = ProjectSortOption.DATE_DESC
)

data class BudgetPagedState(
    val incomeItems: List<IncomeSummaryRow> = emptyList(),
    val incomeTotalCount: Int = 0,
    val incomeHasMore: Boolean = false,
    val expenseItems: List<ExpenseSummaryRow> = emptyList(),
    val expenseTotalCount: Int = 0,
    val expenseHasMore: Boolean = false,
    val periodIncome: Double = 0.0,
    val periodExpenses: Double = 0.0,
    val yearlyIncome: Double = 0.0,
    val yearlyExpenses: Double = 0.0,
    val isLoading: Boolean = false,
    val dateFilter: FinancialDateFilter = FinancialDateFilter.ALL_TIME,
    val customStartEpoch: Long? = null,
    val customEndEpoch: Long? = null,
    val query: String = ""
) {
    val periodNet: Double get() = periodIncome - periodExpenses
}

data class NotesPagedState(
    val items: List<NoteSummaryRow> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val category: String? = null
)

data class GoalsPagedState(
    val items: List<CareerGoalEntity> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val status: GoalStatus? = null
)

data class GlobalSearchPagedState(
    val query: String = "",
    val isSearching: Boolean = false,
    val clients: List<ClientWithStats> = emptyList(),
    val clientsTotal: Int = 0,
    val projects: List<ProjectWithClient> = emptyList(),
    val projectsTotal: Int = 0,
    val income: List<IncomeSummaryRow> = emptyList(),
    val incomeTotal: Int = 0,
    val expenses: List<ExpenseSummaryRow> = emptyList(),
    val expensesTotal: Int = 0,
    val notes: List<NoteSummaryRow> = emptyList(),
    val notesTotal: Int = 0,
    val goals: List<CareerGoalEntity> = emptyList(),
    val goalsTotal: Int = 0
) {
    val isEmptyResults: Boolean
        get() = clients.isEmpty() && projects.isEmpty() && income.isEmpty() &&
            expenses.isEmpty() && notes.isEmpty() && goals.isEmpty()
}

data class ClientDetailState(
    val isLoading: Boolean = true,
    val client: ClientEntity? = null,
    val totalProjects: Int = 0,
    val completedVideos: Int = 0,
    val totalEarningsReceived: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val projects: List<ProjectWithClient> = emptyList(),
    val payments: List<IncomeTransactionEntity> = emptyList()
)

data class ProjectDetailState(
    val isLoading: Boolean = true,
    val project: ProjectEntity? = null,
    val clientName: String? = null,
    val remainingAmount: Double = 0.0,
    val isOverdue: Boolean = false,
    val payments: List<IncomeTransactionEntity> = emptyList(),
    val revisions: List<ProjectRevisionEntity> = emptyList(),
    val revisionsTotalCount: Int = 0,
    val revisionsHasMore: Boolean = false
)

data class StorageManagementState(
    val isLoading: Boolean = false,
    val isIntegrityChecking: Boolean = false,
    val diagnostics: StorageDiagnostics = StorageDiagnostics()
)

data class MainUiState(
    val today: LocalDate = LocalDate.now(),
    val editorProfile: EditorProfileEntity = EditorProfileEntity(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val home: HomeDashboardState = HomeDashboardState(),
    val clientsWithStats: List<ClientWithStats> = emptyList(),
    val projectsWithClient: List<ProjectWithClient> = emptyList(),
    val calendarOverview: CalendarOverviewState = CalendarOverviewState(),
    val incomeTransactions: List<IncomeTransactionEntity> = emptyList(),
    val expenseTransactions: List<ExpenseTransactionEntity> = emptyList(),
    val clientAnalytics: ClientAnalyticsSummary = ClientAnalyticsSummary(),
    val monthlyFinancialSeries: List<FinancialPeriodPoint> = emptyList(),
    val yearlyFinancialSeries: List<FinancialPeriodPoint> = emptyList(),
    val monthlyVideoSeriesAll: List<VideoOutputPeriodPoint> = emptyList(),
    val monthlyVideoSeriesClient: List<VideoOutputPeriodPoint> = emptyList(),
    val monthlyVideoSeriesPersonal: List<VideoOutputPeriodPoint> = emptyList(),
    val monthlyVideoSeriesPortfolio: List<VideoOutputPeriodPoint> = emptyList(),
    val monthlyVideoSeriesPractice: List<VideoOutputPeriodPoint> = emptyList(),
    val isLoading: Boolean = true
)
