package com.example

import android.app.Application
import android.content.Context
import com.example.data.backup.BackupExportManager
import com.example.data.local.AppDatabase
import com.example.data.local.DraftManager
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.CareerRepository
import com.example.util.DateProvider
import com.example.util.SystemDateProvider

class AppContainer(
    context: Context,
    val dateProvider: DateProvider = SystemDateProvider,
    useInMemoryDb: Boolean = false
) {
    val database: AppDatabase = if (useInMemoryDb) {
        AppDatabase.createInMemory(context)
    } else {
        AppDatabase.create(context)
    }
    val preferencesDataStore: PreferencesDataStore = PreferencesDataStore(context)
    val draftManager: DraftManager = DraftManager(context.applicationContext)
    val repository: CareerRepository = CareerRepository(database, preferencesDataStore)
    val backupExportManager: BackupExportManager = BackupExportManager()
}

class RohanEditsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    fun overrideContainerForTesting(testContainer: AppContainer) {
        container = testContainer
    }
}
