package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditorProfileDao {
    @Query("SELECT * FROM editor_profile WHERE id = 1")
    fun observeProfile(): Flow<EditorProfileEntity?>

    @Query("SELECT * FROM editor_profile WHERE id = 1")
    suspend fun getProfileOnce(): EditorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: EditorProfileEntity)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY updatedAtMillis DESC")
    fun observeAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun observeClientById(clientId: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    suspend fun getClientByIdOnce(clientId: Long): ClientEntity?

    @Query("SELECT * FROM clients")
    suspend fun getAllClientsOnce(): List<ClientEntity>

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
    @Query("SELECT * FROM projects ORDER BY projectDateEpochDay DESC, id DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectById(projectId: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectByIdOnce(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE clientId = :clientId ORDER BY projectDateEpochDay DESC, id DESC")
    fun observeProjectsForClient(clientId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsOnce(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET amountPaid = :amountPaid, paymentStatus = :paymentStatus, updatedAtMillis = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectPayment(projectId: Long, amountPaid: Double, paymentStatus: String, updatedAt: Long)

    @Query("UPDATE projects SET clientId = NULL, updatedAtMillis = :updatedAt WHERE clientId = :clientId")
    suspend fun unlinkProjectsFromClient(clientId: Long, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface CalendarDayDao {
    @Query("SELECT * FROM calendar_days ORDER BY dateEpochDay ASC")
    fun observeAllCalendarDays(): Flow<List<CalendarDayEntity>>

    @Query("SELECT * FROM calendar_days")
    suspend fun getAllCalendarDaysOnce(): List<CalendarDayEntity>

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
    @Query("SELECT * FROM income_transactions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAllIncome(): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE projectId = :projectId ORDER BY dateEpochDay DESC, id DESC")
    fun observeIncomeForProject(projectId: Long): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE clientId = :clientId ORDER BY dateEpochDay DESC, id DESC")
    fun observeIncomeForClient(clientId: Long): Flow<List<IncomeTransactionEntity>>

    @Query("SELECT * FROM income_transactions WHERE id = :incomeId")
    suspend fun getIncomeByIdOnce(incomeId: Long): IncomeTransactionEntity?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM income_transactions WHERE projectId = :projectId")
    suspend fun sumPaidForProject(projectId: Long): Double

    @Query("SELECT * FROM income_transactions")
    suspend fun getAllIncomeOnce(): List<IncomeTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIncome(items: List<IncomeTransactionEntity>)

    @Update
    suspend fun updateIncome(income: IncomeTransactionEntity)

    @Query("UPDATE income_transactions SET clientId = NULL WHERE clientId = :clientId")
    suspend fun unlinkClientFromIncome(clientId: Long)

    @Query("UPDATE income_transactions SET projectId = NULL WHERE projectId = :projectId")
    suspend fun unlinkProjectFromIncome(projectId: Long)

    @Query("DELETE FROM income_transactions WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: Long)

    @Query("DELETE FROM income_transactions")
    suspend fun deleteAllIncome()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_transactions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAllExpenses(): Flow<List<ExpenseTransactionEntity>>

    @Query("SELECT * FROM expense_transactions WHERE id = :expenseId")
    suspend fun getExpenseByIdOnce(expenseId: Long): ExpenseTransactionEntity?

    @Query("SELECT * FROM expense_transactions")
    suspend fun getAllExpensesOnce(): List<ExpenseTransactionEntity>

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
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
