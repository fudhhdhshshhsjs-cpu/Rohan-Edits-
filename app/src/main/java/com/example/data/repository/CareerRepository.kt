package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.CalendarStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectStatus
import com.example.data.model.VideoCategory
import com.example.util.DEFAULT_JOURNEY_START_DATE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CareerRepository(
    private val db: AppDatabase,
    private val prefs: PreferencesDataStore
) {
    private val editorDao = db.editorProfileDao()
    private val clientDao = db.clientDao()
    private val projectDao = db.projectDao()
    private val calendarDao = db.calendarDayDao()
    private val incomeDao = db.incomeDao()
    private val expenseDao = db.expenseDao()
    private val settingsDao = db.appSettingsDao()

    val editorProfile: Flow<EditorProfileEntity> = editorDao.observeProfile().map {
        it ?: EditorProfileEntity(officialStartDateEpochDay = DEFAULT_JOURNEY_START_DATE.toEpochDay())
    }

    val clients: Flow<List<ClientEntity>> = clientDao.observeAllClients()
    val projects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()
    val calendarDays: Flow<List<CalendarDayEntity>> = calendarDao.observeAllCalendarDays()
    val incomeTransactions: Flow<List<IncomeTransactionEntity>> = incomeDao.observeAllIncome()
    val expenseTransactions: Flow<List<ExpenseTransactionEntity>> = expenseDao.observeAllExpenses()
    val settings: Flow<AppSettingsEntity> = prefs.settingsFlow

    fun observeClient(clientId: Long): Flow<ClientEntity?> = clientDao.observeClientById(clientId)
    fun observeProject(projectId: Long): Flow<ProjectEntity?> = projectDao.observeProjectById(projectId)
    fun observeProjectsForClient(clientId: Long): Flow<List<ProjectEntity>> = projectDao.observeProjectsForClient(clientId)
    fun observeIncomeForClient(clientId: Long): Flow<List<IncomeTransactionEntity>> = incomeDao.observeIncomeForClient(clientId)
    fun observeIncomeForProject(projectId: Long): Flow<List<IncomeTransactionEntity>> = incomeDao.observeIncomeForProject(projectId)

    suspend fun ensureDefaultsExist() {
        if (editorDao.getProfileOnce() == null) {
            editorDao.upsertProfile(EditorProfileEntity())
        }
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsertSettings(AppSettingsEntity())
        }
    }

    suspend fun saveEditorProfile(profile: EditorProfileEntity) {
        require(profile.name.isNotBlank()) { "Editor name is required." }
        require(profile.brandName.isNotBlank()) { "Brand name is required." }
        editorDao.upsertProfile(profile.copy(id = 1, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveClient(client: ClientEntity): Long {
        require(client.name.isNotBlank()) { "Client name is required." }
        require(client.perVideoPrice >= 0.0) { "Per video price cannot be negative." }
        val now = System.currentTimeMillis()
        return if (client.id == 0L) {
            clientDao.insertClient(client.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            clientDao.updateClient(client.copy(updatedAtMillis = now))
            client.id
        }
    }

    /**
     * Safe deletion policy:
     * Income records are ALWAYS preserved with clientNameSnapshot.
     * By default, associated projects are unlinked (clientId = null) so project history is preserved;
     * if deleteAssociatedProjects is true (explicit user decision), only projects are removed while
     * linked income transactions retain snapshots.
     */
    suspend fun deleteClientSafe(clientId: Long, deleteAssociatedProjects: Boolean = false) {
        db.withTransaction {
            val client = clientDao.getClientByIdOnce(clientId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val linkedIncome = incomeDao.getAllIncomeOnce().filter { it.clientId == clientId }
            linkedIncome.forEach { tx ->
                if (tx.clientNameSnapshot.isBlank()) {
                    incomeDao.updateIncome(tx.copy(clientNameSnapshot = client.name, clientId = null))
                } else {
                    incomeDao.updateIncome(tx.copy(clientId = null))
                }
            }
            if (deleteAssociatedProjects) {
                val clientProjects = projectDao.getAllProjectsOnce().filter { it.clientId == clientId }
                clientProjects.forEach { proj ->
                    incomeDao.unlinkProjectFromIncome(proj.id)
                    projectDao.deleteProjectById(proj.id)
                }
            } else {
                projectDao.unlinkProjectsFromClient(clientId, now)
            }
            clientDao.deleteClientById(clientId)
        }
    }

    suspend fun saveProject(project: ProjectEntity): Long {
        require(project.title.isNotBlank()) { "Project title is required." }
        require(project.projectAmount >= 0.0) { "Project amount cannot be negative." }
        val category = VideoCategory.fromName(project.category)
        if (category == VideoCategory.CLIENT_VIDEO) {
            require(project.clientId != null) { "A client must be selected for commissioned projects." }
        }
        if (project.clientRating != null) {
            require(project.clientRating in 1..10) { "Client rating must be between 1 and 10." }
        }

        val now = System.currentTimeMillis()
        return db.withTransaction {
            val paid = if (project.id != 0L) incomeDao.sumPaidForProject(project.id) else 0.0
            val derivedStatus = computePaymentStatus(project.projectAmount, paid)
            val entity = if (project.id == 0L) {
                project.copy(
                    amountPaid = paid,
                    paymentStatus = derivedStatus.name,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            } else {
                project.copy(
                    amountPaid = paid,
                    paymentStatus = derivedStatus.name,
                    updatedAtMillis = now
                )
            }
            if (entity.id == 0L) {
                projectDao.insertProject(entity)
            } else {
                projectDao.updateProject(entity)
                entity.id
            }
        }
    }

    suspend fun deleteProjectSafe(projectId: Long) {
        db.withTransaction {
            val proj = projectDao.getProjectByIdOnce(projectId) ?: return@withTransaction
            val linkedIncome = incomeDao.getAllIncomeOnce().filter { it.projectId == projectId }
            linkedIncome.forEach { tx ->
                val titleSnap = tx.projectTitleSnapshot.ifBlank { proj.title }
                incomeDao.updateIncome(tx.copy(projectTitleSnapshot = titleSnap, projectId = null))
            }
            projectDao.deleteProjectById(projectId)
        }
    }

    suspend fun setCalendarDayStatus(epochDay: Long, status: CalendarStatus, note: String = "") {
        val now = System.currentTimeMillis()
        if (status == CalendarStatus.NOT_MARKED && note.isBlank()) {
            calendarDao.deleteCalendarDay(epochDay)
        } else {
            calendarDao.upsertCalendarDay(
                CalendarDayEntity(
                    dateEpochDay = epochDay,
                    status = status.name,
                    note = note.trim(),
                    updatedAtMillis = now
                )
            )
        }
    }

    suspend fun recordProjectPayment(
        projectId: Long,
        amount: Double,
        dateEpochDay: Long,
        paymentMethod: PaymentMethod,
        notes: String = ""
    ): Long {
        require(amount > 0.0) { "Payment amount must be greater than zero." }
        return db.withTransaction {
            val project = projectDao.getProjectByIdOnce(projectId)
                ?: throw IllegalArgumentException("Project not found.")
            val client = project.clientId?.let { clientDao.getClientByIdOnce(it) }
            val income = IncomeTransactionEntity(
                source = "Payment: ${project.title}",
                clientId = project.clientId,
                projectId = project.id,
                clientNameSnapshot = client?.name.orEmpty(),
                projectTitleSnapshot = project.title,
                amount = amount,
                dateEpochDay = dateEpochDay,
                paymentMethod = paymentMethod.name,
                notes = notes.trim(),
                createdAtMillis = System.currentTimeMillis()
            )
            val insertedId = incomeDao.insertIncome(income)
            recalculateProjectPaymentInternal(project.id)
            insertedId
        }
    }

    suspend fun saveIncome(income: IncomeTransactionEntity): Long {
        require(income.source.isNotBlank()) { "Income source is required." }
        require(income.amount > 0.0) { "Income amount must be greater than zero." }
        return db.withTransaction {
            val oldIncome = if (income.id != 0L) incomeDao.getIncomeByIdOnce(income.id) else null
            val client = income.clientId?.let { clientDao.getClientByIdOnce(it) }
            val project = income.projectId?.let { projectDao.getProjectByIdOnce(it) }

            val resolvedClientId = income.clientId ?: project?.clientId
            val resolvedClientName = client?.name
                ?: resolvedClientId?.let { clientDao.getClientByIdOnce(it)?.name }
                ?: income.clientNameSnapshot

            val sanitized = income.copy(
                clientId = resolvedClientId,
                clientNameSnapshot = resolvedClientName,
                projectTitleSnapshot = project?.title ?: income.projectTitleSnapshot,
                notes = income.notes.trim(),
                createdAtMillis = if (income.id == 0L) System.currentTimeMillis() else income.createdAtMillis
            )

            val savedId = if (sanitized.id == 0L) {
                incomeDao.insertIncome(sanitized)
            } else {
                incomeDao.updateIncome(sanitized)
                sanitized.id
            }

            if (oldIncome?.projectId != null && oldIncome.projectId != sanitized.projectId) {
                recalculateProjectPaymentInternal(oldIncome.projectId)
            }
            if (sanitized.projectId != null) {
                recalculateProjectPaymentInternal(sanitized.projectId)
            }
            savedId
        }
    }

    suspend fun deleteIncome(incomeId: Long) {
        db.withTransaction {
            val existing = incomeDao.getIncomeByIdOnce(incomeId) ?: return@withTransaction
            incomeDao.deleteIncomeById(incomeId)
            if (existing.projectId != null) {
                recalculateProjectPaymentInternal(existing.projectId)
            }
        }
    }

    suspend fun saveExpense(expense: ExpenseTransactionEntity): Long {
        require(expense.description.isNotBlank()) { "Expense description is required." }
        require(expense.amount > 0.0) { "Expense amount must be greater than zero." }
        val sanitized = expense.copy(
            description = expense.description.trim(),
            notes = expense.notes.trim(),
            createdAtMillis = if (expense.id == 0L) System.currentTimeMillis() else expense.createdAtMillis
        )
        return if (sanitized.id == 0L) {
            expenseDao.insertExpense(sanitized)
        } else {
            expenseDao.updateExpense(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseById(expenseId)
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        val clean = settings.copy(id = 1)
        prefs.updateSettings(clean)
        settingsDao.upsertSettings(clean)
    }

    suspend fun getAllSnapshotForBackup(): DatabaseSnapshot = db.withTransaction {
        DatabaseSnapshot(
            editorProfile = editorDao.getProfileOnce() ?: EditorProfileEntity(),
            clients = clientDao.getAllClientsOnce(),
            projects = projectDao.getAllProjectsOnce(),
            calendarDays = calendarDao.getAllCalendarDaysOnce(),
            incomeTransactions = incomeDao.getAllIncomeOnce(),
            expenseTransactions = expenseDao.getAllExpensesOnce(),
            appSettings = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        )
    }

    suspend fun restoreFromSnapshot(snapshot: DatabaseSnapshot) {
        db.withTransaction {
            incomeDao.deleteAllIncome()
            expenseDao.deleteAllExpenses()
            projectDao.deleteAllProjects()
            clientDao.deleteAllClients()
            calendarDao.deleteAllCalendarDays()

            editorDao.upsertProfile(snapshot.editorProfile.copy(id = 1))
            if (snapshot.clients.isNotEmpty()) {
                clientDao.insertAllClients(snapshot.clients)
            }
            if (snapshot.projects.isNotEmpty()) {
                projectDao.insertAllProjects(snapshot.projects)
            }
            if (snapshot.calendarDays.isNotEmpty()) {
                calendarDao.insertAllCalendarDays(snapshot.calendarDays)
            }
            if (snapshot.incomeTransactions.isNotEmpty()) {
                incomeDao.insertAllIncome(snapshot.incomeTransactions)
            }
            if (snapshot.expenseTransactions.isNotEmpty()) {
                expenseDao.insertAllExpenses(snapshot.expenseTransactions)
            }
            // Re-verify all project payment totals from restored income ledger
            snapshot.projects.forEach { proj ->
                recalculateProjectPaymentInternal(proj.id)
            }
            val cleanSettings = snapshot.appSettings.copy(id = 1)
            settingsDao.upsertSettings(cleanSettings)
        }
        prefs.updateSettings(snapshot.appSettings.copy(id = 1))
    }

    private suspend fun recalculateProjectPaymentInternal(projectId: Long) {
        val project = projectDao.getProjectByIdOnce(projectId) ?: return
        val totalPaid = incomeDao.sumPaidForProject(projectId)
        val status = computePaymentStatus(project.projectAmount, totalPaid)
        projectDao.updateProjectPayment(
            projectId = projectId,
            amountPaid = totalPaid,
            paymentStatus = status.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun computePaymentStatus(projectAmount: Double, amountPaid: Double): PaymentStatus {
            return when {
                projectAmount <= 0.0 -> if (amountPaid > 0.0) PaymentStatus.PAID else PaymentStatus.UNPAID
                amountPaid <= 0.0 -> PaymentStatus.UNPAID
                amountPaid + 0.001 >= projectAmount -> PaymentStatus.PAID
                else -> PaymentStatus.PARTIAL
            }
        }

        fun isProjectOverdue(project: ProjectEntity, todayEpochDay: Long): Boolean {
            val status = ProjectStatus.fromName(project.projectStatus)
            if (status == ProjectStatus.COMPLETED || status == ProjectStatus.CANCELLED) return false
            val deadline = project.deadlineEpochDay ?: return false
            return deadline < todayEpochDay
        }
    }
}

data class DatabaseSnapshot(
    val editorProfile: EditorProfileEntity,
    val clients: List<ClientEntity>,
    val projects: List<ProjectEntity>,
    val calendarDays: List<CalendarDayEntity>,
    val incomeTransactions: List<IncomeTransactionEntity>,
    val expenseTransactions: List<ExpenseTransactionEntity>,
    val appSettings: AppSettingsEntity
)
