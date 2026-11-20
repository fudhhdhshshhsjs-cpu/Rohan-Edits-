package com.example.data.backup

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectRevisionEntity
import com.example.data.model.RestoreMode
import com.example.data.repository.DatabaseSnapshot
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayloadDto(
    val schemaVersion: Int = 2,
    val appName: String = "Rohan Edits App",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val editorProfile: EditorProfileDto,
    val clients: List<ClientDto>,
    val projects: List<ProjectDto>,
    val calendarDays: List<CalendarDayDto>,
    val incomeTransactions: List<IncomeTransactionDto>,
    val expenseTransactions: List<ExpenseTransactionDto>,
    val appSettings: AppSettingsDto,
    val notes: List<NoteDto> = emptyList(),
    val projectRevisions: List<ProjectRevisionDto> = emptyList(),
    val careerGoals: List<CareerGoalDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EditorProfileDto(
    val id: Int = 1,
    val name: String,
    val brandName: String,
    val profession: String,
    val officialStartDateEpochDay: Long,
    val professionalExperience: String = "",
    val skills: String = "",
    val editingSoftware: String = "",
    val specializations: String = "",
    val portfolioLink: String = "",
    val personalNotes: String = "",
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ClientDto(
    val id: Long,
    val name: String,
    val platform: String,
    val clientType: String,
    val mobileNumber: String = "",
    val firstProjectDateEpochDay: Long? = null,
    val instructions: String = "",
    val preferredEditingStyle: String = "",
    val perVideoPrice: Double = 0.0,
    val additionalNotes: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ProjectDto(
    val id: Long,
    val clientId: Long? = null,
    val category: String,
    val title: String,
    val projectType: String,
    val priority: String,
    val videoDuration: String = "",
    val projectDateEpochDay: Long,
    val deadlineEpochDay: Long? = null,
    val projectAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentStatus: String,
    val projectStatus: String,
    val clientRating: Int? = null,
    val clientFeedback: String = "",
    val additionalNotes: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CalendarDayDto(
    val dateEpochDay: Long,
    val status: String,
    val note: String = "",
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class IncomeTransactionDto(
    val id: Long,
    val source: String,
    val clientId: Long? = null,
    val projectId: Long? = null,
    val clientNameSnapshot: String = "",
    val projectTitleSnapshot: String = "",
    val amount: Double,
    val dateEpochDay: Long,
    val paymentMethod: String,
    val notes: String = "",
    val createdAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ExpenseTransactionDto(
    val id: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val dateEpochDay: Long,
    val notes: String = "",
    val createdAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class NoteDto(
    val id: Long,
    val title: String,
    val content: String = "",
    val category: String = "GENERAL",
    val clientId: Long? = null,
    val projectId: Long? = null,
    val isPinned: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ProjectRevisionDto(
    val id: Long,
    val projectId: Long,
    val versionNumber: Int = 1,
    val summary: String,
    val instructionsSnapshot: String = "",
    val statusSnapshot: String = "",
    val createdAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CareerGoalDto(
    val id: Long,
    val title: String,
    val description: String = "",
    val targetMetric: String = "CUSTOM",
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val deadlineEpochDay: Long? = null,
    val status: String = "ACTIVE",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class AppSettingsDto(
    val id: Int = 1,
    val themeMode: String,
    val dateFormat: String,
    val currencySymbol: String,
    val confirmBeforeDelete: Boolean,
    val deadlineRemindersEnabled: Boolean = false
)

data class BackupFileInspection(
    val schemaVersion: Int = 1,
    val appName: String = "Rohan Edits App",
    val exportedAtMillis: Long = 0L,
    val clientCount: Int = 0,
    val projectCount: Int = 0,
    val calendarDayCount: Int = 0,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val noteCount: Int = 0,
    val revisionCount: Int = 0,
    val goalCount: Int = 0
)

data class RestoreSummary(
    val mode: RestoreMode,
    val insertedCount: Int,
    val skippedDuplicatesCount: Int,
    val totalProcessed: Int
)

fun DatabaseSnapshot.toDto(): BackupPayloadDto = BackupPayloadDto(
    schemaVersion = 2,
    editorProfile = EditorProfileDto(
        id = editorProfile.id,
        name = editorProfile.name,
        brandName = editorProfile.brandName,
        profession = editorProfile.profession,
        officialStartDateEpochDay = editorProfile.officialStartDateEpochDay,
        professionalExperience = editorProfile.professionalExperience,
        skills = editorProfile.skills,
        editingSoftware = editorProfile.editingSoftware,
        specializations = editorProfile.specializations,
        portfolioLink = editorProfile.portfolioLink,
        personalNotes = editorProfile.personalNotes,
        updatedAtMillis = editorProfile.updatedAtMillis
    ),
    clients = clients.map {
        ClientDto(
            id = it.id,
            name = it.name,
            platform = it.platform,
            clientType = it.clientType,
            mobileNumber = it.mobileNumber,
            firstProjectDateEpochDay = it.firstProjectDateEpochDay,
            instructions = it.instructions,
            preferredEditingStyle = it.preferredEditingStyle,
            perVideoPrice = it.perVideoPrice,
            additionalNotes = it.additionalNotes,
            createdAtMillis = it.createdAtMillis,
            updatedAtMillis = it.updatedAtMillis
        )
    },
    projects = projects.map {
        ProjectDto(
            id = it.id,
            clientId = it.clientId,
            category = it.category,
            title = it.title,
            projectType = it.projectType,
            priority = it.priority,
            videoDuration = it.videoDuration,
            projectDateEpochDay = it.projectDateEpochDay,
            deadlineEpochDay = it.deadlineEpochDay,
            projectAmount = it.projectAmount,
            amountPaid = it.amountPaid,
            paymentStatus = it.paymentStatus,
            projectStatus = it.projectStatus,
            clientRating = it.clientRating,
            clientFeedback = it.clientFeedback,
            additionalNotes = it.additionalNotes,
            createdAtMillis = it.createdAtMillis,
            updatedAtMillis = it.updatedAtMillis
        )
    },
    calendarDays = calendarDays.map {
        CalendarDayDto(
            dateEpochDay = it.dateEpochDay,
            status = it.status,
            note = it.note,
            updatedAtMillis = it.updatedAtMillis
        )
    },
    incomeTransactions = incomeTransactions.map {
        IncomeTransactionDto(
            id = it.id,
            source = it.source,
            clientId = it.clientId,
            projectId = it.projectId,
            clientNameSnapshot = it.clientNameSnapshot,
            projectTitleSnapshot = it.projectTitleSnapshot,
            amount = it.amount,
            dateEpochDay = it.dateEpochDay,
            paymentMethod = it.paymentMethod,
            notes = it.notes,
            createdAtMillis = it.createdAtMillis
        )
    },
    expenseTransactions = expenseTransactions.map {
        ExpenseTransactionDto(
            id = it.id,
            category = it.category,
            description = it.description,
            amount = it.amount,
            dateEpochDay = it.dateEpochDay,
            notes = it.notes,
            createdAtMillis = it.createdAtMillis
        )
    },
    appSettings = AppSettingsDto(
        id = appSettings.id,
        themeMode = appSettings.themeMode,
        dateFormat = appSettings.dateFormat,
        currencySymbol = appSettings.currencySymbol,
        confirmBeforeDelete = appSettings.confirmBeforeDelete,
        deadlineRemindersEnabled = appSettings.deadlineRemindersEnabled
    ),
    notes = notes.map {
        NoteDto(
            id = it.id,
            title = it.title,
            content = it.content,
            category = it.category,
            clientId = it.clientId,
            projectId = it.projectId,
            isPinned = it.isPinned,
            createdAtMillis = it.createdAtMillis,
            updatedAtMillis = it.updatedAtMillis
        )
    },
    projectRevisions = projectRevisions.map {
        ProjectRevisionDto(
            id = it.id,
            projectId = it.projectId,
            versionNumber = it.versionNumber,
            summary = it.summary,
            instructionsSnapshot = it.instructionsSnapshot,
            statusSnapshot = it.statusSnapshot,
            createdAtMillis = it.createdAtMillis
        )
    },
    careerGoals = careerGoals.map {
        CareerGoalDto(
            id = it.id,
            title = it.title,
            description = it.description,
            targetMetric = it.targetMetric,
            targetValue = it.targetValue,
            currentValue = it.currentValue,
            deadlineEpochDay = it.deadlineEpochDay,
            status = it.status,
            createdAtMillis = it.createdAtMillis,
            updatedAtMillis = it.updatedAtMillis
        )
    }
)

fun BackupPayloadDto.toSnapshotValidated(): DatabaseSnapshot {
    require(schemaVersion >= 1) { "Unsupported backup schema version." }
    require(editorProfile.name.isNotBlank()) { "Invalid backup: editor name missing." }
    val validClientIds = clients.map { it.id }.toSet()
    val validProjectIds = projects.map { it.id }.toSet()

    clients.forEach {
        require(it.name.isNotBlank()) { "Invalid client record found in backup." }
        require(it.perVideoPrice >= 0.0) { "Invalid negative price for client ${it.name}." }
    }
    projects.forEach {
        require(it.title.isNotBlank()) { "Invalid project title in backup." }
        require(it.projectAmount >= 0.0) { "Invalid negative project amount in backup." }
    }
    incomeTransactions.forEach {
        require(it.amount > 0.0) { "Invalid income amount in backup." }
    }
    expenseTransactions.forEach {
        require(it.amount > 0.0) { "Invalid expense amount in backup." }
    }
    notes.forEach {
        require(it.title.isNotBlank()) { "Invalid note title in backup." }
    }
    projectRevisions.forEach {
        require(it.summary.isNotBlank()) { "Invalid revision summary in backup." }
        require(it.projectId in validProjectIds) { "Revision references missing project ${it.projectId}." }
    }
    careerGoals.forEach {
        require(it.title.isNotBlank()) { "Invalid goal title in backup." }
        require(it.targetValue > 0.0) { "Invalid goal target value in backup." }
    }

    return DatabaseSnapshot(
        editorProfile = EditorProfileEntity(
            id = 1,
            name = editorProfile.name,
            brandName = editorProfile.brandName.ifBlank { "Rohan Edits" },
            profession = editorProfile.profession,
            officialStartDateEpochDay = editorProfile.officialStartDateEpochDay,
            professionalExperience = editorProfile.professionalExperience,
            skills = editorProfile.skills,
            editingSoftware = editorProfile.editingSoftware,
            specializations = editorProfile.specializations,
            portfolioLink = editorProfile.portfolioLink,
            personalNotes = editorProfile.personalNotes,
            updatedAtMillis = editorProfile.updatedAtMillis
        ),
        clients = clients.map {
            ClientEntity(
                id = it.id,
                name = it.name,
                platform = it.platform,
                clientType = it.clientType,
                mobileNumber = it.mobileNumber,
                firstProjectDateEpochDay = it.firstProjectDateEpochDay,
                instructions = it.instructions,
                preferredEditingStyle = it.preferredEditingStyle,
                perVideoPrice = it.perVideoPrice,
                additionalNotes = it.additionalNotes,
                createdAtMillis = it.createdAtMillis,
                updatedAtMillis = it.updatedAtMillis
            )
        },
        projects = projects.map {
            ProjectEntity(
                id = it.id,
                clientId = it.clientId?.takeIf { cid -> cid in validClientIds },
                category = it.category,
                title = it.title,
                projectType = it.projectType,
                priority = it.priority,
                videoDuration = it.videoDuration,
                projectDateEpochDay = it.projectDateEpochDay,
                deadlineEpochDay = it.deadlineEpochDay,
                projectAmount = it.projectAmount,
                amountPaid = it.amountPaid.coerceAtLeast(0.0),
                paymentStatus = it.paymentStatus,
                projectStatus = it.projectStatus,
                clientRating = it.clientRating,
                clientFeedback = it.clientFeedback,
                additionalNotes = it.additionalNotes,
                createdAtMillis = it.createdAtMillis,
                updatedAtMillis = it.updatedAtMillis
            )
        },
        calendarDays = calendarDays.distinctBy { it.dateEpochDay }.map {
            CalendarDayEntity(
                dateEpochDay = it.dateEpochDay,
                status = it.status,
                note = it.note,
                updatedAtMillis = it.updatedAtMillis
            )
        },
        incomeTransactions = incomeTransactions.map {
            IncomeTransactionEntity(
                id = it.id,
                source = it.source,
                clientId = it.clientId?.takeIf { cid -> cid in validClientIds },
                projectId = it.projectId?.takeIf { pid -> pid in validProjectIds },
                clientNameSnapshot = it.clientNameSnapshot,
                projectTitleSnapshot = it.projectTitleSnapshot,
                amount = it.amount,
                dateEpochDay = it.dateEpochDay,
                paymentMethod = it.paymentMethod,
                notes = it.notes,
                createdAtMillis = it.createdAtMillis
            )
        },
        expenseTransactions = expenseTransactions.map {
            ExpenseTransactionEntity(
                id = it.id,
                category = it.category,
                description = it.description,
                amount = it.amount,
                dateEpochDay = it.dateEpochDay,
                notes = it.notes,
                createdAtMillis = it.createdAtMillis
            )
        },
        appSettings = AppSettingsEntity(
            id = 1,
            themeMode = appSettings.themeMode,
            dateFormat = appSettings.dateFormat,
            currencySymbol = appSettings.currencySymbol.ifBlank { "₹" },
            confirmBeforeDelete = appSettings.confirmBeforeDelete,
            deadlineRemindersEnabled = appSettings.deadlineRemindersEnabled
        ),
        notes = notes.map {
            NoteEntity(
                id = it.id,
                title = it.title,
                content = it.content,
                category = it.category,
                clientId = it.clientId?.takeIf { cid -> cid in validClientIds },
                projectId = it.projectId?.takeIf { pid -> pid in validProjectIds },
                isPinned = it.isPinned,
                createdAtMillis = it.createdAtMillis,
                updatedAtMillis = it.updatedAtMillis
            )
        },
        projectRevisions = projectRevisions.map {
            ProjectRevisionEntity(
                id = it.id,
                projectId = it.projectId,
                versionNumber = it.versionNumber,
                summary = it.summary,
                instructionsSnapshot = it.instructionsSnapshot,
                statusSnapshot = it.statusSnapshot,
                createdAtMillis = it.createdAtMillis
            )
        },
        careerGoals = careerGoals.map {
            CareerGoalEntity(
                id = it.id,
                title = it.title,
                description = it.description,
                targetMetric = it.targetMetric,
                targetValue = it.targetValue,
                currentValue = it.currentValue,
                deadlineEpochDay = it.deadlineEpochDay,
                status = it.status,
                createdAtMillis = it.createdAtMillis,
                updatedAtMillis = it.updatedAtMillis
            )
        }
    )
}
