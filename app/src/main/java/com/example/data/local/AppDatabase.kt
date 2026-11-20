package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.dao.CalendarDayDao
import com.example.data.local.dao.ClientDao
import com.example.data.local.dao.EditorProfileDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.IncomeDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CalendarDayEntity
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.EditorProfileEntity
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.util.DEFAULT_JOURNEY_START_DATE

@Database(
    entities = [
        EditorProfileEntity::class,
        ClientEntity::class,
        ProjectEntity::class,
        CalendarDayEntity::class,
        IncomeTransactionEntity::class,
        ExpenseTransactionEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun editorProfileDao(): EditorProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun calendarDayDao(): CalendarDayDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val DB_NAME = "rohan_edits_career.db"

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addCallback(SeedCallback())
                .build()
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .addCallback(SeedCallback())
                .build()
        }

        private class SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val startEpochDay = DEFAULT_JOURNEY_START_DATE.toEpochDay()
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO editor_profile (
                        id, name, brandName, profession, officialStartDateEpochDay,
                        professionalExperience, skills, editingSoftware, specializations,
                        portfolioLink, personalNotes, updatedAtMillis
                    ) VALUES (
                        1, 'Rohan', 'Rohan Edits', 'Video Editor & Motion Graphics Designer',
                        $startEpochDay, '', '', '', '', '', '', $now
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO app_settings (
                        id, themeMode, dateFormat, currencySymbol, confirmBeforeDelete, deadlineRemindersEnabled
                    ) VALUES (
                        1, 'LIGHT', 'DD_MMM_YYYY', '₹', 1, 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
