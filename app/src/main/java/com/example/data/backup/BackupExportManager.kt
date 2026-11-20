package com.example.data.backup

import androidx.room.withTransaction
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
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PlatformType
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.RestoreMode
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
import com.example.data.repository.DatabaseSnapshot
import com.example.util.Formatters
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
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

    private val payloadAdapter = moshi.adapter(BackupPayloadDto::class.java).indent("  ")
    private val profileAdapter = moshi.adapter(EditorProfileDto::class.java)
    private val settingsAdapter = moshi.adapter(AppSettingsDto::class.java)
    private val clientAdapter = moshi.adapter(ClientDto::class.java)
    private val projectAdapter = moshi.adapter(ProjectDto::class.java)
    private val calendarAdapter = moshi.adapter(CalendarDayDto::class.java)
    private val incomeAdapter = moshi.adapter(IncomeTransactionDto::class.java)
    private val expenseAdapter = moshi.adapter(ExpenseTransactionDto::class.java)
    private val noteAdapter = moshi.adapter(NoteDto::class.java)
    private val revisionAdapter = moshi.adapter(ProjectRevisionDto::class.java)
    private val goalAdapter = moshi.adapter(CareerGoalDto::class.java)

    companion object {
        private const val STREAM_CHUNK_SIZE = 500
    }

    // --- Legacy String-based helpers (for small in-memory payloads & compatibility) ---

    fun serializeBackup(snapshot: DatabaseSnapshot): String {
        return payloadAdapter.toJson(snapshot.toDto())
    }

    fun parseAndValidateBackup(json: String): Result<BackupPayloadDto> {
        return runCatching {
            require(json.isNotBlank()) { "Backup file is empty." }
            val dto = payloadAdapter.fromJson(json) ?: throw IllegalArgumentException("Invalid JSON backup structure.")
            dto.toSnapshotValidated()
            dto
        }
    }

    // --- True Streaming JSON Backup (constant O(1) RAM for 1,000,000+ rows) ---

    suspend fun writeBackupJsonStreaming(
        outputStream: OutputStream,
        repository: CareerRepository
    ) = withContext(Dispatchers.IO) {
        val profile = repository.getEditorProfileFullSafe()
        val settings = repository.db.appSettingsDao().getSettingsOnce() ?: AppSettingsEntity()

        val bufferedSink = outputStream.sink().buffer()
        val writer = JsonWriter.of(bufferedSink)
        writer.indent = "  "

        writer.beginObject()
        writer.name("schemaVersion").value(2L)
        writer.name("appName").value("Rohan Edits App")
        writer.name("exportedAtMillis").value(System.currentTimeMillis())

        writer.name("editorProfile")
        profileAdapter.toJson(
            writer,
            EditorProfileDto(
                id = profile.id,
                name = profile.name,
                brandName = profile.brandName,
                profession = profile.profession,
                officialStartDateEpochDay = profile.officialStartDateEpochDay,
                professionalExperience = profile.professionalExperience,
                skills = profile.skills,
                editingSoftware = profile.editingSoftware,
                specializations = profile.specializations,
                portfolioLink = profile.portfolioLink,
                personalNotes = profile.personalNotes,
                updatedAtMillis = profile.updatedAtMillis
            )
        )

        writer.name("appSettings")
        settingsAdapter.toJson(
            writer,
            AppSettingsDto(
                id = settings.id,
                themeMode = settings.themeMode,
                dateFormat = settings.dateFormat,
                currencySymbol = settings.currencySymbol,
                confirmBeforeDelete = settings.confirmBeforeDelete,
                deadlineRemindersEnabled = settings.deadlineRemindersEnabled
            )
        )

        writer.name("clients").beginArray()
        var lastClientId = 0L
        while (true) {
            val chunk = repository.db.clientDao().getClientsChunkAfterId(lastClientId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (c in chunk) {
                clientAdapter.toJson(
                    writer,
                    ClientDto(
                        id = c.id,
                        name = c.name,
                        platform = c.platform,
                        clientType = c.clientType,
                        mobileNumber = c.mobileNumber,
                        firstProjectDateEpochDay = c.firstProjectDateEpochDay,
                        instructions = c.instructions,
                        preferredEditingStyle = c.preferredEditingStyle,
                        perVideoPrice = c.perVideoPrice,
                        additionalNotes = c.additionalNotes,
                        createdAtMillis = c.createdAtMillis,
                        updatedAtMillis = c.updatedAtMillis
                    )
                )
            }
            lastClientId = chunk.last().id
        }
        writer.endArray()

        writer.name("projects").beginArray()
        var lastProjectId = 0L
        while (true) {
            val chunk = repository.db.projectDao().getProjectsChunkAfterId(lastProjectId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (p in chunk) {
                projectAdapter.toJson(
                    writer,
                    ProjectDto(
                        id = p.id,
                        clientId = p.clientId,
                        category = p.category,
                        title = p.title,
                        projectType = p.projectType,
                        priority = p.priority,
                        videoDuration = p.videoDuration,
                        projectDateEpochDay = p.projectDateEpochDay,
                        deadlineEpochDay = p.deadlineEpochDay,
                        projectAmount = p.projectAmount,
                        amountPaid = p.amountPaid,
                        paymentStatus = p.paymentStatus,
                        projectStatus = p.projectStatus,
                        clientRating = p.clientRating,
                        clientFeedback = p.clientFeedback,
                        additionalNotes = p.additionalNotes,
                        createdAtMillis = p.createdAtMillis,
                        updatedAtMillis = p.updatedAtMillis
                    )
                )
            }
            lastProjectId = chunk.last().id
        }
        writer.endArray()

        writer.name("calendarDays").beginArray()
        var lastEpoch = Long.MIN_VALUE
        while (true) {
            val chunk = repository.db.calendarDayDao().getCalendarDaysChunkAfterEpoch(lastEpoch, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (d in chunk) {
                calendarAdapter.toJson(
                    writer,
                    CalendarDayDto(
                        dateEpochDay = d.dateEpochDay,
                        status = d.status,
                        note = d.note,
                        updatedAtMillis = d.updatedAtMillis
                    )
                )
            }
            lastEpoch = chunk.last().dateEpochDay
        }
        writer.endArray()

        writer.name("incomeTransactions").beginArray()
        var lastIncomeId = 0L
        while (true) {
            val chunk = repository.db.incomeDao().getIncomeChunkAfterId(lastIncomeId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (tx in chunk) {
                incomeAdapter.toJson(
                    writer,
                    IncomeTransactionDto(
                        id = tx.id,
                        source = tx.source,
                        clientId = tx.clientId,
                        projectId = tx.projectId,
                        clientNameSnapshot = tx.clientNameSnapshot,
                        projectTitleSnapshot = tx.projectTitleSnapshot,
                        amount = tx.amount,
                        dateEpochDay = tx.dateEpochDay,
                        paymentMethod = tx.paymentMethod,
                        notes = tx.notes,
                        createdAtMillis = tx.createdAtMillis
                    )
                )
            }
            lastIncomeId = chunk.last().id
        }
        writer.endArray()

        writer.name("expenseTransactions").beginArray()
        var lastExpenseId = 0L
        while (true) {
            val chunk = repository.db.expenseDao().getExpensesChunkAfterId(lastExpenseId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (ex in chunk) {
                expenseAdapter.toJson(
                    writer,
                    ExpenseTransactionDto(
                        id = ex.id,
                        category = ex.category,
                        description = ex.description,
                        amount = ex.amount,
                        dateEpochDay = ex.dateEpochDay,
                        notes = ex.notes,
                        createdAtMillis = ex.createdAtMillis
                    )
                )
            }
            lastExpenseId = chunk.last().id
        }
        writer.endArray()

        writer.name("notes").beginArray()
        var lastNoteId = 0L
        while (true) {
            val chunk = repository.db.noteDao().getNotesChunkAfterId(lastNoteId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (n in chunk) {
                noteAdapter.toJson(
                    writer,
                    NoteDto(
                        id = n.id,
                        title = n.title,
                        content = n.content,
                        category = n.category,
                        clientId = n.clientId,
                        projectId = n.projectId,
                        isPinned = n.isPinned,
                        createdAtMillis = n.createdAtMillis,
                        updatedAtMillis = n.updatedAtMillis
                    )
                )
            }
            lastNoteId = chunk.last().id
        }
        writer.endArray()

        writer.name("projectRevisions").beginArray()
        var lastRevId = 0L
        while (true) {
            val chunk = repository.db.projectRevisionDao().getRevisionsChunkAfterId(lastRevId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (r in chunk) {
                revisionAdapter.toJson(
                    writer,
                    ProjectRevisionDto(
                        id = r.id,
                        projectId = r.projectId,
                        versionNumber = r.versionNumber,
                        summary = r.summary,
                        instructionsSnapshot = r.instructionsSnapshot,
                        statusSnapshot = r.statusSnapshot,
                        createdAtMillis = r.createdAtMillis
                    )
                )
            }
            lastRevId = chunk.last().id
        }
        writer.endArray()

        writer.name("careerGoals").beginArray()
        var lastGoalId = 0L
        while (true) {
            val chunk = repository.db.careerGoalDao().getGoalsChunkAfterId(lastGoalId, STREAM_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (g in chunk) {
                goalAdapter.toJson(
                    writer,
                    CareerGoalDto(
                        id = g.id,
                        title = g.title,
                        description = g.description,
                        targetMetric = g.targetMetric,
                        targetValue = g.targetValue,
                        currentValue = g.currentValue,
                        deadlineEpochDay = g.deadlineEpochDay,
                        status = g.status,
                        createdAtMillis = g.createdAtMillis,
                        updatedAtMillis = g.updatedAtMillis
                    )
                )
            }
            lastGoalId = chunk.last().id
        }
        writer.endArray()

        writer.endObject()
        writer.flush()
        bufferedSink.flush()
    }

    // --- Streaming Backup Inspection (validates schema & counts without allocating lists) ---

    suspend fun inspectBackupStreaming(inputStream: InputStream): Result<BackupFileInspection> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = inputStream.source().buffer()
                val reader = JsonReader.of(source)
                reader.beginObject()

                var schemaVersion = 0
                var appName = "Rohan Edits App"
                var exportedAtMillis = 0L
                var hasProfile = false
                var clientCount = 0
                var projectCount = 0
                var calendarCount = 0
                var incomeCount = 0
                var expenseCount = 0
                var noteCount = 0
                var revisionCount = 0
                var goalCount = 0

                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "schemaVersion" -> schemaVersion = reader.nextInt()
                        "appName" -> appName = reader.nextString()
                        "exportedAtMillis" -> exportedAtMillis = reader.nextLong()
                        "editorProfile" -> {
                            val profile = profileAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid editorProfile object in backup.")
                            require(profile.name.isNotBlank()) { "Invalid backup: editor name is missing." }
                            hasProfile = true
                        }
                        "appSettings" -> {
                            settingsAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid appSettings object in backup.")
                        }
                        "clients" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = clientAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid client entry in backup.")
                                require(dto.name.isNotBlank()) { "Invalid client record found in backup." }
                                require(dto.perVideoPrice >= 0.0) { "Negative per-video price for client ${dto.name}." }
                                clientCount++
                            }
                            reader.endArray()
                        }
                        "projects" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = projectAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid project entry in backup.")
                                require(dto.title.isNotBlank()) { "Invalid project title in backup." }
                                require(dto.projectAmount >= 0.0) { "Negative project amount in backup." }
                                projectCount++
                            }
                            reader.endArray()
                        }
                        "calendarDays" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                calendarAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid calendar entry in backup.")
                                calendarCount++
                            }
                            reader.endArray()
                        }
                        "incomeTransactions" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = incomeAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid income entry in backup.")
                                require(dto.amount > 0.0) { "Invalid income amount in backup." }
                                incomeCount++
                            }
                            reader.endArray()
                        }
                        "expenseTransactions" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = expenseAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid expense entry in backup.")
                                require(dto.amount > 0.0) { "Invalid expense amount in backup." }
                                expenseCount++
                            }
                            reader.endArray()
                        }
                        "notes" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = noteAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid note entry in backup.")
                                require(dto.title.isNotBlank()) { "Invalid note title in backup." }
                                noteCount++
                            }
                            reader.endArray()
                        }
                        "projectRevisions" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = revisionAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid revision entry in backup.")
                                require(dto.summary.isNotBlank()) { "Invalid revision summary in backup." }
                                revisionCount++
                            }
                            reader.endArray()
                        }
                        "careerGoals" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val dto = goalAdapter.fromJson(reader)
                                    ?: throw IllegalArgumentException("Invalid goal entry in backup.")
                                require(dto.title.isNotBlank()) { "Invalid goal title in backup." }
                                require(dto.targetValue > 0.0) { "Invalid goal target value in backup." }
                                goalCount++
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                require(schemaVersion >= 1) { "Unsupported backup schema version ($schemaVersion)." }
                require(hasProfile) { "Backup file does not contain editorProfile." }

                BackupFileInspection(
                    schemaVersion = schemaVersion,
                    appName = appName,
                    exportedAtMillis = exportedAtMillis,
                    clientCount = clientCount,
                    projectCount = projectCount,
                    calendarDayCount = calendarCount,
                    incomeCount = incomeCount,
                    expenseCount = expenseCount,
                    noteCount = noteCount,
                    revisionCount = revisionCount,
                    goalCount = goalCount
                )
            }
        }

    // --- Streaming Atomic Transactional Restore (rolls back completely on any error) ---

    suspend fun restoreBackupStreaming(
        inputStream: InputStream,
        repository: CareerRepository,
        mode: RestoreMode = RestoreMode.REPLACE_ALL
    ): RestoreSummary = withContext(Dispatchers.IO) {
        val db = repository.db
        val clientDao = db.clientDao()
        val projectDao = db.projectDao()
        val calendarDao = db.calendarDayDao()
        val incomeDao = db.incomeDao()
        val expenseDao = db.expenseDao()
        val noteDao = db.noteDao()
        val revisionDao = db.projectRevisionDao()
        val goalDao = db.careerGoalDao()
        val editorDao = db.editorProfileDao()
        val settingsDao = db.appSettingsDao()

        var insertedCount = 0
        var skippedDuplicatesCount = 0
        var totalProcessed = 0
        var restoredSettings: AppSettingsEntity? = null

        val source = inputStream.source().buffer()
        val reader = JsonReader.of(source)

        db.withTransaction {
            if (mode == RestoreMode.REPLACE_ALL) {
                revisionDao.deleteAllRevisions()
                noteDao.deleteAllNotes()
                goalDao.deleteAllGoals()
                incomeDao.deleteAllIncome()
                expenseDao.deleteAllExpenses()
                projectDao.deleteAllProjects()
                clientDao.deleteAllClients()
                calendarDao.deleteAllCalendarDays()
            }

            // Remap tables used ONLY in MERGE_SKIP_DUPLICATES when IDs collide or match natural duplicates
            val clientRemap = HashMap<Long, Long>()
            val projectRemap = HashMap<Long, Long>()

            var schemaVersion = 0
            var hasProfile = false

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "schemaVersion" -> {
                        schemaVersion = reader.nextInt()
                        require(schemaVersion >= 1) { "Unsupported backup schema version ($schemaVersion)." }
                    }
                    "editorProfile" -> {
                        val dto = profileAdapter.fromJson(reader)
                            ?: throw IllegalArgumentException("Invalid editorProfile object in backup.")
                        require(dto.name.isNotBlank()) { "Invalid backup: editor name missing." }
                        hasProfile = true
                        if (mode == RestoreMode.REPLACE_ALL) {
                            editorDao.upsertProfile(
                                EditorProfileEntity(
                                    id = 1,
                                    name = dto.name,
                                    brandName = dto.brandName.ifBlank { "Rohan Edits" },
                                    profession = dto.profession,
                                    officialStartDateEpochDay = dto.officialStartDateEpochDay,
                                    professionalExperience = dto.professionalExperience,
                                    skills = dto.skills,
                                    editingSoftware = dto.editingSoftware,
                                    specializations = dto.specializations,
                                    portfolioLink = dto.portfolioLink,
                                    personalNotes = dto.personalNotes,
                                    updatedAtMillis = dto.updatedAtMillis
                                )
                            )
                        }
                    }
                    "appSettings" -> {
                        val dto = settingsAdapter.fromJson(reader)
                            ?: throw IllegalArgumentException("Invalid appSettings object in backup.")
                        val entity = AppSettingsEntity(
                            id = 1,
                            themeMode = dto.themeMode,
                            dateFormat = dto.dateFormat,
                            currencySymbol = dto.currencySymbol.ifBlank { "₹" },
                            confirmBeforeDelete = dto.confirmBeforeDelete,
                            deadlineRemindersEnabled = dto.deadlineRemindersEnabled
                        )
                        if (mode == RestoreMode.REPLACE_ALL) {
                            settingsDao.upsertSettings(entity)
                            restoredSettings = entity
                        }
                    }
                    "clients" -> {
                        reader.beginArray()
                        val batch = ArrayList<ClientEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = clientAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid client record in backup.")
                            require(dto.name.isNotBlank()) { "Invalid client record found in backup." }
                            require(dto.perVideoPrice >= 0.0) { "Invalid negative price for client ${dto.name}." }
                            totalProcessed++

                            if (mode == RestoreMode.REPLACE_ALL) {
                                batch.add(
                                    ClientEntity(
                                        id = dto.id,
                                        name = dto.name,
                                        platform = dto.platform,
                                        clientType = dto.clientType,
                                        mobileNumber = dto.mobileNumber,
                                        firstProjectDateEpochDay = dto.firstProjectDateEpochDay,
                                        instructions = dto.instructions,
                                        preferredEditingStyle = dto.preferredEditingStyle,
                                        perVideoPrice = dto.perVideoPrice,
                                        additionalNotes = dto.additionalNotes,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                                if (batch.size >= STREAM_CHUNK_SIZE) {
                                    clientDao.insertAllClients(batch)
                                    batch.clear()
                                }
                            } else {
                                val dupId = clientDao.findClientIdByNameAndPlatform(dto.name, dto.platform)
                                if (dupId != null) {
                                    skippedDuplicatesCount++
                                    if (dupId != dto.id) clientRemap[dto.id] = dupId
                                } else {
                                    val idCollision = dto.id > 0L && clientDao.existsById(dto.id)
                                    val entity = ClientEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        name = dto.name,
                                        platform = dto.platform,
                                        clientType = dto.clientType,
                                        mobileNumber = dto.mobileNumber,
                                        firstProjectDateEpochDay = dto.firstProjectDateEpochDay,
                                        instructions = dto.instructions,
                                        preferredEditingStyle = dto.preferredEditingStyle,
                                        perVideoPrice = dto.perVideoPrice,
                                        additionalNotes = dto.additionalNotes,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                    if (idCollision) {
                                        val newId = clientDao.insertClient(entity)
                                        clientRemap[dto.id] = newId
                                    } else {
                                        batch.add(entity)
                                        if (batch.size >= STREAM_CHUNK_SIZE) {
                                            clientDao.insertAllClients(batch)
                                            batch.clear()
                                        }
                                    }
                                    insertedCount++
                                }
                            }
                        }
                        if (batch.isNotEmpty()) {
                            clientDao.insertAllClients(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "projects" -> {
                        reader.beginArray()
                        val batch = ArrayList<ProjectEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = projectAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid project record in backup.")
                            require(dto.title.isNotBlank()) { "Invalid project title in backup." }
                            require(dto.projectAmount >= 0.0) { "Invalid negative project amount in backup." }
                            totalProcessed++

                            val resolvedClientId = dto.clientId?.let { origCid ->
                                val mapped = clientRemap[origCid] ?: origCid
                                if (mode == RestoreMode.REPLACE_ALL || clientDao.existsById(mapped)) mapped else null
                            }

                            if (mode == RestoreMode.REPLACE_ALL) {
                                batch.add(
                                    ProjectEntity(
                                        id = dto.id,
                                        clientId = resolvedClientId,
                                        category = dto.category,
                                        title = dto.title,
                                        projectType = dto.projectType,
                                        priority = dto.priority,
                                        videoDuration = dto.videoDuration,
                                        projectDateEpochDay = dto.projectDateEpochDay,
                                        deadlineEpochDay = dto.deadlineEpochDay,
                                        projectAmount = dto.projectAmount,
                                        amountPaid = dto.amountPaid.coerceAtLeast(0.0),
                                        paymentStatus = dto.paymentStatus,
                                        projectStatus = dto.projectStatus,
                                        clientRating = dto.clientRating,
                                        clientFeedback = dto.clientFeedback,
                                        additionalNotes = dto.additionalNotes,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                                if (batch.size >= STREAM_CHUNK_SIZE) {
                                    projectDao.insertAllProjects(batch)
                                    batch.clear()
                                }
                            } else {
                                val dupId = projectDao.findProjectIdByNaturalKey(
                                    title = dto.title,
                                    dateEpoch = dto.projectDateEpochDay,
                                    clientId = resolvedClientId
                                )
                                if (dupId != null) {
                                    skippedDuplicatesCount++
                                    if (dupId != dto.id) projectRemap[dto.id] = dupId
                                } else {
                                    val idCollision = dto.id > 0L && projectDao.existsById(dto.id)
                                    val entity = ProjectEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        clientId = resolvedClientId,
                                        category = dto.category,
                                        title = dto.title,
                                        projectType = dto.projectType,
                                        priority = dto.priority,
                                        videoDuration = dto.videoDuration,
                                        projectDateEpochDay = dto.projectDateEpochDay,
                                        deadlineEpochDay = dto.deadlineEpochDay,
                                        projectAmount = dto.projectAmount,
                                        amountPaid = dto.amountPaid.coerceAtLeast(0.0),
                                        paymentStatus = dto.paymentStatus,
                                        projectStatus = dto.projectStatus,
                                        clientRating = dto.clientRating,
                                        clientFeedback = dto.clientFeedback,
                                        additionalNotes = dto.additionalNotes,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                    if (idCollision) {
                                        val newId = projectDao.insertProject(entity)
                                        projectRemap[dto.id] = newId
                                    } else {
                                        batch.add(entity)
                                        if (batch.size >= STREAM_CHUNK_SIZE) {
                                            projectDao.insertAllProjects(batch)
                                            batch.clear()
                                        }
                                    }
                                    insertedCount++
                                }
                            }
                        }
                        if (batch.isNotEmpty()) {
                            projectDao.insertAllProjects(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "calendarDays" -> {
                        reader.beginArray()
                        val batch = ArrayList<CalendarDayEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = calendarAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid calendar entry in backup.")
                            totalProcessed++
                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES && calendarDao.getDayOnce(dto.dateEpochDay) != null) {
                                skippedDuplicatesCount++
                            } else {
                                batch.add(
                                    CalendarDayEntity(
                                        dateEpochDay = dto.dateEpochDay,
                                        status = dto.status,
                                        note = dto.note,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                                if (batch.size >= STREAM_CHUNK_SIZE) {
                                    calendarDao.insertAllCalendarDays(batch)
                                    batch.clear()
                                }
                            }
                        }
                        if (batch.isNotEmpty()) {
                            calendarDao.insertAllCalendarDays(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "incomeTransactions" -> {
                        reader.beginArray()
                        val batch = ArrayList<IncomeTransactionEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = incomeAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid income entry in backup.")
                            require(dto.amount > 0.0) { "Invalid income amount in backup." }
                            totalProcessed++

                            val resolvedClientId = dto.clientId?.let { clientRemap[it] ?: it }
                            val resolvedProjectId = dto.projectId?.let { projectRemap[it] ?: it }

                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) {
                                val dup = incomeDao.findIncomeIdByNaturalKey(
                                    source = dto.source,
                                    dateEpoch = dto.dateEpochDay,
                                    amount = dto.amount,
                                    projectId = resolvedProjectId
                                )
                                if (dup != null) {
                                    skippedDuplicatesCount++
                                    continue
                                }
                                val idCollision = dto.id > 0L && incomeDao.existsById(dto.id)
                                batch.add(
                                    IncomeTransactionEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        source = dto.source,
                                        clientId = resolvedClientId,
                                        projectId = resolvedProjectId,
                                        clientNameSnapshot = dto.clientNameSnapshot,
                                        projectTitleSnapshot = dto.projectTitleSnapshot,
                                        amount = dto.amount,
                                        dateEpochDay = dto.dateEpochDay,
                                        paymentMethod = dto.paymentMethod,
                                        notes = dto.notes,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            } else {
                                batch.add(
                                    IncomeTransactionEntity(
                                        id = dto.id,
                                        source = dto.source,
                                        clientId = resolvedClientId,
                                        projectId = resolvedProjectId,
                                        clientNameSnapshot = dto.clientNameSnapshot,
                                        projectTitleSnapshot = dto.projectTitleSnapshot,
                                        amount = dto.amount,
                                        dateEpochDay = dto.dateEpochDay,
                                        paymentMethod = dto.paymentMethod,
                                        notes = dto.notes,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            }
                            if (batch.size >= STREAM_CHUNK_SIZE) {
                                incomeDao.insertAllIncome(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            incomeDao.insertAllIncome(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "expenseTransactions" -> {
                        reader.beginArray()
                        val batch = ArrayList<ExpenseTransactionEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = expenseAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid expense entry in backup.")
                            require(dto.amount > 0.0) { "Invalid expense amount in backup." }
                            totalProcessed++

                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) {
                                val dup = expenseDao.findExpenseIdByNaturalKey(
                                    description = dto.description,
                                    category = dto.category,
                                    dateEpoch = dto.dateEpochDay,
                                    amount = dto.amount
                                )
                                if (dup != null) {
                                    skippedDuplicatesCount++
                                    continue
                                }
                                val idCollision = dto.id > 0L && expenseDao.existsById(dto.id)
                                batch.add(
                                    ExpenseTransactionEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        category = dto.category,
                                        description = dto.description,
                                        amount = dto.amount,
                                        dateEpochDay = dto.dateEpochDay,
                                        notes = dto.notes,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            } else {
                                batch.add(
                                    ExpenseTransactionEntity(
                                        id = dto.id,
                                        category = dto.category,
                                        description = dto.description,
                                        amount = dto.amount,
                                        dateEpochDay = dto.dateEpochDay,
                                        notes = dto.notes,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            }
                            if (batch.size >= STREAM_CHUNK_SIZE) {
                                expenseDao.insertAllExpenses(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            expenseDao.insertAllExpenses(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "notes" -> {
                        reader.beginArray()
                        val batch = ArrayList<NoteEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = noteAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid note entry in backup.")
                            require(dto.title.isNotBlank()) { "Invalid note title in backup." }
                            totalProcessed++

                            val resolvedClientId = dto.clientId?.let { clientRemap[it] ?: it }
                            val resolvedProjectId = dto.projectId?.let { projectRemap[it] ?: it }

                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) {
                                val dup = noteDao.findNoteIdByNaturalKey(dto.title, dto.createdAtMillis)
                                if (dup != null) {
                                    skippedDuplicatesCount++
                                    continue
                                }
                                val idCollision = dto.id > 0L && noteDao.existsById(dto.id)
                                batch.add(
                                    NoteEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        title = dto.title,
                                        content = dto.content,
                                        category = dto.category,
                                        clientId = resolvedClientId,
                                        projectId = resolvedProjectId,
                                        isPinned = dto.isPinned,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                            } else {
                                batch.add(
                                    NoteEntity(
                                        id = dto.id,
                                        title = dto.title,
                                        content = dto.content,
                                        category = dto.category,
                                        clientId = resolvedClientId,
                                        projectId = resolvedProjectId,
                                        isPinned = dto.isPinned,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                            }
                            if (batch.size >= STREAM_CHUNK_SIZE) {
                                noteDao.insertAllNotes(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            noteDao.insertAllNotes(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "projectRevisions" -> {
                        reader.beginArray()
                        val batch = ArrayList<ProjectRevisionEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = revisionAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid revision entry in backup.")
                            require(dto.summary.isNotBlank()) { "Invalid revision summary in backup." }
                            totalProcessed++

                            val resolvedProjectId = projectRemap[dto.projectId] ?: dto.projectId
                            if (!projectDao.existsById(resolvedProjectId)) {
                                throw IllegalArgumentException("Revision references missing project (${dto.projectId}).")
                            }

                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) {
                                val dup = revisionDao.findRevisionIdByNaturalKey(
                                    projectId = resolvedProjectId,
                                    versionNumber = dto.versionNumber,
                                    createdAt = dto.createdAtMillis
                                )
                                if (dup != null) {
                                    skippedDuplicatesCount++
                                    continue
                                }
                                val idCollision = dto.id > 0L && revisionDao.existsById(dto.id)
                                batch.add(
                                    ProjectRevisionEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        projectId = resolvedProjectId,
                                        versionNumber = dto.versionNumber,
                                        summary = dto.summary,
                                        instructionsSnapshot = dto.instructionsSnapshot,
                                        statusSnapshot = dto.statusSnapshot,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            } else {
                                batch.add(
                                    ProjectRevisionEntity(
                                        id = dto.id,
                                        projectId = resolvedProjectId,
                                        versionNumber = dto.versionNumber,
                                        summary = dto.summary,
                                        instructionsSnapshot = dto.instructionsSnapshot,
                                        statusSnapshot = dto.statusSnapshot,
                                        createdAtMillis = dto.createdAtMillis
                                    )
                                )
                                insertedCount++
                            }
                            if (batch.size >= STREAM_CHUNK_SIZE) {
                                revisionDao.insertAllRevisions(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            revisionDao.insertAllRevisions(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    "careerGoals" -> {
                        reader.beginArray()
                        val batch = ArrayList<CareerGoalEntity>(STREAM_CHUNK_SIZE)
                        while (reader.hasNext()) {
                            val dto = goalAdapter.fromJson(reader)
                                ?: throw IllegalArgumentException("Invalid career goal entry in backup.")
                            require(dto.title.isNotBlank()) { "Invalid goal title in backup." }
                            require(dto.targetValue > 0.0) { "Invalid goal target value in backup." }
                            totalProcessed++

                            if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) {
                                val dup = goalDao.findGoalIdByNaturalKey(dto.title, dto.createdAtMillis)
                                if (dup != null) {
                                    skippedDuplicatesCount++
                                    continue
                                }
                                val idCollision = dto.id > 0L && goalDao.existsById(dto.id)
                                batch.add(
                                    CareerGoalEntity(
                                        id = if (idCollision) 0L else dto.id,
                                        title = dto.title,
                                        description = dto.description,
                                        targetMetric = dto.targetMetric,
                                        targetValue = dto.targetValue,
                                        currentValue = dto.currentValue,
                                        deadlineEpochDay = dto.deadlineEpochDay,
                                        status = dto.status,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                            } else {
                                batch.add(
                                    CareerGoalEntity(
                                        id = dto.id,
                                        title = dto.title,
                                        description = dto.description,
                                        targetMetric = dto.targetMetric,
                                        targetValue = dto.targetValue,
                                        currentValue = dto.currentValue,
                                        deadlineEpochDay = dto.deadlineEpochDay,
                                        status = dto.status,
                                        createdAtMillis = dto.createdAtMillis,
                                        updatedAtMillis = dto.updatedAtMillis
                                    )
                                )
                                insertedCount++
                            }
                            if (batch.size >= STREAM_CHUNK_SIZE) {
                                goalDao.insertAllGoals(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            goalDao.insertAllGoals(batch)
                            batch.clear()
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            require(schemaVersion >= 1) { "Unsupported backup schema version." }
            require(hasProfile) { "Invalid backup: editor profile missing." }

            projectDao.recalculateAllProjectPaymentsBulk(System.currentTimeMillis())
        }

        if (mode == RestoreMode.REPLACE_ALL && restoredSettings != null) {
            repository.updateSettings(restoredSettings!!)
        }

        RestoreSummary(
            mode = mode,
            insertedCount = insertedCount,
            skippedDuplicatesCount = skippedDuplicatesCount,
            totalProcessed = totalProcessed
        )
    }

    // --- True Streaming CSV Export (chunked SQL cursors -> BufferedWriter, zero full-table loads) ---

    suspend fun writeCsvStreaming(
        target: ExportTarget,
        outputStream: OutputStream,
        repository: CareerRepository,
        today: LocalDate,
        dateFormat: DateFormatOption
    ) = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 65536)
        when (target) {
            ExportTarget.CLIENT_LIST -> {
                writer.write("ID,Client Name,Platform,Client Type,Mobile,First Project Date,Per Video Price (INR),Preferred Style,Instructions,Notes\n")
                var lastId = 0L
                while (true) {
                    val chunk = repository.db.clientDao().getClientsChunkAfterId(lastId, STREAM_CHUNK_SIZE)
                    if (chunk.isEmpty()) break
                    for (c in chunk) {
                        val firstDate = c.firstProjectDateEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: ""
                        writer.write(
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
                        writer.newLine()
                    }
                    lastId = chunk.last().id
                }
            }

            ExportTarget.PROJECT_LIST -> {
                writer.write("ID,Title,Category,Client,Project Type,Priority,Duration,Project Date,Deadline,Amount (INR),Amount Paid (INR),Pending (INR),Payment Status,Project Status,Rating,Feedback,Notes\n")
                var lastId = 0L
                while (true) {
                    val chunk = repository.db.projectDao().getProjectExportChunkAfterId(lastId, STREAM_CHUNK_SIZE)
                    if (chunk.isEmpty()) break
                    for (p in chunk) {
                        val pending = if (ProjectStatus.fromName(p.projectStatus) == ProjectStatus.CANCELLED) {
                            0.0
                        } else {
                            (p.projectAmount - p.amountPaid).coerceAtLeast(0.0)
                        }
                        val deadlineStr = p.deadlineEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: ""
                        writer.write(
                            csvRow(
                                p.id.toString(),
                                p.title,
                                VideoCategory.fromName(p.category).label,
                                p.clientName.orEmpty(),
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
                        writer.newLine()
                    }
                    lastId = chunk.last().id
                }
            }

            ExportTarget.EDITING_CALENDAR -> {
                writer.write("Date,Status,Note\n")
                var lastEpoch = Long.MIN_VALUE
                while (true) {
                    val chunk = repository.db.calendarDayDao().getCalendarDaysChunkAfterEpoch(lastEpoch, STREAM_CHUNK_SIZE)
                    if (chunk.isEmpty()) break
                    for (d in chunk) {
                        writer.write(
                            csvRow(
                                Formatters.formatDate(d.dateEpochDay, dateFormat),
                                CalendarStatus.fromName(d.status).label,
                                d.note
                            )
                        )
                        writer.newLine()
                    }
                    lastEpoch = chunk.last().dateEpochDay
                }
            }

            ExportTarget.INCOME_RECORDS -> {
                writer.write("ID,Date,Source,Client,Related Project,Amount (INR),Payment Method,Notes\n")
                var lastId = 0L
                while (true) {
                    val chunk = repository.db.incomeDao().getIncomeExportChunkAfterId(lastId, STREAM_CHUNK_SIZE)
                    if (chunk.isEmpty()) break
                    for (tx in chunk) {
                        writer.write(
                            csvRow(
                                tx.id.toString(),
                                Formatters.formatDate(tx.dateEpochDay, dateFormat),
                                tx.source,
                                tx.clientName,
                                tx.projectTitle,
                                tx.amount.toString(),
                                PaymentMethod.fromName(tx.paymentMethod).label,
                                tx.notes
                            )
                        )
                        writer.newLine()
                    }
                    lastId = chunk.last().id
                }
            }

            ExportTarget.EXPENSE_RECORDS -> {
                writer.write("ID,Date,Category,Description,Amount (INR),Notes\n")
                var lastId = 0L
                while (true) {
                    val chunk = repository.db.expenseDao().getExpensesChunkAfterId(lastId, STREAM_CHUNK_SIZE)
                    if (chunk.isEmpty()) break
                    for (ex in chunk) {
                        writer.write(
                            csvRow(
                                ex.id.toString(),
                                Formatters.formatDate(ex.dateEpochDay, dateFormat),
                                ExpenseCategory.fromName(ex.category).label,
                                ex.description,
                                ex.amount.toString(),
                                ex.notes
                            )
                        )
                        writer.newLine()
                    }
                    lastId = chunk.last().id
                }
            }

            ExportTarget.FINANCIAL_SUMMARY -> {
                val profile = repository.getEditorProfileFullSafe()
                val startDate = LocalDate.ofEpochDay(profile.officialStartDateEpochDay)
                val metrics = repository.observeHomeMetrics(today, startDate).first()
                val counts = repository.db.dashboardAggregateDao().getStorageRecordCounts()
                val netBalance = metrics.totalEarnings - metrics.totalExpenses

                writer.write("Metric,Amount (INR)\n")
                writer.write(csvRow("Total Recorded Income", metrics.totalEarnings.toString())); writer.newLine()
                writer.write(csvRow("Total Recorded Expenses", metrics.totalExpenses.toString())); writer.newLine()
                writer.write(csvRow("Net Balance", netBalance.toString())); writer.newLine()
                writer.write(csvRow("Pending Client Payments", metrics.pendingClientPayments.toString())); writer.newLine()
                writer.write(csvRow("Income Transaction Count", counts.incomeTransactions.toString())); writer.newLine()
                writer.write(csvRow("Expense Transaction Count", counts.expenseTransactions.toString())); writer.newLine()
            }

            ExportTarget.ANALYTICS_SUMMARY -> {
                val profile = repository.getEditorProfileFullSafe()
                val startDate = LocalDate.ofEpochDay(profile.officialStartDateEpochDay)
                val metrics = repository.observeHomeMetrics(today, startDate).first()
                val consistency = Formatters.calculateConsistencyPercent(metrics.totalEditingDays, metrics.totalMissedDays)
                val exp = Formatters.calculateExperience(startDate, today)

                writer.write("Metric,Value\n")
                writer.write(csvRow("Editor Name", profile.name)); writer.newLine()
                writer.write(csvRow("Brand", profile.brandName)); writer.newLine()
                writer.write(csvRow("Official Start Date", Formatters.formatDate(startDate, dateFormat))); writer.newLine()
                writer.write(csvRow("Experience Days", exp.totalDays.toString())); writer.newLine()
                writer.write(csvRow("Total Editing Done Days", metrics.totalEditingDays.toString())); writer.newLine()
                writer.write(csvRow("Total Missed Days", metrics.totalMissedDays.toString())); writer.newLine()
                writer.write(csvRow("Editing Consistency (%)", Formatters.formatPercent(consistency))); writer.newLine()
                writer.write(csvRow("Total Clients", metrics.totalClients.toString())); writer.newLine()
                writer.write(csvRow("Total Projects", metrics.totalProjects.toString())); writer.newLine()
                writer.write(csvRow("Total Completed Videos", metrics.totalVideosCompleted.toString())); writer.newLine()
            }
        }
        writer.flush()
    }

    // --- Legacy snapshot CSV generator (kept for small in-memory test compatibility) ---

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
