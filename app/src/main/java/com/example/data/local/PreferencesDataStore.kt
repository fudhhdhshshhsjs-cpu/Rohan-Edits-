package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.model.DateFormatOption
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rohan_edits_prefs")

class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        val DEADLINE_REMINDERS_ENABLED = booleanPreferencesKey("deadline_reminders_enabled")
    }

    val settingsFlow: Flow<AppSettingsEntity> = context.settingsDataStore.data.map { prefs ->
        AppSettingsEntity(
            id = 1,
            themeMode = prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.name,
            dateFormat = prefs[Keys.DATE_FORMAT] ?: DateFormatOption.DD_MMM_YYYY.name,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            confirmBeforeDelete = prefs[Keys.CONFIRM_BEFORE_DELETE] ?: true,
            deadlineRemindersEnabled = prefs[Keys.DEADLINE_REMINDERS_ENABLED] ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode
            prefs[Keys.DATE_FORMAT] = settings.dateFormat
            prefs[Keys.CURRENCY_SYMBOL] = settings.currencySymbol
            prefs[Keys.CONFIRM_BEFORE_DELETE] = settings.confirmBeforeDelete
            prefs[Keys.DEADLINE_REMINDERS_ENABLED] = settings.deadlineRemindersEnabled
        }
    }
}
