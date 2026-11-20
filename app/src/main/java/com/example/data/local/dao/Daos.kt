package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.FormDraftEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditorProfileDao {
    @Query("SELECT * FROM editor_profile WHERE id = 1")
    fun observeProfile(): Flow<EditorProfileEntity?>

    @Query(
        """
        SELECT id, name, brandName, profession, officialStartDateEpochDay,
               '' AS professionalExperience, '' AS skills, '' AS editingSoftware,
               '' AS specializations, portfolioLink, '' AS personalNotes, updatedAtMillis
        FROM editor_profile WHERE id = 1
        """
    )
    fun observeProfileHeader(): Flow<EditorProfileEntity?>

    @Query(
        """
        SELECT id, name, brandName, profession, officialStartDateEpochDay,
               '' AS professionalExperience, skills, editingSoftware,
               specializations, portfolioLink, '' AS personalNotes, updatedAtMillis
        FROM editor_profile WHERE id = 1
        """
    )
    suspend fun getProfileHeaderOnce(): EditorProfileEntity?

    @Query("SELECT * FROM editor_profile WHERE id = 1")
    suspend fun getProfileOnce(): EditorProfileEntity?

    @Query("SELECT LENGTH(personalNotes) FROM editor_profile WHERE id = 1")
    suspend fun getPersonalNotesLength(): Int?

    @Query("SELECT LENGTH(professionalExperience) FROM editor_profile WHERE id = 1")
    suspend fun getExperienceLength(): Int?

    @Query("SELECT SUBSTR(personalNotes, :start1Based, :len) FROM editor_profile WHERE id = 1")
    suspend fun getPersonalNotesChunk(start1Based: Int, len: Int): String?

    @Query("SELECT SUBSTR(professionalExperience, :start1Based, :len) FROM editor_profile WHERE id = 1")
    suspend fun getExperienceChunk(start1Based: Int, len: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: EditorProfileEntity)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY updatedAtMillis DESC LIMIT 100")
    fun observeAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun observeClientById(clientId: Long): Flow<ClientEntity?>

    @Query(
        """
        SELECT id, name, platform, clientType, mobileNumber, firstProjectDateEpochDay,
               '' AS instructions, preferredEditingStyle, perVideoPrice, '' AS additionalNotes,
               createdAtMillis, updatedAtMillis
        FROM clients WHERE id = :clientId
        """
    )
    suspend fun getClientHeaderOnce(clientId: Long): ClientEntity?

    @Query("SELECT LENGTH(instructions) FROM clients WHERE id = :clientId")
    suspend fun getInstructionsLength(clientId: Long): Int?

    @Query("SELECT SUBSTR(instructions, :start1Based, :len) FROM clients WHERE id = :clientId")
    suspend fun getInstructionsChunk(clientId: Long, start1Based: Int, len: Int): String?

    @Query("SELECT LENGTH(additionalNotes) FROM clients WHERE id = :clientId")
    suspend fun getNotesLength(clientId: Long): Int?

    @Query("SELECT SUBSTR(additionalNotes, :start1Based, :len) FROM clients WHERE id = :clientId")
    suspend fun getNotesChunk(clientId: Long, start1Based: Int, len: Int): String?

    @Query("SELECT * FROM clients WHERE id = :clientId")
    suspend fun getClientByIdOnce(clientId: Long): ClientEntity?

    @Query("SELECT id FROM clients WHERE LOWER(name) = LOWER(:name) AND platform = :platform LIMIT 1")
    suspend fun findClientIdByNameAndPlatform(name: String, platform: String): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM clients WHERE id = :clientId)")
    suspend fun existsById(clientId: Long): Boolean

    @Query("SELECT * FROM clients ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getClientsPageOnce(limit: Int, offset: Int): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getClientsChunkAfterId(lastId: Long, limit: Int): List<ClientEntity>

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun countAllClients(): Long

    @Query(
        """
        SELECT id, name, platform, perVideoPrice
        FROM clients
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR platform LIKE '%' || :query || '%')
        ORDER BY updatedAtMillis DESC, LOWER(name) ASC
        LIMIT :limit
        """
    )
    suspend fun searchClientPickerOptions(query: String, limit: Int = 50): List<ClientPickerOption>

    @Query(
        """
        SELECT c.id, c.name, c.platform, c.clientType, c.perVideoPrice, c.updatedAtMillis,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id) AS totalProjects,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id AND p.projectStatus = 'COMPLETED') AS completedVideos,
            (SELECT COALESCE(SUM(i.amount), 0.0) FROM income_transactions i WHERE i.clientId = c.id) AS totalEarningsReceived,
            (SELECT COALESCE(SUM(CASE WHEN p.projectStatus != 'CANCELLED' AND p.projectAmount > p.amountPaid THEN p.projectAmount - p.amountPaid ELSE 0.0 END), 0.0) FROM projects p WHERE p.clientId = c.id) AS pendingPayments,
            c.updatedAtMillis AS lastActivityMillis
        FROM clients c
        WHERE c.id = :clientId
        """
    )
    fun observeClientSummary(clientId: Long): Flow<ClientSummaryRow?>

    @Query(
        """
        SELECT c.id, c.name, c.platform, c.clientType, c.perVideoPrice, c.updatedAtMillis,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id) AS totalProjects,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id AND p.projectStatus = 'COMPLETED') AS completedVideos,
            (SELECT COALESCE(SUM(i.amount), 0.0) FROM income_transactions i WHERE i.clientId = c.id) AS totalEarningsReceived,
            (SELECT COALESCE(SUM(CASE WHEN p.projectStatus != 'CANCELLED' AND p.projectAmount > p.amountPaid THEN p.projectAmount - p.amountPaid ELSE 0.0 END), 0.0) FROM projects p WHERE p.clientId = c.id) AS pendingPayments,
            c.updatedAtMillis AS lastActivityMillis
        FROM clients c
        WHERE (:clientType IS NULL OR c.clientType = :clientType)
          AND (:query = '' OR c.name LIKE '%' || :query || '%' OR c.preferredEditingStyle LIKE '%' || :query || '%' OR c.platform LIKE '%' || :query || '%')
        ORDER BY c.updatedAtMillis DESC, c.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryClientsRecent(query: String, clientType: String?, limit: Int, offset: Int): List<ClientSummaryRow>

    @Query(
        """
        SELECT c.id, c.name, c.platform, c.clientType, c.perVideoPrice, c.updatedAtMillis,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id) AS totalProjects,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id AND p.projectStatus = 'COMPLETED') AS completedVideos,
            (SELECT COALESCE(SUM(i.amount), 0.0) FROM income_transactions i WHERE i.clientId = c.id) AS totalEarningsReceived,
            (SELECT COALESCE(SUM(CASE WHEN p.projectStatus != 'CANCELLED' AND p.projectAmount > p.amountPaid THEN p.projectAmount - p.amountPaid ELSE 0.0 END), 0.0) FROM projects p WHERE p.clientId = c.id) AS pendingPayments,
            c.updatedAtMillis AS lastActivityMillis
        FROM clients c
        WHERE (:clientType IS NULL OR c.clientType = :clientType)
          AND (:query = '' OR c.name LIKE '%' || :query || '%' OR c.preferredEditingStyle LIKE '%' || :query || '%' OR c.platform LIKE '%' || :query || '%')
        ORDER BY LOWER(c.name) ASC, c.id ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryClientsByName(query: String, clientType: String?, limit: Int, offset: Int): List<ClientSummaryRow>

    @Query(
        """
        SELECT COUNT(*) FROM clients c
        WHERE (:clientType IS NULL OR c.clientType = :clientType)
          AND (:query = '' OR c.name LIKE '%' || :query || '%' OR c.preferredEditingStyle LIKE '%' || :query || '%' OR c.platform LIKE '%' || :query || '%')
        """
    )
    suspend fun countFilteredClients(query: String, clientType: String?): Int

    @Query(
        """
        SELECT c.id, c.name, c.platform, c.clientType, c.perVideoPrice, c.updatedAtMillis,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id) AS totalProjects,
            (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id AND p.projectStatus = 'COMPLETED') AS completedVideos,
            (SELECT COALESCE(SUM(i.amount), 0.0) FROM income_transactions i WHERE i.clientId = c.id) AS totalEarningsReceived,
            (SELECT COALESCE(SUM(CASE WHEN p.projectStatus != 'CANCELLED' AND p.projectAmount > p.amountPaid THEN p.projectAmount - p.amountPaid ELSE 0.0 END), 0.0) FROM projects p WHERE p.clientId = c.id) AS pendingPayments,
            c.updatedAtMillis AS lastActivityMillis
        FROM clients c
        ORDER BY (SELECT COUNT(*) FROM projects p WHERE p.clientId = c.id) DESC, c.updatedAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeTopActiveClients(limit: Int = 5): Flow<List<ClientSummaryRow>>

    @Query(
        """
        SELECT COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN clientType = 'NEW' THEN 1 ELSE 0 END), 0) AS newCount,
            COALESCE(SUM(CASE WHEN clientType = 'OLD' THEN 1 ELSE 0 END), 0) AS oldCount,
            COALESCE(SUM(CASE WHEN clientType = 'REGULAR' THEN 1 ELSE 0 END), 0) AS regularCount
        FROM clients
        """
    )
    fun observeClientTypeBreakdown(): Flow<ClientTypeBreakdownAggregate>

    @Query("UPDATE clients SET updatedAtMillis = :updatedAt WHERE id = :clientId")
    suspend fun touchClientActivity(clientId: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClientById(clientId: Long)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY projectDateEpochDay DESC, id DESC LIMIT 100")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectById(projectId: Long): Flow<ProjectEntity?>

    @Query(
        """
        SELECT id, clientId, category, title, projectType, priority, videoDuration,
               projectDateEpochDay, deadlineEpochDay, projectAmount, amountPaid,
               paymentStatus, projectStatus, clientRating,
               '' AS clientFeedback, '' AS additionalNotes, createdAtMillis, updatedAtMillis
        FROM projects WHERE id = :projectId
        """
    )
    suspend fun getProjectHeaderOnce(projectId: Long): ProjectEntity?

    @Query("SELECT LENGTH(clientFeedback) FROM projects WHERE id = :projectId")
    suspend fun getFeedbackLength(projectId: Long): Int?

    @Query("SELECT SUBSTR(clientFeedback, :start1Based, :len) FROM projects WHERE id = :projectId")
    suspend fun getFeedbackChunk(projectId: Long, start1Based: Int, len: Int): String?

    @Query("SELECT LENGTH(additionalNotes) FROM projects WHERE id = :projectId")
    suspend fun getNotesLength(projectId: Long): Int?

    @Query("SELECT SUBSTR(additionalNotes, :start1Based, :len) FROM projects WHERE id = :projectId")
    suspend fun getNotesChunk(projectId: Long, start1Based: Int, len: Int): String?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectByIdOnce(projectId: Long): ProjectEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE id = :projectId)")
    suspend fun existsById(projectId: Long): Boolean

    @Query("SELECT id FROM projects WHERE LOWER(title) = LOWER(:title) AND projectDateEpochDay = :dateEpoch AND ((clientId IS NULL AND :clientId IS NULL) OR clientId = :clientId) LIMIT 1")
    suspend fun findProjectIdByNaturalKey(title: String, dateEpoch: Long, clientId: Long?): Long?

    @Query("SELECT * FROM projects WHERE clientId = :clientId ORDER BY projectDateEpochDay DESC, id DESC LIMIT :limit")
    fun observeProjectsForClient(clientId: Long, limit: Int = 50): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects WHERE clientId = :clientId")
    suspend fun countProjectsForClient(clientId: Long): Int

    @Query("SELECT * FROM projects WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getProjectsChunkAfterId(lastId: Long, limit: Int): List<ProjectEntity>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun countAllProjects(): Long

    @Query(
        """
        SELECT p.id, p.title, p.clientId, c.name AS clientName, p.projectAmount, p.amountPaid
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR COALESCE(c.name, '') LIKE '%' || :query || '%')
        ORDER BY p.projectDateEpochDay DESC, p.id DESC
        LIMIT :limit
        """
    )
    suspend fun searchProjectPickerOptions(query: String, clientId: Long?, limit: Int = 50): List<ProjectPickerOption>

    @Query(
        """
        SELECT p.id, c.name AS clientName, p.category, p.title, p.projectType, p.priority,
               p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay, p.projectAmount,
               p.amountPaid, p.paymentStatus, p.projectStatus, p.clientRating,
               p.clientFeedback, p.additionalNotes
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE p.id > :lastId
        ORDER BY p.id ASC
        LIMIT :limit
        """
    )
    suspend fun getProjectExportChunkAfterId(lastId: Long, limit: Int): List<ProjectExportRow>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE p.projectStatus IN ('PENDING', 'IN_PROGRESS')
        ORDER BY p.projectDateEpochDay DESC, p.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentActiveProjectSummaries(limit: Int = 5): Flow<List<ProjectSummaryRow>>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        ORDER BY p.projectDateEpochDay DESC, p.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentProjectSummaries(limit: Int = 50): Flow<List<ProjectSummaryRow>>

    @Query(
        """
        SELECT * FROM projects
        WHERE projectStatus NOT IN ('COMPLETED', 'CANCELLED')
          AND deadlineEpochDay IS NOT NULL
          AND deadlineEpochDay <= :soonEpoch
        ORDER BY deadlineEpochDay ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getDueOrOverdueProjectsOnce(soonEpoch: Long, limit: Int = 25): List<ProjectEntity>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:category IS NULL OR p.category = :category)
          AND (:projectType IS NULL OR p.projectType = :projectType)
          AND (:priority IS NULL OR p.priority = :priority)
          AND (:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus)
          AND (:projectStatus IS NULL OR p.projectStatus = :projectStatus)
          AND (:exactDate IS NULL OR p.projectDateEpochDay = :exactDate)
          AND (:deadlineBefore IS NULL OR (p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay <= :deadlineBefore))
          AND (:onlyOverdue = 0 OR (p.projectStatus NOT IN ('COMPLETED', 'CANCELLED') AND p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay < :todayEpoch))
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR p.projectType LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        ORDER BY p.projectDateEpochDay DESC, p.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryProjectsDateDesc(
        query: String,
        clientId: Long?,
        category: String?,
        projectType: String?,
        priority: String?,
        paymentStatus: String?,
        projectStatus: String?,
        exactDate: Long?,
        deadlineBefore: Long?,
        onlyOverdue: Boolean,
        todayEpoch: Long,
        limit: Int,
        offset: Int
    ): List<ProjectSummaryRow>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:category IS NULL OR p.category = :category)
          AND (:projectType IS NULL OR p.projectType = :projectType)
          AND (:priority IS NULL OR p.priority = :priority)
          AND (:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus)
          AND (:projectStatus IS NULL OR p.projectStatus = :projectStatus)
          AND (:exactDate IS NULL OR p.projectDateEpochDay = :exactDate)
          AND (:deadlineBefore IS NULL OR (p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay <= :deadlineBefore))
          AND (:onlyOverdue = 0 OR (p.projectStatus NOT IN ('COMPLETED', 'CANCELLED') AND p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay < :todayEpoch))
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR p.projectType LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        ORDER BY COALESCE(p.deadlineEpochDay, 9223372036854775807) ASC, p.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryProjectsDeadlineAsc(
        query: String,
        clientId: Long?,
        category: String?,
        projectType: String?,
        priority: String?,
        paymentStatus: String?,
        projectStatus: String?,
        exactDate: Long?,
        deadlineBefore: Long?,
        onlyOverdue: Boolean,
        todayEpoch: Long,
        limit: Int,
        offset: Int
    ): List<ProjectSummaryRow>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:category IS NULL OR p.category = :category)
          AND (:projectType IS NULL OR p.projectType = :projectType)
          AND (:priority IS NULL OR p.priority = :priority)
          AND (:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus)
          AND (:projectStatus IS NULL OR p.projectStatus = :projectStatus)
          AND (:exactDate IS NULL OR p.projectDateEpochDay = :exactDate)
          AND (:deadlineBefore IS NULL OR (p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay <= :deadlineBefore))
          AND (:onlyOverdue = 0 OR (p.projectStatus NOT IN ('COMPLETED', 'CANCELLED') AND p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay < :todayEpoch))
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR p.projectType LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        ORDER BY p.projectAmount DESC, p.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryProjectsAmountDesc(
        query: String,
        clientId: Long?,
        category: String?,
        projectType: String?,
        priority: String?,
        paymentStatus: String?,
        projectStatus: String?,
        exactDate: Long?,
        deadlineBefore: Long?,
        onlyOverdue: Boolean,
        todayEpoch: Long,
        limit: Int,
        offset: Int
    ): List<ProjectSummaryRow>

    @Query(
        """
        SELECT p.id, p.clientId, c.name AS clientName, p.category, p.title, p.projectType,
               p.priority, p.videoDuration, p.projectDateEpochDay, p.deadlineEpochDay,
               p.projectAmount, p.amountPaid, p.paymentStatus, p.projectStatus,
               p.clientRating, p.createdAtMillis, p.updatedAtMillis
        FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:category IS NULL OR p.category = :category)
          AND (:projectType IS NULL OR p.projectType = :projectType)
          AND (:priority IS NULL OR p.priority = :priority)
          AND (:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus)
          AND (:projectStatus IS NULL OR p.projectStatus = :projectStatus)
          AND (:exactDate IS NULL OR p.projectDateEpochDay = :exactDate)
          AND (:deadlineBefore IS NULL OR (p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay <= :deadlineBefore))
          AND (:onlyOverdue = 0 OR (p.projectStatus NOT IN ('COMPLETED', 'CANCELLED') AND p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay < :todayEpoch))
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR p.projectType LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        ORDER BY CASE p.priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'NORMAL' THEN 2 ELSE 1 END DESC, p.projectDateEpochDay DESC, p.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryProjectsPriorityDesc(
        query: String,
        clientId: Long?,
        category: String?,
        projectType: String?,
        priority: String?,
        paymentStatus: String?,
        projectStatus: String?,
        exactDate: Long?,
        deadlineBefore: Long?,
        onlyOverdue: Boolean,
        todayEpoch: Long,
        limit: Int,
        offset: Int
    ): List<ProjectSummaryRow>

    @Query(
        """
        SELECT COUNT(*) FROM projects p
        LEFT JOIN clients c ON c.id = p.clientId
        WHERE (:clientId IS NULL OR p.clientId = :clientId)
          AND (:category IS NULL OR p.category = :category)
          AND (:projectType IS NULL OR p.projectType = :projectType)
          AND (:priority IS NULL OR p.priority = :priority)
          AND (:paymentStatus IS NULL OR p.paymentStatus = :paymentStatus)
          AND (:projectStatus IS NULL OR p.projectStatus = :projectStatus)
          AND (:exactDate IS NULL OR p.projectDateEpochDay = :exactDate)
          AND (:deadlineBefore IS NULL OR (p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay <= :deadlineBefore))
          AND (:onlyOverdue = 0 OR (p.projectStatus NOT IN ('COMPLETED', 'CANCELLED') AND p.deadlineEpochDay IS NOT NULL AND p.deadlineEpochDay < :todayEpoch))
          AND (:query = '' OR p.title LIKE '%' || :query || '%' OR p.projectType LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        """
    )
    suspend fun countFilteredProjects(
        query: String,
        clientId: Long?,
        category: String?,
        projectType: String?,
        priority: String?,
        paymentStatus: String?,
        projectStatus: String?,
        exactDate: Long?,
        deadlineBefore: Long?,
        onlyOverdue: Boolean,
        todayEpoch: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM projects
        WHERE projectStatus = 'COMPLETED'
          AND (:category IS NULL OR category = :category)
          AND projectDateEpochDay BETWEEN :startEpoch AND :endEpoch
        """
    )
    suspend fun countCompletedProjectsInRange(category: String?, startEpoch: Long, endEpoch: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET amountPaid = :amountPaid, paymentStatus = :paymentStatus, updatedAtMillis = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectPayment(projectId: Long, amountPaid: Double, paymentStatus: String, updatedAt: Long)

    @Query(
        """
        UPDATE projects SET
            amountPaid = COALESCE((SELECT SUM(amount) FROM income_transactions WHERE projectId = projects.id), 0.0),
            paymentStatus = CASE
                WHEN COALESCE((SELECT SUM(amount) FROM income_transactions WHERE projectId = projects.id), 0.0) <= 0.0 THEN 'UNPAID'
                WHEN projectAmount <= 0.0 THEN 'PAID'
                WHEN COALESCE((SELECT SUM(amount) FROM income_transactions WHERE projectId = projects.id), 0.0) + 0.001 >= projectAmount THEN 'PAID'
                ELSE 'PARTIAL'
            END,
            updatedAtMillis = :updatedAt
        """
    )
    suspend fun recalculateAllProjectPaymentsBulk(updatedAt: Long)

    @Query("UPDATE projects SET clientId = NULL, updatedAtMillis = :updatedAt WHERE clientId = :clientId")
    suspend fun unlinkProjectsFromClient(clientId: Long, updatedAt: Long)

    @Query("DELETE FROM projects WHERE clientId = :clientId")
    suspend fun deleteProjectsByClientId(clientId: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface CalendarDayDao {
    @Query("SELECT * FROM calendar_days ORDER BY dateEpochDay ASC")
    fun observeAllCalendarDays(): Flow<List<CalendarDayEntity>>

    @Query("SELECT * FROM calendar_days WHERE dateEpochDay BETWEEN :startEpoch AND :endEpoch ORDER BY dateEpochDay ASC")
    fun observeCalendarDaysInRange(startEpoch: Long, endEpoch: Long): Flow<List<CalendarDayEntity>>

    @Query("SELECT * FROM calendar_days WHERE dateEpochDay > :lastEpoch ORDER BY dateEpochDay ASC LIMIT :limit")
    suspend fun getCalendarDaysChunkAfterEpoch(lastEpoch: Long, limit: Int): List<CalendarDayEntity>

    @Query("SELECT COUNT(*) FROM calendar_days")
    suspend fun countAllCalendarDays(): Long

    @Query("SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay BETWEEN :startEpoch AND :endEpoch AND status = :status")
    suspend fun countStatusInRange(startEpoch: Long, endEpoch: Long, status: String): Int

    @Query("SELECT * FROM calendar_days WHERE dateEpochDay = :epochDay")
    suspend fun getDayOnce(epochDay: Long): CalendarDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarDay(day: CalendarDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCalendarDays(days: List<CalendarDayEntity>)

    @Query("DELETE FROM calendar_days WHERE dateEpochDay = :epochDay")
    suspend fun deleteCalendarDay(epochDay: Long)

    @Query("DELETE FROM calendar_days")
    suspend fun deleteAllCalendarDays()
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_transactions ORDER BY dateEpochDay DESC, id DESC LIMIT 100")
    fun observeAllIncome(): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE projectId = :projectId ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeIncomeForProject(projectId: Long, limit: Int = 50): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE clientId = :clientId ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeIncomeForClient(clientId: Long, limit: Int = 50): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE id = :incomeId")
    suspend fun getIncomeByIdOnce(incomeId: Long): IncomeTransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM income_transactions WHERE id = :incomeId)")
    suspend fun existsById(incomeId: Long): Boolean

    @Query("SELECT id FROM income_transactions WHERE LOWER(source) = LOWER(:source) AND dateEpochDay = :dateEpoch AND ABS(amount - :amount) < 0.001 AND ((projectId IS NULL AND :projectId IS NULL) OR projectId = :projectId) LIMIT 1")
    suspend fun findIncomeIdByNaturalKey(source: String, dateEpoch: Long, amount: Double, projectId: Long?): Long?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM income_transactions WHERE projectId = :projectId")
    suspend fun sumPaidForProject(projectId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM income_transactions WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch) AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch)")
    suspend fun sumIncomeInRange(startEpoch: Long?, endEpoch: Long?): Double

    @Query("SELECT COUNT(*) FROM income_transactions WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch) AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch) AND (:query = '' OR source LIKE '%' || :query || '%' OR clientNameSnapshot LIKE '%' || :query || '%' OR projectTitleSnapshot LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')")
    suspend fun countFilteredIncome(query: String, startEpoch: Long?, endEpoch: Long?): Int

    @Query(
        """
        SELECT id, source, clientId, projectId, clientNameSnapshot, projectTitleSnapshot,
               amount, dateEpochDay, paymentMethod, SUBSTR(notes, 1, 160) AS notesPreview, createdAtMillis
        FROM income_transactions
        WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch)
          AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch)
          AND (:query = '' OR source LIKE '%' || :query || '%' OR clientNameSnapshot LIKE '%' || :query || '%' OR projectTitleSnapshot LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY dateEpochDay DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryIncomePage(query: String, startEpoch: Long?, endEpoch: Long?, limit: Int, offset: Int): List<IncomeSummaryRow>

    @Query("SELECT * FROM income_transactions WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getIncomeChunkAfterId(lastId: Long, limit: Int): List<IncomeTransactionEntity>

    @Query(
        """
        SELECT i.id, i.dateEpochDay, i.source,
               COALESCE(c.name, i.clientNameSnapshot) AS clientName,
               COALESCE(p.title, i.projectTitleSnapshot) AS projectTitle,
               i.amount, i.paymentMethod, i.notes
        FROM income_transactions i
        LEFT JOIN clients c ON c.id = i.clientId
        LEFT JOIN projects p ON p.id = i.projectId
        WHERE i.id > :lastId
        ORDER BY i.id ASC
        LIMIT :limit
        """
    )
    suspend fun getIncomeExportChunkAfterId(lastId: Long, limit: Int): List<IncomeExportRow>

    @Query("SELECT COUNT(*) FROM income_transactions")
    suspend fun countAllIncome(): Long

    @Query("SELECT MIN(dateEpochDay) FROM income_transactions")
    suspend fun getMinIncomeEpochDay(): Long?

    @Query("SELECT MAX(dateEpochDay) FROM income_transactions")
    suspend fun getMaxIncomeEpochDay(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIncome(items: List<IncomeTransactionEntity>)

    @Update
    suspend fun updateIncome(income: IncomeTransactionEntity)

    @Query(
        """
        UPDATE income_transactions
        SET clientNameSnapshot = CASE WHEN clientNameSnapshot = '' THEN :clientName ELSE clientNameSnapshot END,
            clientId = NULL
        WHERE clientId = :clientId
        """
    )
    suspend fun unlinkClientFromIncomePreservingSnapshot(clientId: Long, clientName: String)

    @Query(
        """
        UPDATE income_transactions
        SET projectTitleSnapshot = CASE WHEN projectTitleSnapshot = '' THEN :projectTitle ELSE projectTitleSnapshot END,
            projectId = NULL
        WHERE projectId = :projectId
        """
    )
    suspend fun unlinkProjectFromIncomePreservingSnapshot(projectId: Long, projectTitle: String)

    @Query(
        """
        UPDATE income_transactions
        SET projectId = NULL
        WHERE projectId IN (SELECT id FROM projects WHERE clientId = :clientId)
        """
    )
    suspend fun unlinkProjectsForClientFromIncome(clientId: Long)

    @Query("DELETE FROM income_transactions WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: Long)

    @Query("DELETE FROM income_transactions")
    suspend fun deleteAllIncome()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_transactions ORDER BY dateEpochDay DESC, id DESC LIMIT 100")
    fun observeAllExpenses(): Flow<List<ExpenseTransactionEntity>>

    @Query("SELECT * FROM expense_transactions WHERE id = :expenseId")
    suspend fun getExpenseByIdOnce(expenseId: Long): ExpenseTransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM expense_transactions WHERE id = :expenseId)")
    suspend fun existsById(expenseId: Long): Boolean

    @Query("SELECT id FROM expense_transactions WHERE LOWER(description) = LOWER(:description) AND category = :category AND dateEpochDay = :dateEpoch AND ABS(amount - :amount) < 0.001 LIMIT 1")
    suspend fun findExpenseIdByNaturalKey(description: String, category: String, dateEpoch: Long, amount: Double): Long?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expense_transactions WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch) AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch)")
    suspend fun sumExpensesInRange(startEpoch: Long?, endEpoch: Long?): Double

    @Query("SELECT COUNT(*) FROM expense_transactions WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch) AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch) AND (:query = '' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')")
    suspend fun countFilteredExpenses(query: String, startEpoch: Long?, endEpoch: Long?): Int

    @Query(
        """
        SELECT id, category, description, amount, dateEpochDay, SUBSTR(notes, 1, 160) AS notesPreview, createdAtMillis
        FROM expense_transactions
        WHERE (:startEpoch IS NULL OR dateEpochDay >= :startEpoch)
          AND (:endEpoch IS NULL OR dateEpochDay <= :endEpoch)
          AND (:query = '' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY dateEpochDay DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryExpensesPage(query: String, startEpoch: Long?, endEpoch: Long?, limit: Int, offset: Int): List<ExpenseSummaryRow>

    @Query("SELECT * FROM expense_transactions WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getExpensesChunkAfterId(lastId: Long, limit: Int): List<ExpenseTransactionEntity>

    @Query("SELECT COUNT(*) FROM expense_transactions")
    suspend fun countAllExpenses(): Long

    @Query("SELECT MIN(dateEpochDay) FROM expense_transactions")
    suspend fun getMinExpenseEpochDay(): Long?

    @Query("SELECT MAX(dateEpochDay) FROM expense_transactions")
    suspend fun getMaxExpenseEpochDay(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(items: List<ExpenseTransactionEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseTransactionEntity)

    @Query("DELETE FROM expense_transactions WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("DELETE FROM expense_transactions")
    suspend fun deleteAllExpenses()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdOnce(noteId: Long): NoteEntity?

    @Query(
        """
        SELECT id, title, '' AS content, category, clientId, projectId, isPinned, createdAtMillis, updatedAtMillis
        FROM notes WHERE id = :noteId
        """
    )
    suspend fun getNoteHeaderOnce(noteId: Long): NoteEntity?

    @Query("SELECT LENGTH(content) FROM notes WHERE id = :noteId")
    suspend fun getNoteContentLength(noteId: Long): Int?

    @Query("SELECT SUBSTR(content, :start1Based, :len) FROM notes WHERE id = :noteId")
    suspend fun getNoteContentChunk(noteId: Long, start1Based: Int, len: Int): String?

    @Query("SELECT EXISTS(SELECT 1 FROM notes WHERE id = :noteId)")
    suspend fun existsById(noteId: Long): Boolean

    @Query("SELECT id FROM notes WHERE LOWER(title) = LOWER(:title) AND createdAtMillis = :createdAt LIMIT 1")
    suspend fun findNoteIdByNaturalKey(title: String, createdAt: Long): Long?

    @Query(
        """
        SELECT n.id, n.title, SUBSTR(n.content, 1, 200) AS contentPreview, LENGTH(n.content) AS contentLength,
               n.category, n.clientId, c.name AS clientName, n.projectId, p.title AS projectTitle,
               n.isPinned, n.createdAtMillis, n.updatedAtMillis
        FROM notes n
        LEFT JOIN clients c ON c.id = n.clientId
        LEFT JOIN projects p ON p.id = n.projectId
        WHERE (:category IS NULL OR n.category = :category)
          AND (:clientId IS NULL OR n.clientId = :clientId)
          AND (:projectId IS NULL OR n.projectId = :projectId)
          AND (:query = '' OR n.title LIKE '%' || :query || '%' OR n.content LIKE '%' || :query || '%')
        ORDER BY n.isPinned DESC, n.updatedAtMillis DESC, n.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryNotesPage(
        query: String,
        category: String?,
        clientId: Long?,
        projectId: Long?,
        limit: Int,
        offset: Int
    ): List<NoteSummaryRow>

    @Query(
        """
        SELECT COUNT(*) FROM notes n
        WHERE (:category IS NULL OR n.category = :category)
          AND (:clientId IS NULL OR n.clientId = :clientId)
          AND (:projectId IS NULL OR n.projectId = :projectId)
          AND (:query = '' OR n.title LIKE '%' || :query || '%' OR n.content LIKE '%' || :query || '%')
        """
    )
    suspend fun countFilteredNotes(query: String, category: String?, clientId: Long?, projectId: Long?): Int

    @Query("SELECT * FROM notes WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getNotesChunkAfterId(lastId: Long, limit: Int): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAllNotes(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Dao
interface ProjectRevisionDao {
    @Query("SELECT * FROM project_revisions WHERE projectId = :projectId ORDER BY versionNumber DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getRevisionsForProjectPage(projectId: Long, limit: Int, offset: Int): List<ProjectRevisionEntity>

    @Query("SELECT * FROM project_revisions WHERE projectId = :projectId ORDER BY versionNumber DESC, id DESC LIMIT :limit")
    fun observeRevisionsForProject(projectId: Long, limit: Int = 50): Flow<List<ProjectRevisionEntity>>

    @Query("SELECT COUNT(*) FROM project_revisions WHERE projectId = :projectId")
    suspend fun countRevisionsForProject(projectId: Long): Int

    @Query("SELECT COALESCE(MAX(versionNumber), 0) FROM project_revisions WHERE projectId = :projectId")
    suspend fun getMaxVersionNumber(projectId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM project_revisions WHERE id = :revisionId)")
    suspend fun existsById(revisionId: Long): Boolean

    @Query("SELECT id FROM project_revisions WHERE projectId = :projectId AND versionNumber = :versionNumber AND createdAtMillis = :createdAt LIMIT 1")
    suspend fun findRevisionIdByNaturalKey(projectId: Long, versionNumber: Int, createdAt: Long): Long?

    @Query("SELECT * FROM project_revisions WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getRevisionsChunkAfterId(lastId: Long, limit: Int): List<ProjectRevisionEntity>

    @Query("SELECT COUNT(*) FROM project_revisions")
    suspend fun countAllRevisions(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: ProjectRevisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRevisions(revisions: List<ProjectRevisionEntity>)

    @Query("DELETE FROM project_revisions WHERE id = :revisionId")
    suspend fun deleteRevisionById(revisionId: Long)

    @Query("DELETE FROM project_revisions")
    suspend fun deleteAllRevisions()
}

@Dao
interface CareerGoalDao {
    @Query(
        """
        SELECT * FROM career_goals
        WHERE (:status IS NULL OR status = :status)
          AND (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY CASE status WHEN 'ACTIVE' THEN 1 WHEN 'PAUSED' THEN 2 ELSE 3 END ASC, updatedAtMillis DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryGoalsPage(query: String, status: String?, limit: Int, offset: Int): List<CareerGoalEntity>

    @Query(
        """
        SELECT COUNT(*) FROM career_goals
        WHERE (:status IS NULL OR status = :status)
          AND (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        """
    )
    suspend fun countFilteredGoals(query: String, status: String?): Int

    @Query("SELECT EXISTS(SELECT 1 FROM career_goals WHERE id = :goalId)")
    suspend fun existsById(goalId: Long): Boolean

    @Query("SELECT id FROM career_goals WHERE LOWER(title) = LOWER(:title) AND createdAtMillis = :createdAt LIMIT 1")
    suspend fun findGoalIdByNaturalKey(title: String, createdAt: Long): Long?

    @Query("SELECT * FROM career_goals WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getGoalsChunkAfterId(lastId: Long, limit: Int): List<CareerGoalEntity>

    @Query("SELECT COUNT(*) FROM career_goals")
    suspend fun countAllGoals(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: CareerGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(goals: List<CareerGoalEntity>)

    @Update
    suspend fun updateGoal(goal: CareerGoalEntity)

    @Query("DELETE FROM career_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Long)

    @Query("DELETE FROM career_goals")
    suspend fun deleteAllGoals()
}

@Dao
interface FormDraftDao {
    @Query("SELECT * FROM form_drafts WHERE draftKey = :key")
    suspend fun getDraft(key: String): FormDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: FormDraftEntity)

    @Query("DELETE FROM form_drafts WHERE draftKey = :key")
    suspend fun deleteDraft(key: String)

    @Query("DELETE FROM form_drafts")
    suspend fun deleteAllDrafts()
}

@Dao
interface DashboardAggregateDao {
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM clients) AS totalClients,
            (SELECT COUNT(*) FROM projects) AS totalProjects,
            (SELECT COUNT(*) FROM projects WHERE category = 'CLIENT_VIDEO') AS commissionedProjectsCount,
            (SELECT COUNT(*) FROM projects WHERE projectStatus = 'COMPLETED') AS totalVideosCompleted,
            (SELECT COUNT(*) FROM projects WHERE projectStatus = 'COMPLETED' AND projectDateEpochDay BETWEEN :monthStartEpoch AND :monthEndEpoch) AS monthCompletedVideos,
            (SELECT COUNT(*) FROM projects WHERE projectStatus IN ('PENDING', 'IN_PROGRESS')) AS activeProjectsCount,
            (SELECT COUNT(*) FROM projects WHERE projectStatus IN ('PENDING', 'IN_PROGRESS') AND deadlineEpochDay IS NOT NULL AND deadlineEpochDay < :todayEpoch) AS overdueProjectsCount,
            (SELECT COALESCE(SUM(CASE WHEN projectStatus != 'CANCELLED' AND projectAmount > amountPaid THEN projectAmount - amountPaid ELSE 0.0 END), 0.0) FROM projects) AS pendingClientPayments,
            (SELECT COALESCE(SUM(amount), 0.0) FROM income_transactions) AS totalEarnings,
            (SELECT COALESCE(SUM(amount), 0.0) FROM expense_transactions) AS totalExpenses,
            (SELECT COALESCE(SUM(amount), 0.0) FROM income_transactions WHERE dateEpochDay BETWEEN :monthStartEpoch AND :monthEndEpoch) AS monthEarnings,
            (SELECT COALESCE(SUM(amount), 0.0) FROM expense_transactions WHERE dateEpochDay BETWEEN :monthStartEpoch AND :monthEndEpoch) AS monthExpenses,
            (SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay <= :todayEpoch AND status = 'DONE') AS totalEditingDays,
            (SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay <= :todayEpoch AND status = 'MISSED') AS totalMissedDays,
            (SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay BETWEEN :startEpoch AND :todayEpoch AND status IN ('DONE', 'MISSED')) AS markedSinceStart,
            (SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay <= :todayEpoch AND dateEpochDay BETWEEN :monthStartEpoch AND :monthEndEpoch AND status = 'DONE') AS monthDoneDays,
            (SELECT COUNT(*) FROM calendar_days WHERE dateEpochDay <= :todayEpoch AND dateEpochDay BETWEEN :monthStartEpoch AND :monthEndEpoch AND status = 'MISSED') AS monthMissedDays
        """
    )
    fun observeHomeMetrics(
        todayEpoch: Long,
        monthStartEpoch: Long,
        monthEndEpoch: Long,
        startEpoch: Long
    ): Flow<HomeMetricsAggregate>

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM clients) AS clients,
            (SELECT COUNT(*) FROM projects) AS projects,
            (SELECT COUNT(*) FROM income_transactions) AS incomeTransactions,
            (SELECT COUNT(*) FROM expense_transactions) AS expenseTransactions,
            (SELECT COUNT(*) FROM calendar_days) AS calendarDays,
            (SELECT COUNT(*) FROM notes) AS notes,
            (SELECT COUNT(*) FROM project_revisions) AS revisions,
            (SELECT COUNT(*) FROM career_goals) AS careerGoals
        """
    )
    suspend fun getStorageRecordCounts(): StorageRecordCounts
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
