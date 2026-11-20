package com.example.data.local.dao

data class HomeMetricsAggregate(
    val totalClients: Int = 0,
    val totalProjects: Int = 0,
    val commissionedProjectsCount: Int = 0,
    val totalVideosCompleted: Int = 0,
    val monthCompletedVideos: Int = 0,
    val activeProjectsCount: Int = 0,
    val overdueProjectsCount: Int = 0,
    val pendingClientPayments: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val monthEarnings: Double = 0.0,
    val monthExpenses: Double = 0.0,
    val totalEditingDays: Int = 0,
    val totalMissedDays: Int = 0,
    val markedSinceStart: Int = 0,
    val monthDoneDays: Int = 0,
    val monthMissedDays: Int = 0
)

data class PeriodFinancialAggregate(
    val income: Double = 0.0,
    val expense: Double = 0.0
)

data class ClientTypeBreakdownAggregate(
    val total: Int = 0,
    val newCount: Int = 0,
    val oldCount: Int = 0,
    val regularCount: Int = 0
)

data class ClientSummaryRow(
    val id: Long,
    val name: String,
    val platform: String,
    val clientType: String,
    val perVideoPrice: Double,
    val updatedAtMillis: Long,
    val totalProjects: Int,
    val completedVideos: Int,
    val totalEarningsReceived: Double,
    val pendingPayments: Double,
    val lastActivityMillis: Long
)

data class ProjectSummaryRow(
    val id: Long,
    val clientId: Long?,
    val clientName: String?,
    val category: String,
    val title: String,
    val projectType: String,
    val priority: String,
    val videoDuration: String,
    val projectDateEpochDay: Long,
    val deadlineEpochDay: Long?,
    val projectAmount: Double,
    val amountPaid: Double,
    val paymentStatus: String,
    val projectStatus: String,
    val clientRating: Int?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class IncomeSummaryRow(
    val id: Long,
    val source: String,
    val clientId: Long?,
    val projectId: Long?,
    val clientNameSnapshot: String,
    val projectTitleSnapshot: String,
    val amount: Double,
    val dateEpochDay: Long,
    val paymentMethod: String,
    val notesPreview: String,
    val createdAtMillis: Long
)

data class ExpenseSummaryRow(
    val id: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val dateEpochDay: Long,
    val notesPreview: String,
    val createdAtMillis: Long
)

data class NoteSummaryRow(
    val id: Long,
    val title: String,
    val contentPreview: String,
    val contentLength: Int,
    val category: String,
    val clientId: Long?,
    val clientName: String?,
    val projectId: Long?,
    val projectTitle: String?,
    val isPinned: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class StorageRecordCounts(
    val clients: Long = 0L,
    val projects: Long = 0L,
    val incomeTransactions: Long = 0L,
    val expenseTransactions: Long = 0L,
    val calendarDays: Long = 0L,
    val notes: Long = 0L,
    val revisions: Long = 0L,
    val careerGoals: Long = 0L
) {
    val totalRecords: Long
        get() = clients + projects + incomeTransactions + expenseTransactions +
            calendarDays + notes + revisions + careerGoals
}

data class StorageDiagnostics(
    val databaseSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val availableDeviceBytes: Long = 0L,
    val counts: StorageRecordCounts = StorageRecordCounts(),
    val integrityOk: Boolean? = null,
    val integrityDetail: String = "Not checked yet"
)

data class ClientPickerOption(
    val id: Long,
    val name: String,
    val platform: String,
    val perVideoPrice: Double
)

data class ProjectPickerOption(
    val id: Long,
    val title: String,
    val clientId: Long?,
    val clientName: String?,
    val projectAmount: Double,
    val amountPaid: Double
)

data class ProjectExportRow(
    val id: Long,
    val clientName: String?,
    val category: String,
    val title: String,
    val projectType: String,
    val priority: String,
    val videoDuration: String,
    val projectDateEpochDay: Long,
    val deadlineEpochDay: Long?,
    val projectAmount: Double,
    val amountPaid: Double,
    val paymentStatus: String,
    val projectStatus: String,
    val clientRating: Int?,
    val clientFeedback: String,
    val additionalNotes: String
)

data class IncomeExportRow(
    val id: Long,
    val dateEpochDay: Long,
    val source: String,
    val clientName: String,
    val projectTitle: String,
    val amount: Double,
    val paymentMethod: String,
    val notes: String
)

