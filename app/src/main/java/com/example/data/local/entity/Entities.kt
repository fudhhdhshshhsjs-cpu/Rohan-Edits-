package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CalendarStatus
import com.example.data.model.DateFormatOption
import com.example.data.model.ThemeMode
import com.example.util.DEFAULT_JOURNEY_START_DATE

@Entity(tableName = "editor_profile")
data class EditorProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Rohan",
    val brandName: String = "Rohan Edits",
    val profession: String = "Video Editor & Motion Graphics Designer",
    val officialStartDateEpochDay: Long = DEFAULT_JOURNEY_START_DATE.toEpochDay(),
    val professionalExperience: String = "",
    val skills: String = "",
    val editingSoftware: String = "",
    val specializations: String = "",
    val portfolioLink: String = "",
    val personalNotes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["name"]),
        Index(value = ["clientType"]),
        Index(value = ["updatedAtMillis"])
    ]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val platform: String,
    val clientType: String,
    val mobileNumber: String = "",
    val firstProjectDateEpochDay: Long? = null,
    val instructions: String = "",
    val preferredEditingStyle: String = "",
    val perVideoPrice: Double = 0.0,
    val additionalNotes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["clientId"]),
        Index(value = ["projectStatus"]),
        Index(value = ["paymentStatus"]),
        Index(value = ["projectDateEpochDay"]),
        Index(value = ["deadlineEpochDay"])
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_days")
data class CalendarDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val status: String = CalendarStatus.NOT_MARKED.name,
    val note: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "income_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["clientId"]),
        Index(value = ["projectId"]),
        Index(value = ["dateEpochDay"])
    ]
)
data class IncomeTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val clientId: Long? = null,
    val projectId: Long? = null,
    val clientNameSnapshot: String = "",
    val projectTitleSnapshot: String = "",
    val amount: Double,
    val dateEpochDay: Long,
    val paymentMethod: String,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expense_transactions",
    indices = [
        Index(value = ["category"]),
        Index(value = ["dateEpochDay"])
    ]
)
data class ExpenseTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val dateEpochDay: Long,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = ThemeMode.DARK.name,
    val dateFormat: String = DateFormatOption.DD_MMM_YYYY.name,
    val currencySymbol: String = "₹",
    val confirmBeforeDelete: Boolean = true,
    val deadlineRemindersEnabled: Boolean = false
)
