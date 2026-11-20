package com.example.data.local

import android.content.Context
import com.example.data.local.dao.FormDraftDao
import com.example.data.local.entity.FormDraftEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

@JsonClass(generateAdapter = true)
data class NoteFormDraft(
    val targetNoteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val clientId: Long? = null,
    val projectId: Long? = null,
    val isPinned: Boolean = false
)

/**
 * Persists in-progress form drafts cleanly on disk (via SQLite form_drafts table when provided,
 * or individual atomic files under filesDir/drafts) so arbitrarily large text entries never bloat
 * memory or a single Preferences DataStore file, and never exceed Navigation/Bundle limits.
 */
class DraftManager(
    private val context: Context,
    private val formDraftDao: FormDraftDao? = null
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val clientAdapter = moshi.adapter(ClientFormDraft::class.java)
    private val projectAdapter = moshi.adapter(ProjectFormDraft::class.java)
    private val noteAdapter = moshi.adapter(NoteFormDraft::class.java)

    private val draftsDir: File by lazy {
        File(context.applicationContext.filesDir, "form_drafts").apply { mkdirs() }
    }

    private fun clientKey(clientId: Long?) = "draft_client_${clientId ?: "new"}"
    private fun projectKey(projectId: Long?) = "draft_project_${projectId ?: "new"}"
    private fun noteKey(noteId: Long?) = "draft_note_${noteId ?: "new"}"

    private suspend fun writeRawDraft(key: String, json: String) = withContext(Dispatchers.IO) {
        if (formDraftDao != null) {
            formDraftDao.saveDraft(FormDraftEntity(draftKey = key, payloadJson = json, updatedAtMillis = System.currentTimeMillis()))
        } else {
            val tmp = File(draftsDir, "$key.json.tmp")
            val target = File(draftsDir, "$key.json")
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private suspend fun readRawDraft(key: String): String? = withContext(Dispatchers.IO) {
        if (formDraftDao != null) {
            formDraftDao.getDraft(key)?.payloadJson
        } else {
            val target = File(draftsDir, "$key.json")
            if (target.exists()) target.readText(Charsets.UTF_8) else null
        }
    }

    private suspend fun deleteRawDraft(key: String) = withContext(Dispatchers.IO) {
        if (formDraftDao != null) {
            formDraftDao.deleteDraft(key)
        }
        File(draftsDir, "$key.json").delete()
        File(draftsDir, "$key.json.tmp").delete()
    }

    suspend fun clearAllDrafts() = withContext(Dispatchers.IO) {
        runCatching { formDraftDao?.deleteAllDrafts() }
        runCatching {
            if (draftsDir.exists()) {
                draftsDir.listFiles()?.forEach { it.delete() }
            }
        }
        Unit
    }

    suspend fun saveClientDraft(draft: ClientFormDraft) {
        runCatching { writeRawDraft(clientKey(draft.targetClientId), clientAdapter.toJson(draft)) }
    }

    suspend fun getClientDraft(clientId: Long?): ClientFormDraft? =
        runCatching {
            readRawDraft(clientKey(clientId))?.let { clientAdapter.fromJson(it) }
        }.getOrNull()

    suspend fun clearClientDraft(clientId: Long?) {
        runCatching { deleteRawDraft(clientKey(clientId)) }
    }

    suspend fun saveProjectDraft(draft: ProjectFormDraft) {
        runCatching { writeRawDraft(projectKey(draft.targetProjectId), projectAdapter.toJson(draft)) }
    }

    suspend fun getProjectDraft(projectId: Long?): ProjectFormDraft? =
        runCatching {
            readRawDraft(projectKey(projectId))?.let { projectAdapter.fromJson(it) }
        }.getOrNull()

    suspend fun clearProjectDraft(projectId: Long?) {
        runCatching { deleteRawDraft(projectKey(projectId)) }
    }

    suspend fun saveNoteDraft(draft: NoteFormDraft) {
        runCatching { writeRawDraft(noteKey(draft.targetNoteId), noteAdapter.toJson(draft)) }
    }

    suspend fun getNoteDraft(noteId: Long?): NoteFormDraft? =
        runCatching {
            readRawDraft(noteKey(noteId))?.let { noteAdapter.fromJson(it) }
        }.getOrNull()

    suspend fun clearNoteDraft(noteId: Long?) {
        runCatching { deleteRawDraft(noteKey(noteId)) }
    }
}
