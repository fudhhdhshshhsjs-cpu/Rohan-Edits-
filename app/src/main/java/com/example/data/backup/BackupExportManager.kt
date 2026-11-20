package com.example.data.backup

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
import com.example.data.model.VideoCategory
import com.example.data.repository.DatabaseSnapshot
import com.example.util.Formatters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate

enum class ExportTarget(val label: String, val filePrefix: String, val description: String) {
    CLIENT_LIST("Client List", "rohan_edits_clients", "All saved clients, platforms, pricing & notes"),
    PROJECT_LIST("Project List", "rohan_edits_projects", "All video projects, categories, deadlines & status"),
    EDITING_CALENDAR("Editing Calendar", "rohan_edits_calendar", "All marked editing dates & daily notes"),
    INCOME_RECORDS("Income Records", "rohan_edits_income", "Complete income ledger with client & project links"),
    EXPENSE_RECORDS("Expense Records", "rohan_edits_expenses", "Complete expense ledger by category"),
    FINANCIAL_SUMMARY("Financial Summary", "rohan_edits_financial_summary", "Total earnings, expenses, net balance & pending dues"),
    ANALYTICS_SUMMARY("Analytics Summary", "rohan_edits_analytics_summary", "Career consistency, completion rates & breakdowns")
}

class BackupExportManager {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupPayloadDto::class.java).indent("  ")

    fun serializeBackup(snapshot: DatabaseSnapshot): String {
        return adapter.toJson(snapshot.toDto())
    }

    fun parseAndValidateBackup(json: String): Result<BackupPayloadDto> {
        return runCatching {
            require(json.isNotBlank()) { "Backup file is empty." }
            val dto = adapter.fromJson(json) ?: throw IllegalArgumentException("Invalid JSON backup structure.")
            dto.toSnapshotValidated()
            dto
        }
    }

    fun generateCsv(
        target: ExportTarget,
        snapshot: DatabaseSnapshot,
        today: LocalDate,
        dateFormat: DateFormatOption
    ): String {
        val sb = StringBuilder()
        val clientNames = snapshot.clients.associate { it.id to it.name }
        val projectTitles = snapshot.projects.associate { it.id to it.title }

        when (target) {
            ExportTarget.CLIENT_LIST -> {
                sb.appendLine("ID,Client Name,Platform,Client Type,Mobile,First Project Date,Per Video Price (INR),Preferred Style,Instructions,Notes")
                snapshot.clients.forEach { c ->
                    val firstDate = c.firstProjectDateEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: ""
                    sb.appendLine(
                        csvRow(
                            c.id.toString(),
                            c.name,
                            PlatformType.fromName(c.platform).label,
                            ClientType.fromName(c.clientType).label,
                            c.mobileNumber,
                            firstDate,
                            c.perVideoPrice.toString(),
                            c.preferredEditingStyle,
                            c.instructions,
                            c.additionalNotes
                        )
                    )
                }
            }

            ExportTarget.PROJECT_LIST -> {
                sb.appendLine("ID,Title,Category,Client,Project Type,Priority,Duration,Project Date,Deadline,Amount (INR),Amount Paid (INR),Pending (INR),Payment Status,Project Status,Rating,Feedback,Notes")
                snapshot.projects.forEach { p ->
                    val clientName = p.clientId?.let { clientNames[it] }.orEmpty()
                    val pending = if (ProjectStatus.fromName(p.projectStatus) == ProjectStatus.CANCELLED) {
                        0.0
                    } else {
                        (p.projectAmount - p.amountPaid).coerceAtLeast(0.0)
                    }
                    val deadlineStr = p.deadlineEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: ""
                    sb.appendLine(
                        csvRow(
                            p.id.toString(),
                            p.title,
                            VideoCategory.fromName(p.category).label,
                            clientName,
                            ProjectType.fromName(p.projectType).label,
                            ProjectPriority.fromName(p.priority).label,
                            p.videoDuration,
                            Formatters.formatDate(p.projectDateEpochDay, dateFormat),
                            deadlineStr,
                            p.projectAmount.toString(),
                            p.amountPaid.toString(),
                            pending.toString(),
                            PaymentStatus.fromName(p.paymentStatus).label,
                            ProjectStatus.fromName(p.projectStatus).label,
                            p.clientRating?.toString() ?: "",
                            p.clientFeedback,
                            p.additionalNotes
                        )
                    )
                }
            }

            ExportTarget.EDITING_CALENDAR -> {
                sb.appendLine("Date,Status,Note")
                snapshot.calendarDays.sortedBy { it.dateEpochDay }.forEach { d ->
                    sb.appendLine(
                        csvRow(
                            Formatters.formatDate(d.dateEpochDay, dateFormat),
                            CalendarStatus.fromName(d.status).label,
                            d.note
                        )
                    )
                }
            }

            ExportTarget.INCOME_RECORDS -> {
                sb.appendLine("ID,Date,Source,Client,Related Project,Amount (INR),Payment Method,Notes")
                snapshot.incomeTransactions.sortedByDescending { it.dateEpochDay }.forEach { tx ->
                    val cName = tx.clientId?.let { clientNames[it] } ?: tx.clientNameSnapshot
                    val pTitle = tx.projectId?.let { projectTitles[it] } ?: tx.projectTitleSnapshot
                    sb.appendLine(
                        csvRow(
                            tx.id.toString(),
                            Formatters.formatDate(tx.dateEpochDay, dateFormat),
                            tx.source,
                            cName,
                            pTitle,
                            tx.amount.toString(),
                            PaymentMethod.fromName(tx.paymentMethod).label,
                            tx.notes
                        )
                    )
                }
            }

            ExportTarget.EXPENSE_RECORDS -> {
                sb.appendLine("ID,Date,Category,Description,Amount (INR),Notes")
                snapshot.expenseTransactions.sortedByDescending { it.dateEpochDay }.forEach { ex ->
                    sb.appendLine(
                        csvRow(
                            ex.id.toString(),
                            Formatters.formatDate(ex.dateEpochDay, dateFormat),
                            ExpenseCategory.fromName(ex.category).label,
                            ex.description,
                            ex.amount.toString(),
                            ex.notes
                        )
                    )
                }
            }

            ExportTarget.FINANCIAL_SUMMARY -> {
                val totalIncome = snapshot.incomeTransactions.sumOf { it.amount }
                val totalExpenses = snapshot.expenseTransactions.sumOf { it.amount }
                val netBalance = totalIncome - totalExpenses
                val pendingPayments = snapshot.projects
                    .filter { ProjectStatus.fromName(it.projectStatus) != ProjectStatus.CANCELLED }
                    .sumOf { (it.projectAmount - it.amountPaid).coerceAtLeast(0.0) }
                sb.appendLine("Metric,Amount (INR)")
                sb.appendLine(csvRow("Total Recorded Income", totalIncome.toString()))
                sb.appendLine(csvRow("Total Recorded Expenses", totalExpenses.toString()))
                sb.appendLine(csvRow("Net Balance", netBalance.toString()))
                sb.appendLine(csvRow("Pending Client Payments", pendingPayments.toString()))
                sb.appendLine(csvRow("Income Transaction Count", snapshot.incomeTransactions.size.toString()))
                sb.appendLine(csvRow("Expense Transaction Count", snapshot.expenseTransactions.size.toString()))
            }

            ExportTarget.ANALYTICS_SUMMARY -> {
                val todayEpoch = today.toEpochDay()
                val pastCalendar = snapshot.calendarDays.filter { it.dateEpochDay <= todayEpoch }
                val doneDays = pastCalendar.count { it.status == CalendarStatus.DONE.name }
                val missedDays = pastCalendar.count { it.status == CalendarStatus.MISSED.name }
                val consistency = Formatters.calculateConsistencyPercent(doneDays, missedDays)
                val completedVideos = snapshot.projects.count { it.projectStatus == ProjectStatus.COMPLETED.name }
                val startDate = LocalDate.ofEpochDay(snapshot.editorProfile.officialStartDateEpochDay)
                val exp = Formatters.calculateExperience(startDate, today)

                sb.appendLine("Metric,Value")
                sb.appendLine(csvRow("Editor Name", snapshot.editorProfile.name))
                sb.appendLine(csvRow("Brand", snapshot.editorProfile.brandName))
                sb.appendLine(csvRow("Official Start Date", Formatters.formatDate(startDate, dateFormat)))
                sb.appendLine(csvRow("Experience Days", exp.totalDays.toString()))
                sb.appendLine(csvRow("Total Editing Done Days", doneDays.toString()))
                sb.appendLine(csvRow("Total Missed Days", missedDays.toString()))
                sb.appendLine(csvRow("Editing Consistency (%)", Formatters.formatPercent(consistency)))
                sb.appendLine(csvRow("Total Clients", snapshot.clients.size.toString()))
                sb.appendLine(csvRow("Total Projects", snapshot.projects.size.toString()))
                sb.appendLine(csvRow("Total Completed Videos", completedVideos.toString()))
            }
        }
        return sb.toString()
    }

    private fun csvRow(vararg fields: String): String {
        return fields.joinToString(",") { value ->
            val escaped = value.replace("\"", "\"\"")
            "\"$escaped\""
        }
    }
}
