package com.example.ui.state

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
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
    val isLoading: Boolean = true
)
