package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NoteFormDraft
import com.example.data.local.dao.NoteSummaryRow
import com.example.data.local.entity.NoteEntity
import com.example.data.model.DateFormatOption
import com.example.data.model.NoteCategory
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaginationFooter
import com.example.ui.components.PickerItem
import com.example.ui.components.SearchableEntityPickerField
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notesState by viewModel.notesListState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    var searchText by remember(notesState.query) { mutableStateOf(notesState.query) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<NoteSummaryRow?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "notes_search_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.updateNotesFilter(query = it)
                    },
                    placeholder = { Text("Search notes & briefs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.updateNotesFilter(query = "")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_search_input")
                )
                Button(
                    onClick = {
                        editingNote = null
                        showNoteDialog = true
                    },
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("notes_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        item(key = "notes_category_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = notesState.category == null,
                    onClick = { viewModel.updateNotesFilter(category = null) },
                    label = { Text("All") }
                )
                NoteCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = notesState.category == cat.name,
                        onClick = {
                            val next = if (notesState.category == cat.name) null else cat.name
                            viewModel.updateNotesFilter(category = next)
                        },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        if (notesState.items.isEmpty() && !notesState.isLoading) {
            item(key = "notes_empty") {
                EmptyStateCard(
                    icon = Icons.Default.Description,
                    title = "No notes saved",
                    subtitle = "Save long project briefs, creative hooks, scripts, and client notes with automatic draft recovery.",
                    actionLabel = "Create Note",
                    actionTag = "notes_empty_add",
                    onActionClick = {
                        editingNote = null
                        showNoteDialog = true
                    }
                )
            }
        } else {
            itemsIndexed(
                items = notesState.items,
                key = { _, note -> note.id },
                contentType = { _, _ -> "note_card" }
            ) { index, note ->
                if (index >= notesState.items.lastIndex - 5 && notesState.hasMore && !notesState.isLoading) {
                    LaunchedEffect(notesState.items.size) {
                        viewModel.loadMoreNotes()
                    }
                }
                StudioCard(
                    onClick = {
                        scope.launch {
                            val full = viewModel.getNoteFullSafe(note.id)
                            if (full != null) {
                                editingNote = full
                                showNoteDialog = true
                            }
                        }
                    },
                    modifier = Modifier.testTag("note_card_${note.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(
                                    text = NoteCategory.fromName(note.category).label,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (!note.clientName.isNullOrBlank()) {
                                    Text(
                                        text = note.clientName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (note.contentPreview.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.contentPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = buildString {
                                    append(Formatters.formatTimestamp(note.updatedAtMillis, dateFormat))
                                    append(" · ${note.contentLength} chars")
                                    if (!note.projectTitle.isNullOrBlank()) append(" · ${note.projectTitle}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { viewModel.toggleNotePin(note.id) }) {
                                Icon(
                                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val full = viewModel.getNoteFullSafe(note.id)
                                        if (full != null) {
                                            editingNote = full
                                            showNoteDialog = true
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit note", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (uiState.settings.confirmBeforeDelete) {
                                        pendingDeleteNote = note
                                    } else {
                                        viewModel.deleteNote(note.id)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete note",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "notes_pagination") {
            PaginationFooter(
                shownCount = notesState.items.size,
                totalCount = notesState.totalCount,
                hasMore = notesState.hasMore,
                isLoading = notesState.isLoading,
                itemLabel = "notes",
                onLoadMore = { viewModel.loadMoreNotes() }
            )
        }
    }

    if (showNoteDialog) {
        NoteEditorDialog(
            existing = editingNote,
            viewModel = viewModel,
            onSave = { entity ->
                viewModel.saveNote(entity) {
                    viewModel.clearNoteDraft(editingNote?.id)
                    showNoteDialog = false
                }
            },
            onDismiss = { showNoteDialog = false }
        )
    }

    pendingDeleteNote?.let { n ->
        ConfirmDeleteDialog(
            title = "Delete Note?",
            message = "Delete '${n.title}' (${n.contentLength} characters)?",
            onConfirm = {
                viewModel.deleteNote(n.id)
                pendingDeleteNote = null
            },
            onDismiss = { pendingDeleteNote = null }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    existing: NoteEntity?,
    viewModel: MainViewModel,
    onSave: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var content by remember(existing) { mutableStateOf(existing?.content.orEmpty()) }
    var category by remember(existing) { mutableStateOf(NoteCategory.fromName(existing?.category)) }
    var selectedClientId by remember(existing) { mutableStateOf(existing?.clientId) }
    var selectedClientLabel by remember(existing) { mutableStateOf("None") }
    var selectedProjectId by remember(existing) { mutableStateOf(existing?.projectId) }
    var selectedProjectLabel by remember(existing) { mutableStateOf("None") }
    var isPinned by remember(existing) { mutableStateOf(existing?.isPinned ?: false) }
    var draftLoaded by remember(existing) { mutableStateOf(false) }
    var restoredFromDraft by remember(existing) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing?.id) {
        val cid = existing?.clientId
        if (cid != null) selectedClientLabel = viewModel.getClientFullSafe(cid)?.name ?: "Client #$cid"
        val pid = existing?.projectId
        if (pid != null) selectedProjectLabel = viewModel.getProjectFullSafe(pid)?.title ?: "Project #$pid"

        val draft = viewModel.getNoteDraft(existing?.id)
        if (draft != null && (draft.title.isNotBlank() || draft.content.isNotBlank())) {
            title = draft.title
            content = draft.content
            if (draft.category.isNotBlank()) category = NoteCategory.fromName(draft.category)
            selectedClientId = draft.clientId
            selectedProjectId = draft.projectId
            restoredFromDraft = true
        }
        draftLoaded = true
    }

    LaunchedEffect(draftLoaded, title, content, category, selectedClientId, selectedProjectId) {
        if (!draftLoaded) return@LaunchedEffect
        delay(500)
        if (title.isNotBlank() || content.isNotBlank()) {
            viewModel.saveNoteDraft(
                NoteFormDraft(
                    targetNoteId = existing?.id,
                    title = title,
                    content = content,
                    category = category.name,
                    clientId = selectedClientId,
                    projectId = selectedProjectId
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Note" else "Edit Note") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (restoredFromDraft) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Draft restored",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            viewModel.clearNoteDraft(existing?.id)
                            title = existing?.title.orEmpty()
                            content = existing?.content.orEmpty()
                            category = NoteCategory.fromName(existing?.category)
                            selectedClientId = existing?.clientId
                            selectedProjectId = existing?.projectId
                            restoredFromDraft = false
                        }) {
                            Text("Discard")
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Note Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
                DropdownSelectorField(
                    label = "Category",
                    selectedLabel = category.label,
                    options = NoteCategory.entries.map { it to it.label },
                    onOptionSelected = { category = it }
                )
                SearchableEntityPickerField(
                    label = "Linked Client (Optional)",
                    selectedLabel = if (selectedClientId == null) "None" else selectedClientLabel,
                    noneOptionLabel = "None",
                    onSelectNone = {
                        selectedClientId = null
                        selectedClientLabel = "None"
                    },
                    onSearch = { q ->
                        viewModel.searchClientsForPicker(q).map {
                            PickerItem(it.id, it.name, it.platform)
                        }
                    },
                    onItemSelected = { picked ->
                        selectedClientId = picked.value
                        selectedClientLabel = picked.title
                    }
                )
                SearchableEntityPickerField(
                    label = "Linked Project (Optional)",
                    selectedLabel = if (selectedProjectId == null) "None" else selectedProjectLabel,
                    noneOptionLabel = "None",
                    onSelectNone = {
                        selectedProjectId = null
                        selectedProjectLabel = "None"
                    },
                    onSearch = { q ->
                        viewModel.searchProjectsForPicker(q, selectedClientId).map {
                            PickerItem(it.id, it.title, it.clientName)
                        }
                    },
                    onItemSelected = { picked ->
                        selectedProjectId = picked.value
                        selectedProjectLabel = picked.title
                    }
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content (${content.length} chars)") },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "Note title is required."
                        return@Button
                    }
                    onSave(
                        NoteEntity(
                            id = existing?.id ?: 0L,
                            title = title.trim(),
                            content = content,
                            category = category.name,
                            clientId = selectedClientId,
                            projectId = selectedProjectId,
                            isPinned = isPinned,
                            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("note_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
