package com.example.data.repository

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.DraftManager
import com.example.data.local.PreferencesDataStore
import com.example.data.local.dao.ClientSummaryRow
import com.example.data.local.dao.ClientTypeBreakdownAggregate
import com.example.data.local.dao.ExpenseSummaryRow
import com.example.data.local.dao.HomeMetricsAggregate
import com.example.data.local.dao.IncomeSummaryRow
import com.example.data.local.dao.NoteSummaryRow
import com.example.data.local.dao.ProjectSummaryRow
import com.example.data.local.dao.StorageDiagnostics
import com.example.data.local.dao.StorageRecordCounts
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
import com.example.data.model.CalendarStatus
import com.example.data.model.ClientSortOption
import com.example.data.model.ClientType
import com.example.data.model.GoalStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectSortOption
import com.example.data.model.ProjectStatus
import com.example.data.model.VideoCategory
import com.example.ui.state.FinancialPeriodPoint
import com.example.ui.state.VideoOutputPeriodPoint
import com.example.util.DEFAULT_JOURNEY_START_DATE
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

data class PagedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val hasMore: Boolean
)

class CareerRepository(
    val db: AppDatabase,
    private val prefs: PreferencesDataStore
) {
    private val editorDao = db.editorProfileDao()
    private val clientDao = db.clientDao()
    private val projectDao = db.projectDao()
    private val calendarDao = db.calendarDayDao()
    private val incomeDao = db.incomeDao()
    private val expenseDao = db.expenseDao()
    private val noteDao = db.noteDao()
    private val revisionDao = db.projectRevisionDao()
    private val goalDao = db.careerGoalDao()
    private val dashboardDao = db.dashboardAggregateDao()
    private val settingsDao = db.appSettingsDao()

    private val _lastStorageError = MutableStateFlow<String?>(null)
    val lastStorageError: StateFlow<String?> = _lastStorageError.asStateFlow()

    fun clearStorageError() {
        _lastStorageError.value = null
    }

    private fun reportDbError(operation: String, error: Throwable) {
        val friendly = when (error) {
            is SQLiteFullException -> "Device storage is full during $operation. Free up space."
            is SQLiteBlobTooBigException -> "Record field exceeded SQLite window limit during $operation."
            is SQLiteDatabaseCorruptException -> "Database integrity issue detected during $operation. Run Integrity Check in Storage Management."
            is SQLiteConstraintException -> "Database relation constraint prevented $operation."
            is SQLiteException -> "Database error during $operation: ${error.localizedMessage ?: "unknown SQLite error"}"
            else -> "Storage error during $operation: ${error.localizedMessage ?: "unknown error"}"
        }
        _lastStorageError.value = friendly
    }

    val editorProfile: Flow<EditorProfileEntity> = editorDao.observeProfile()
        .map { it ?: EditorProfileEntity(officialStartDateEpochDay = DEFAULT_JOURNEY_START_DATE.toEpochDay()) }
        .catch { err ->
            if (err is SQLiteBlobTooBigException) {
                emit(getEditorProfileFullSafe())
            } else {
                reportDbError("reading editor profile", err)
                emit(EditorProfileEntity(officialStartDateEpochDay = DEFAULT_JOURNEY_START_DATE.toEpochDay()))
            }
        }
        .flowOn(Dispatchers.IO)

    // Bounded observation flows for lightweight previews / compatibility (capped in SQL at 100 rows)
    val clients: Flow<List<ClientEntity>> = clientDao.observeAllClients()
        .catch { err ->
            reportDbError("observing clients", err)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    val projects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()
        .catch { err ->
            reportDbError("observing projects", err)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    val calendarDays: Flow<List<CalendarDayEntity>> = calendarDao.observeAllCalendarDays()
        .catch { err ->
            reportDbError("observing calendar", err)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    val incomeTransactions: Flow<List<IncomeTransactionEntity>> = incomeDao.observeAllIncome()
        .catch { err ->
            reportDbError("observing income", err)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    val expenseTransactions: Flow<List<ExpenseTransactionEntity>> = expenseDao.observeAllExpenses()
        .catch { err ->
            reportDbError("observing expenses", err)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    val settings: Flow<AppSettingsEntity> = prefs.settingsFlow
        .catch { err ->
            reportDbError("reading preferences", err)
            emit(AppSettingsEntity())
        }
        .flowOn(Dispatchers.IO)

    fun observeHomeMetrics(today: LocalDate, startDate: LocalDate): Flow<HomeMetricsAggregate> {
        val currentMonth = YearMonth.from(today)
        return dashboardDao.observeHomeMetrics(
            todayEpoch = today.toEpochDay(),
            monthStartEpoch = currentMonth.atDay(1).toEpochDay(),
            monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay(),
            startEpoch = startDate.toEpochDay()
        ).catch { err ->
            reportDbError("computing dashboard metrics", err)
            emit(HomeMetricsAggregate())
        }.flowOn(Dispatchers.IO)
    }

    fun observeRecentActiveProjects(limit: Int = 5): Flow<List<ProjectSummaryRow>> =
        projectDao.observeRecentActiveProjectSummaries(limit)
            .catch { err ->
                reportDbError("loading active projects", err)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeRecentProjects(limit: Int = 50): Flow<List<ProjectSummaryRow>> =
        projectDao.observeRecentProjectSummaries(limit)
            .catch { err ->
                reportDbError("loading recent projects", err)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeClientTypeBreakdown(): Flow<ClientTypeBreakdownAggregate> =
        clientDao.observeClientTypeBreakdown()
            .catch { err ->
                reportDbError("computing client analytics", err)
                emit(ClientTypeBreakdownAggregate())
            }
            .flowOn(Dispatchers.IO)

    fun observeTopActiveClients(limit: Int = 5): Flow<List<ClientSummaryRow>> =
        clientDao.observeTopActiveClients(limit)
            .catch { err ->
                reportDbError("loading top clients", err)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeCalendarDaysInRange(startEpoch: Long, endEpoch: Long): Flow<List<CalendarDayEntity>> =
        calendarDao.observeCalendarDaysInRange(startEpoch, endEpoch)
            .catch { err ->
                reportDbError("loading calendar month", err)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeClient(clientId: Long): Flow<ClientEntity?> =
        clientDao.observeClientById(clientId).flowOn(Dispatchers.IO)

    fun observeClientSummary(clientId: Long): Flow<ClientSummaryRow?> =
        clientDao.observeClientSummary(clientId).flowOn(Dispatchers.IO)

    fun observeProject(projectId: Long): Flow<ProjectEntity?> =
        projectDao.observeProjectById(projectId).flowOn(Dispatchers.IO)

    fun observeProjectsForClient(clientId: Long, limit: Int = 50): Flow<List<ProjectEntity>> =
        projectDao.observeProjectsForClient(clientId, limit).flowOn(Dispatchers.IO)

    fun observeIncomeForClient(clientId: Long, limit: Int = 50): Flow<List<IncomeTransactionEntity>> =
        incomeDao.observeIncomeForClient(clientId, limit).flowOn(Dispatchers.IO)

    fun observeIncomeForProject(projectId: Long, limit: Int = 50): Flow<List<IncomeTransactionEntity>> =
        incomeDao.observeIncomeForProject(projectId, limit).flowOn(Dispatchers.IO)

    fun observeRevisionsForProject(projectId: Long, limit: Int = 50): Flow<List<ProjectRevisionEntity>> =
        revisionDao.observeRevisionsForProject(projectId, limit).flowOn(Dispatchers.IO)

    private suspend inline fun <T> safeDbWrite(
        operation: String = "database write",
        crossinline block: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        try {
            block().also { _lastStorageError.value = null }
        } catch (e: SQLiteFullException) {
            val msg = "Device storage is full. Free up space and try again."
            _lastStorageError.value = msg
            throw IllegalStateException(msg, e)
        } catch (e: SQLiteBlobTooBigException) {
            val msg = "Entry exceeds single SQLite record buffer ($operation)."
            _lastStorageError.value = msg
            throw IllegalStateException(msg, e)
        } catch (e: SQLiteDatabaseCorruptException) {
            val msg = "Database integrity warning during $operation. Run Integrity Check."
            _lastStorageError.value = msg
            throw IllegalStateException(msg, e)
        } catch (e: SQLiteException) {
            val msg = "Database storage error: ${e.localizedMessage ?: "unable to save"}"
            _lastStorageError.value = msg
            throw IllegalStateException(msg, e)
        }
    }

    // --- Safe Huge-Text Chunked Readers (prevents CursorWindow SQLiteBlobTooBigException) ---

    private suspend fun readTextChunked(
        totalLength: Int,
        fetchChunk: suspend (start1Based: Int, len: Int) -> String?
    ): String {
        if (totalLength <= 0) return ""
        val chunkSize = 250_000
        if (totalLength <= chunkSize) {
            return fetchChunk(1, totalLength).orEmpty()
        }
        val sb = StringBuilder(totalLength)
        var pos = 1
        while (pos <= totalLength) {
            val len = minOf(chunkSize, totalLength - pos + 1)
            sb.append(fetchChunk(pos, len).orEmpty())
            pos += len
        }
        return sb.toString()
    }

    suspend fun getEditorProfileFullSafe(): EditorProfileEntity = withContext(Dispatchers.IO) {
        try {
            editorDao.getProfileOnce() ?: EditorProfileEntity()
        } catch (_: SQLiteBlobTooBigException) {
            val header = editorDao.getProfileHeaderOnce() ?: EditorProfileEntity()
            val notesLen = editorDao.getPersonalNotesLength() ?: 0
            val expLen = editorDao.getExperienceLength() ?: 0
            val notes = readTextChunked(notesLen) { s, l -> editorDao.getPersonalNotesChunk(s, l) }
            val exp = readTextChunked(expLen) { s, l -> editorDao.getExperienceChunk(s, l) }
            header.copy(professionalExperience = exp, personalNotes = notes)
        }
    }

    suspend fun getClientFullSafe(clientId: Long): ClientEntity? = withContext(Dispatchers.IO) {
        try {
            clientDao.getClientByIdOnce(clientId)
        } catch (_: SQLiteBlobTooBigException) {
            val header = clientDao.getClientHeaderOnce(clientId) ?: return@withContext null
            val instLen = clientDao.getInstructionsLength(clientId) ?: 0
            val notesLen = clientDao.getNotesLength(clientId) ?: 0
            val instructions = readTextChunked(instLen) { s, l -> clientDao.getInstructionsChunk(clientId, s, l) }
            val notes = readTextChunked(notesLen) { s, l -> clientDao.getNotesChunk(clientId, s, l) }
            header.copy(instructions = instructions, additionalNotes = notes)
        }
    }

    suspend fun getProjectFullSafe(projectId: Long): ProjectEntity? = withContext(Dispatchers.IO) {
        try {
            projectDao.getProjectByIdOnce(projectId)
        } catch (_: SQLiteBlobTooBigException) {
            val header = projectDao.getProjectHeaderOnce(projectId) ?: return@withContext null
            val fbLen = projectDao.getFeedbackLength(projectId) ?: 0
            val notesLen = projectDao.getNotesLength(projectId) ?: 0
            val feedback = readTextChunked(fbLen) { s, l -> projectDao.getFeedbackChunk(projectId, s, l) }
            val notes = readTextChunked(notesLen) { s, l -> projectDao.getNotesChunk(projectId, s, l) }
            header.copy(clientFeedback = feedback, additionalNotes = notes)
        }
    }

    suspend fun getNoteFullSafe(noteId: Long): NoteEntity? = withContext(Dispatchers.IO) {
        try {
            noteDao.getNoteByIdOnce(noteId)
        } catch (_: SQLiteBlobTooBigException) {
            val header = noteDao.getNoteHeaderOnce(noteId) ?: return@withContext null
            val contentLen = noteDao.getNoteContentLength(noteId) ?: 0
            val content = readTextChunked(contentLen) { s, l -> noteDao.getNoteContentChunk(noteId, s, l) }
            header.copy(content = content)
        }
    }

    // --- Paginated Database Search & Filtering ---

    suspend fun getClientsPage(
        query: String,
        clientType: ClientType?,
        sortOption: ClientSortOption,
        limit: Int,
        offset: Int = 0
    ): PagedResult<ClientSummaryRow> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val typeName = clientType?.name
        val total = clientDao.countFilteredClients(cleanQuery, typeName)
        val rows = when (sortOption) {
            ClientSortOption.NAME -> clientDao.queryClientsByName(cleanQuery, typeName, limit, offset)
            ClientSortOption.RECENT_ACTIVITY -> clientDao.queryClientsRecent(cleanQuery, typeName, limit, offset)
        }
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getProjectsPage(
        query: String,
        clientId: Long? = null,
        category: VideoCategory? = null,
        projectType: com.example.data.model.ProjectType? = null,
        priority: ProjectPriority? = null,
        paymentStatus: PaymentStatus? = null,
        projectStatus: ProjectStatus? = null,
        exactDate: Long? = null,
        deadlineBefore: Long? = null,
        onlyOverdue: Boolean = false,
        todayEpoch: Long,
        sortOption: ProjectSortOption = ProjectSortOption.DATE_DESC,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<ProjectSummaryRow> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val total = projectDao.countFilteredProjects(
            query = cleanQuery,
            clientId = clientId,
            category = category?.name,
            projectType = projectType?.name,
            priority = priority?.name,
            paymentStatus = paymentStatus?.name,
            projectStatus = projectStatus?.name,
            exactDate = exactDate,
            deadlineBefore = deadlineBefore,
            onlyOverdue = onlyOverdue,
            todayEpoch = todayEpoch
        )
        val rows = when (sortOption) {
            ProjectSortOption.DATE_DESC -> projectDao.queryProjectsDateDesc(
                cleanQuery, clientId, category?.name, projectType?.name, priority?.name,
                paymentStatus?.name, projectStatus?.name, exactDate, deadlineBefore,
                onlyOverdue, todayEpoch, limit, offset
            )
            ProjectSortOption.DEADLINE_ASC -> projectDao.queryProjectsDeadlineAsc(
                cleanQuery, clientId, category?.name, projectType?.name, priority?.name,
                paymentStatus?.name, projectStatus?.name, exactDate, deadlineBefore,
                onlyOverdue, todayEpoch, limit, offset
            )
            ProjectSortOption.AMOUNT_DESC -> projectDao.queryProjectsAmountDesc(
                cleanQuery, clientId, category?.name, projectType?.name, priority?.name,
                paymentStatus?.name, projectStatus?.name, exactDate, deadlineBefore,
                onlyOverdue, todayEpoch, limit, offset
            )
            ProjectSortOption.PRIORITY_DESC -> projectDao.queryProjectsPriorityDesc(
                cleanQuery, clientId, category?.name, projectType?.name, priority?.name,
                paymentStatus?.name, projectStatus?.name, exactDate, deadlineBefore,
                onlyOverdue, todayEpoch, limit, offset
            )
        }
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getIncomePage(
        query: String = "",
        startEpoch: Long? = null,
        endEpoch: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<IncomeSummaryRow> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val total = incomeDao.countFilteredIncome(cleanQuery, startEpoch, endEpoch)
        val rows = incomeDao.queryIncomePage(cleanQuery, startEpoch, endEpoch, limit, offset)
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getExpensesPage(
        query: String = "",
        startEpoch: Long? = null,
        endEpoch: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<ExpenseSummaryRow> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val total = expenseDao.countFilteredExpenses(cleanQuery, startEpoch, endEpoch)
        val rows = expenseDao.queryExpensesPage(cleanQuery, startEpoch, endEpoch, limit, offset)
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getFinancialPeriodTotals(startEpoch: Long?, endEpoch: Long?): Pair<Double, Double> =
        withContext(Dispatchers.IO) {
            val inc = incomeDao.sumIncomeInRange(startEpoch, endEpoch)
            val exp = expenseDao.sumExpensesInRange(startEpoch, endEpoch)
            inc to exp
        }

    suspend fun getNotesPage(
        query: String = "",
        category: String? = null,
        clientId: Long? = null,
        projectId: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<NoteSummaryRow> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val total = noteDao.countFilteredNotes(cleanQuery, category, clientId, projectId)
        val rows = noteDao.queryNotesPage(cleanQuery, category, clientId, projectId, limit, offset)
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getGoalsPage(
        query: String = "",
        status: GoalStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<CareerGoalEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val total = goalDao.countFilteredGoals(cleanQuery, status?.name)
        val rows = goalDao.queryGoalsPage(cleanQuery, status?.name, limit, offset)
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getRevisionsPage(
        projectId: Long,
        limit: Int = 50,
        offset: Int = 0
    ): PagedResult<ProjectRevisionEntity> = withContext(Dispatchers.IO) {
        val total = revisionDao.countRevisionsForProject(projectId)
        val rows = revisionDao.getRevisionsForProjectPage(projectId, limit, offset)
        PagedResult(items = rows, totalCount = total, hasMore = offset + rows.size < total)
    }

    suspend fun getDueOrOverdueProjectsForReminder(today: LocalDate): List<ProjectEntity> =
        withContext(Dispatchers.IO) {
            projectDao.getDueOrOverdueProjectsOnce(today.plusDays(2).toEpochDay(), 25)
        }

    suspend fun searchClientsForPicker(
        query: String,
        limit: Int = 50
    ): List<com.example.data.local.dao.ClientPickerOption> = withContext(Dispatchers.IO) {
        clientDao.searchClientPickerOptions(query.trim(), limit)
    }

    suspend fun searchProjectsForPicker(
        query: String,
        clientId: Long? = null,
        limit: Int = 50
    ): List<com.example.data.local.dao.ProjectPickerOption> = withContext(Dispatchers.IO) {
        projectDao.searchProjectPickerOptions(query.trim(), clientId, limit)
    }

    suspend fun getCalendarRangeCounts(
        startEpoch: Long,
        endEpoch: Long
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val done = calendarDao.countStatusInRange(startEpoch, endEpoch, CalendarStatus.DONE.name)
        val missed = calendarDao.countStatusInRange(startEpoch, endEpoch, CalendarStatus.MISSED.name)
        done to missed
    }

    // --- SQL-Backed Analytics Series (O(log N) per period, zero full-table loads) ---

    suspend fun getMonthlyFinancialSeries(today: LocalDate): List<FinancialPeriodPoint> =
        withContext(Dispatchers.IO) {
            val currentMonth = YearMonth.from(today)
            (5 downTo 0).map { offset ->
                val ym = currentMonth.minusMonths(offset.toLong())
                val start = ym.atDay(1).toEpochDay()
                val end = ym.atEndOfMonth().toEpochDay()
                val inc = incomeDao.sumIncomeInRange(start, end)
                val exp = expenseDao.sumExpensesInRange(start, end)
                FinancialPeriodPoint(
                    label = Formatters.formatShortMonth(ym.year, ym.monthValue),
                    income = inc,
                    expense = exp
                )
            }
        }

    suspend fun getYearlyFinancialSeries(today: LocalDate, startDate: LocalDate): List<FinancialPeriodPoint> =
        withContext(Dispatchers.IO) {
            val minYear = listOfNotNull(
                incomeDao.getMinIncomeEpochDay()?.let { LocalDate.ofEpochDay(it).year },
                expenseDao.getMinExpenseEpochDay()?.let { LocalDate.ofEpochDay(it).year },
                startDate.year,
                today.year
            ).min()
            val maxYear = listOfNotNull(
                incomeDao.getMaxIncomeEpochDay()?.let { LocalDate.ofEpochDay(it).year },
                expenseDao.getMaxExpenseEpochDay()?.let { LocalDate.ofEpochDay(it).year },
                today.year
            ).max()

            val yearsToShow = ((minYear..maxYear).toList()).takeLast(6)
            yearsToShow.map { year ->
                val start = LocalDate.of(year, 1, 1).toEpochDay()
                val end = LocalDate.of(year, 12, 31).toEpochDay()
                val inc = incomeDao.sumIncomeInRange(start, end)
                val exp = expenseDao.sumExpensesInRange(start, end)
                FinancialPeriodPoint(label = year.toString(), income = inc, expense = exp)
            }
        }

    suspend fun getMonthlyVideoOutputSeries(today: LocalDate, categoryFilter: VideoCategory?): List<VideoOutputPeriodPoint> =
        withContext(Dispatchers.IO) {
            val currentMonth = YearMonth.from(today)
            (5 downTo 0).map { offset ->
                val ym = currentMonth.minusMonths(offset.toLong())
                val start = ym.atDay(1).toEpochDay()
                val end = ym.atEndOfMonth().toEpochDay()
                val count = projectDao.countCompletedProjectsInRange(categoryFilter?.name, start, end)
                VideoOutputPeriodPoint(
                    label = Formatters.formatShortMonth(ym.year, ym.monthValue),
                    completedCount = count
                )
            }
        }

    // --- Mutations ---

    suspend fun ensureDefaultsExist() = safeDbWrite("initializing defaults") {
        if (editorDao.getProfileOnce() == null) {
            editorDao.upsertProfile(EditorProfileEntity())
        }
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsertSettings(AppSettingsEntity())
        }
    }

    suspend fun saveEditorProfile(profile: EditorProfileEntity) = safeDbWrite("saving editor profile") {
        require(profile.name.isNotBlank()) { "Editor name is required." }
        require(profile.brandName.isNotBlank()) { "Brand name is required." }
        editorDao.upsertProfile(profile.copy(id = 1, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveClient(client: ClientEntity): Long = safeDbWrite("saving client") {
        require(client.name.isNotBlank()) { "Client name is required." }
        require(client.perVideoPrice >= 0.0) { "Per video price cannot be negative." }
        val now = System.currentTimeMillis()
        if (client.id == 0L) {
            clientDao.insertClient(client.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            clientDao.updateClient(client.copy(updatedAtMillis = now))
            client.id
        }
    }

    suspend fun deleteClientSafe(clientId: Long, deleteAssociatedProjects: Boolean = false) = safeDbWrite("deleting client") {
        db.withTransaction {
            val client = clientDao.getClientHeaderOnce(clientId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            incomeDao.unlinkClientFromIncomePreservingSnapshot(clientId, client.name)
            if (deleteAssociatedProjects) {
                incomeDao.unlinkProjectsForClientFromIncome(clientId)
                projectDao.deleteProjectsByClientId(clientId)
            } else {
                projectDao.unlinkProjectsFromClient(clientId, now)
            }
            clientDao.deleteClientById(clientId)
        }
    }

    suspend fun saveProject(project: ProjectEntity): Long = safeDbWrite("saving project") {
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
        db.withTransaction {
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
            val savedId = if (entity.id == 0L) {
                projectDao.insertProject(entity)
            } else {
                projectDao.updateProject(entity)
                entity.id
            }
            entity.clientId?.let { clientDao.touchClientActivity(it, now) }
            savedId
        }
    }

    suspend fun deleteProjectSafe(projectId: Long) = safeDbWrite("deleting project") {
        db.withTransaction {
            val proj = projectDao.getProjectHeaderOnce(projectId) ?: return@withTransaction
            incomeDao.unlinkProjectFromIncomePreservingSnapshot(projectId, proj.title)
            projectDao.deleteProjectById(projectId)
        }
    }

    suspend fun setCalendarDayStatus(epochDay: Long, status: CalendarStatus, note: String = "") = safeDbWrite("updating calendar") {
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
    ): Long = safeDbWrite("recording project payment") {
        require(amount > 0.0) { "Payment amount must be greater than zero." }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val project = projectDao.getProjectHeaderOnce(projectId)
                ?: throw IllegalArgumentException("Project not found.")
            val client = project.clientId?.let { clientDao.getClientHeaderOnce(it) }
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
                createdAtMillis = now
            )
            val insertedId = incomeDao.insertIncome(income)
            recalculateProjectPaymentInternal(project.id)
            project.clientId?.let { clientDao.touchClientActivity(it, now) }
            insertedId
        }
    }

    suspend fun saveIncome(income: IncomeTransactionEntity): Long = safeDbWrite("saving income") {
        require(income.source.isNotBlank()) { "Income source is required." }
        require(income.amount > 0.0) { "Income amount must be greater than zero." }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val oldIncome = if (income.id != 0L) incomeDao.getIncomeByIdOnce(income.id) else null
            val client = income.clientId?.let { clientDao.getClientHeaderOnce(it) }
            val project = income.projectId?.let { projectDao.getProjectHeaderOnce(it) }

            val resolvedClientId = income.clientId ?: project?.clientId
            val resolvedClientName = client?.name
                ?: resolvedClientId?.let { clientDao.getClientHeaderOnce(it)?.name }
                ?: income.clientNameSnapshot

            val sanitized = income.copy(
                clientId = resolvedClientId,
                clientNameSnapshot = resolvedClientName,
                projectTitleSnapshot = project?.title ?: income.projectTitleSnapshot,
                notes = income.notes.trim(),
                createdAtMillis = if (income.id == 0L) now else income.createdAtMillis
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
            resolvedClientId?.let { clientDao.touchClientActivity(it, now) }
            savedId
        }
    }

    suspend fun deleteIncome(incomeId: Long) = safeDbWrite("deleting income") {
        db.withTransaction {
            val existing = incomeDao.getIncomeByIdOnce(incomeId) ?: return@withTransaction
            incomeDao.deleteIncomeById(incomeId)
            if (existing.projectId != null) {
                recalculateProjectPaymentInternal(existing.projectId)
            }
        }
    }

    suspend fun saveExpense(expense: ExpenseTransactionEntity): Long = safeDbWrite("saving expense") {
        require(expense.description.isNotBlank()) { "Expense description is required." }
        require(expense.amount > 0.0) { "Expense amount must be greater than zero." }
        val sanitized = expense.copy(
            description = expense.description.trim(),
            notes = expense.notes.trim(),
            createdAtMillis = if (expense.id == 0L) System.currentTimeMillis() else expense.createdAtMillis
        )
        if (sanitized.id == 0L) {
            expenseDao.insertExpense(sanitized)
        } else {
            expenseDao.updateExpense(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteExpense(expenseId: Long) = safeDbWrite("deleting expense") {
        expenseDao.deleteExpenseById(expenseId)
    }

    suspend fun saveNote(note: NoteEntity): Long = safeDbWrite("saving note") {
        require(note.title.isNotBlank()) { "Note title is required." }
        val now = System.currentTimeMillis()
        val sanitized = note.copy(
            title = note.title.trim(),
            createdAtMillis = if (note.id == 0L) now else note.createdAtMillis,
            updatedAtMillis = now
        )
        if (sanitized.id == 0L) {
            noteDao.insertNote(sanitized)
        } else {
            noteDao.updateNote(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteNote(noteId: Long) = safeDbWrite("deleting note") {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun addProjectRevision(
        projectId: Long,
        summary: String,
        instructionsSnapshot: String = "",
        statusSnapshot: String = ""
    ): Long = safeDbWrite("saving project revision") {
        require(summary.isNotBlank()) { "Revision summary is required." }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val nextVersion = revisionDao.getMaxVersionNumber(projectId) + 1
            val id = revisionDao.insertRevision(
                ProjectRevisionEntity(
                    projectId = projectId,
                    versionNumber = nextVersion,
                    summary = summary.trim(),
                    instructionsSnapshot = instructionsSnapshot.trim(),
                    statusSnapshot = statusSnapshot,
                    createdAtMillis = now
                )
            )
            id
        }
    }

    suspend fun deleteProjectRevision(revisionId: Long) = safeDbWrite("deleting revision") {
        revisionDao.deleteRevisionById(revisionId)
    }

    suspend fun saveGoal(goal: CareerGoalEntity): Long = safeDbWrite("saving career goal") {
        require(goal.title.isNotBlank()) { "Goal title is required." }
        require(goal.targetValue > 0.0) { "Target value must be greater than zero." }
        require(goal.currentValue >= 0.0) { "Current progress cannot be negative." }
        val now = System.currentTimeMillis()
        val sanitized = goal.copy(
            title = goal.title.trim(),
            description = goal.description.trim(),
            createdAtMillis = if (goal.id == 0L) now else goal.createdAtMillis,
            updatedAtMillis = now
        )
        if (sanitized.id == 0L) {
            goalDao.insertGoal(sanitized)
        } else {
            goalDao.updateGoal(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteGoal(goalId: Long) = safeDbWrite("deleting career goal") {
        goalDao.deleteGoalById(goalId)
    }

    suspend fun updateSettings(settings: AppSettingsEntity) = safeDbWrite("updating settings") {
        val clean = settings.copy(id = 1)
        prefs.updateSettings(clean)
        settingsDao.upsertSettings(clean)
    }

    // --- Storage Diagnostics & Clear Data ---

    suspend fun getStorageDiagnostics(
        context: Context,
        runIntegrity: Boolean = false
    ): StorageDiagnostics = withContext(Dispatchers.IO) {
        val counts = runCatching { dashboardDao.getStorageRecordCounts() }.getOrDefault(StorageRecordCounts())
        val dbBytes = AppDatabase.getDatabaseTotalSizeBytes(context)
        val cacheBytes = AppDatabase.getDirectorySizeBytes(context.cacheDir)
        val freeBytes = AppDatabase.getAvailableStorageBytes(context)
        val (integrityOk, detail) = if (runIntegrity) {
            db.runIntegrityCheck()
        } else {
            null to "Tap Verify Integrity to run SQLite quick_check"
        }
        StorageDiagnostics(
            databaseSizeBytes = dbBytes,
            cacheSizeBytes = cacheBytes,
            availableDeviceBytes = freeBytes,
            counts = counts,
            integrityOk = integrityOk,
            integrityDetail = detail
        )
    }

    suspend fun clearCacheOnly(context: Context): Long = withContext(Dispatchers.IO) {
        val before = AppDatabase.getDirectorySizeBytes(context.cacheDir)
        runCatching {
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        }
        before
    }

    suspend fun clearAllApplicationData(context: Context, draftManager: DraftManager? = null) =
        safeDbWrite("clearing application data") {
            db.withTransaction {
                revisionDao.deleteAllRevisions()
                noteDao.deleteAllNotes()
                goalDao.deleteAllGoals()
                incomeDao.deleteAllIncome()
                expenseDao.deleteAllExpenses()
                projectDao.deleteAllProjects()
                clientDao.deleteAllClients()
                calendarDao.deleteAllCalendarDays()
                editorDao.upsertProfile(EditorProfileEntity())
                settingsDao.upsertSettings(AppSettingsEntity())
            }
            prefs.updateSettings(AppSettingsEntity())
            draftManager?.clearAllDrafts()
            runCatching { context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() } }
            db.walCheckpoint()
        }

    // --- Streaming Chunk Readers & Legacy Snapshot Helpers ---

    suspend fun getAllSnapshotForBackup(): DatabaseSnapshot = safeDbWrite("creating snapshot") {
        db.withTransaction {
            val clients = mutableListOf<ClientEntity>()
            var lastClientId = 0L
            while (true) {
                val chunk = clientDao.getClientsChunkAfterId(lastClientId, 500)
                if (chunk.isEmpty()) break
                clients.addAll(chunk)
                lastClientId = chunk.last().id
            }

            val projects = mutableListOf<ProjectEntity>()
            var lastProjectId = 0L
            while (true) {
                val chunk = projectDao.getProjectsChunkAfterId(lastProjectId, 500)
                if (chunk.isEmpty()) break
                projects.addAll(chunk)
                lastProjectId = chunk.last().id
            }

            val calendarDays = mutableListOf<CalendarDayEntity>()
            var lastEpoch = Long.MIN_VALUE
            while (true) {
                val chunk = calendarDao.getCalendarDaysChunkAfterEpoch(lastEpoch, 500)
                if (chunk.isEmpty()) break
                calendarDays.addAll(chunk)
                lastEpoch = chunk.last().dateEpochDay
            }

            val income = mutableListOf<IncomeTransactionEntity>()
            var lastIncId = 0L
            while (true) {
                val chunk = incomeDao.getIncomeChunkAfterId(lastIncId, 500)
                if (chunk.isEmpty()) break
                income.addAll(chunk)
                lastIncId = chunk.last().id
            }

            val expenses = mutableListOf<ExpenseTransactionEntity>()
            var lastExpId = 0L
            while (true) {
                val chunk = expenseDao.getExpensesChunkAfterId(lastExpId, 500)
                if (chunk.isEmpty()) break
                expenses.addAll(chunk)
                lastExpId = chunk.last().id
            }

            val notes = mutableListOf<NoteEntity>()
            var lastNoteId = 0L
            while (true) {
                val chunk = noteDao.getNotesChunkAfterId(lastNoteId, 500)
                if (chunk.isEmpty()) break
                notes.addAll(chunk)
                lastNoteId = chunk.last().id
            }

            val revisions = mutableListOf<ProjectRevisionEntity>()
            var lastRevId = 0L
            while (true) {
                val chunk = revisionDao.getRevisionsChunkAfterId(lastRevId, 500)
                if (chunk.isEmpty()) break
                revisions.addAll(chunk)
                lastRevId = chunk.last().id
            }

            val goals = mutableListOf<CareerGoalEntity>()
            var lastGoalId = 0L
            while (true) {
                val chunk = goalDao.getGoalsChunkAfterId(lastGoalId, 500)
                if (chunk.isEmpty()) break
                goals.addAll(chunk)
                lastGoalId = chunk.last().id
            }

            DatabaseSnapshot(
                editorProfile = getEditorProfileFullSafe(),
                clients = clients,
                projects = projects,
                calendarDays = calendarDays,
                incomeTransactions = income,
                expenseTransactions = expenses,
                appSettings = settingsDao.getSettingsOnce() ?: AppSettingsEntity(),
                notes = notes,
                projectRevisions = revisions,
                careerGoals = goals
            )
        }
    }

    suspend fun restoreFromSnapshot(snapshot: DatabaseSnapshot) = safeDbWrite("restoring snapshot") {
        val now = System.currentTimeMillis()
        db.withTransaction {
            revisionDao.deleteAllRevisions()
            noteDao.deleteAllNotes()
            goalDao.deleteAllGoals()
            incomeDao.deleteAllIncome()
            expenseDao.deleteAllExpenses()
            projectDao.deleteAllProjects()
            clientDao.deleteAllClients()
            calendarDao.deleteAllCalendarDays()

            editorDao.upsertProfile(snapshot.editorProfile.copy(id = 1))
            snapshot.clients.chunked(500).forEach { clientDao.insertAllClients(it) }
            snapshot.projects.chunked(500).forEach { projectDao.insertAllProjects(it) }
            snapshot.calendarDays.chunked(500).forEach { calendarDao.insertAllCalendarDays(it) }
            snapshot.incomeTransactions.chunked(500).forEach { incomeDao.insertAllIncome(it) }
            snapshot.expenseTransactions.chunked(500).forEach { expenseDao.insertAllExpenses(it) }
            snapshot.notes.chunked(500).forEach { noteDao.insertAllNotes(it) }
            snapshot.projectRevisions.chunked(500).forEach { revisionDao.insertAllRevisions(it) }
            snapshot.careerGoals.chunked(500).forEach { goalDao.insertAllGoals(it) }

            projectDao.recalculateAllProjectPaymentsBulk(now)
            val cleanSettings = snapshot.appSettings.copy(id = 1)
            settingsDao.upsertSettings(cleanSettings)
        }
        prefs.updateSettings(snapshot.appSettings.copy(id = 1))
    }

    private suspend fun recalculateProjectPaymentInternal(projectId: Long) {
        val project = projectDao.getProjectHeaderOnce(projectId) ?: return
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
            return isProjectOverdue(project.projectStatus, project.deadlineEpochDay, todayEpochDay)
        }

        fun isProjectOverdue(projectStatus: String, deadlineEpochDay: Long?, todayEpochDay: Long): Boolean {
            val status = ProjectStatus.fromName(projectStatus)
            if (status == ProjectStatus.COMPLETED || status == ProjectStatus.CANCELLED) return false
            val deadline = deadlineEpochDay ?: return false
            return deadline < todayEpochDay
        }

        fun formatEpochMonthYear(epochDay: Long): String {
            val date = LocalDate.ofEpochDay(epochDay)
            return Formatters.formatShortMonth(date.year, date.monthValue)
        }

        fun formatMillisDate(millis: Long): String {
            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            return Formatters.formatDate(date)
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
    val appSettings: AppSettingsEntity,
    val notes: List<NoteEntity> = emptyList(),
    val projectRevisions: List<ProjectRevisionEntity> = emptyList(),
    val careerGoals: List<CareerGoalEntity> = emptyList()
)
