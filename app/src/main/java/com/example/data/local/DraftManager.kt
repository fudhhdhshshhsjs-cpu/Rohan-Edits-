package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.draftDataStore: DataStore<Preferences> by preferencesDataStore(name = "rohan_edits_drafts")

@JsonClass(generateAdapter = true)
data class ClientFormDraft(
    val targetClientId: Long? = null,
    val name: String = "",
    val platform: String = "",
    val clientType: String = "",
    val mobileNumber: String = "",
    val firstProjectDateEpochDay: Long? = null,
    val instructions: String = "",
    val preferredEditingStyle: String = "",
    val perVideoPrice: String = "",
    val additionalNotes: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectFormDraft(
    val targetProjectId: Long? = null,
    val category: String = "",
    val clientId: Long? = null,
    val title: String = "",
    val projectType: String = "",
    val priority: String = "",
    val duration: String = "",
    val projectDateEpochDay: Long = 0L,
    val deadlineEpochDay: Long? = null,
    val amount: String = "",
    val projectStatus: String = "",
    val rating: String = "",
    val feedback: String = "",
    val notes: String = ""
)

/**
 * Persists in-progress form drafts cleanly on disk (via DataStore) so large text entries
 * are never lost on navigation, screen rotation, or unexpected app closure, without passing
 * oversized payloads through Bundle/Navigation arguments.
 */
class DraftManager(private val context: Context) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val clientAdapter = moshi.adapter(ClientFormDraft::class.java)
    private val projectAdapter = moshi.adapter(ProjectFormDraft::class.java)

    private fun clientKey(clientId: Long?) =
        stringPreferencesKey("draft_client_${clientId ?: "new"}")

    private fun projectKey(projectId: Long?) =
        stringPreferencesKey("draft_project_${projectId ?: "new"}")

    suspend fun saveClientDraft(draft: ClientFormDraft) = withContext(Dispatchers.IO) {
        runCatching {
            val json = clientAdapter.toJson(draft)
            context.draftDataStore.edit { prefs ->
                prefs[clientKey(draft.targetClientId)] = json
            }
        }
        Unit
    }

    suspend fun getClientDraft(clientId: Long?): ClientFormDraft? = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.draftDataStore.data.first()[clientKey(clientId)] ?: return@runCatching null
            clientAdapter.fromJson(json)
        }.getOrNull()
    }

    suspend fun clearClientDraft(clientId: Long?) = withContext(Dispatchers.IO) {
        runCatching {
            context.draftDataStore.edit { prefs ->
                prefs.remove(clientKey(clientId))
            }
        }
        Unit
    }

    suspend fun saveProjectDraft(draft: ProjectFormDraft) = withContext(Dispatchers.IO) {
        runCatching {
            val json = projectAdapter.toJson(draft)
            context.draftDataStore.edit { prefs ->
                prefs[projectKey(draft.targetProjectId)] = json
            }
        }
        Unit
    }

    suspend fun getProjectDraft(projectId: Long?): ProjectFormDraft? = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.draftDataStore.data.first()[projectKey(projectId)] ?: return@runCatching null
            projectAdapter.fromJson(json)
        }.getOrNull()
    }

    suspend fun clearProjectDraft(projectId: Long?) = withContext(Dispatchers.IO) {
        runCatching {
            context.draftDataStore.edit { prefs ->
                prefs.remove(projectKey(projectId))
            }
        }
        Unit
    }
}
