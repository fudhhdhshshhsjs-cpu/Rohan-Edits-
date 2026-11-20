package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectFormDraft
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.VideoCategory
import com.example.data.repository.CareerRepository
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ProjectFormScreen(
    projectId: Long?,
    preselectedClientId: Long?,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existing = remember(projectId, uiState.projectsWithClient) {
        projectId?.let { id -> uiState.projectsWithClient.firstOrNull { it.project.id == id }?.project }
    }
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val currency = uiState.settings.currencySymbol

    var category by remember(existing) {
        mutableStateOf(
            if (existing != null) VideoCategory.fromName(existing.category)
            else VideoCategory.CLIENT_VIDEO
        )
    }
    var selectedClientId by remember(existing, preselectedClientId) {
        mutableStateOf(existing?.clientId ?: preselectedClientId)
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var projectType by remember(existing) { mutableStateOf(ProjectType.fromName(existing?.projectType)) }
    var priority by remember(existing) { mutableStateOf(ProjectPriority.fromName(existing?.priority)) }
    var duration by remember(existing) { mutableStateOf(existing?.videoDuration.orEmpty()) }
    var projectDate by remember(existing) {
        mutableStateOf(existing?.projectDateEpochDay ?: uiState.today.toEpochDay())
    }
    var deadline by remember(existing) { mutableStateOf(existing?.deadlineEpochDay) }
    var amountText by remember(existing) {
        mutableStateOf(
            existing?.projectAmount?.takeIf { it > 0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
        )
    }
    var projectStatus by remember(existing) { mutableStateOf(ProjectStatus.fromName(existing?.projectStatus)) }
    var ratingText by remember(existing) { mutableStateOf(existing?.clientRating?.toString().orEmpty()) }
    var feedback by remember(existing) { mutableStateOf(existing?.clientFeedback.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.additionalNotes.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var draftLoaded by remember { mutableStateOf(false) }
    var restoredFromDraft by remember { mutableStateOf(false) }

    LaunchedEffect(projectId, existing) {
        val draft = viewModel.getProjectDraft(projectId)
        if (draft != null && (draft.title.isNotBlank() || draft.feedback.isNotBlank() || draft.notes.isNotBlank())) {
            if (draft.category.isNotBlank()) category = VideoCategory.fromName(draft.category)
            if (preselectedClientId == null) selectedClientId = draft.clientId
            title = draft.title
            if (draft.projectType.isNotBlank()) projectType = ProjectType.fromName(draft.projectType)
            if (draft.priority.isNotBlank()) priority = ProjectPriority.fromName(draft.priority)
            duration = draft.duration
            if (draft.projectDateEpochDay > 0L) projectDate = draft.projectDateEpochDay
            deadline = draft.deadlineEpochDay
            amountText = draft.amount
            if (draft.projectStatus.isNotBlank()) projectStatus = ProjectStatus.fromName(draft.projectStatus)
            ratingText = draft.rating
            feedback = draft.feedback
            notes = draft.notes
            restoredFromDraft = true
        }
        draftLoaded = true
    }

    LaunchedEffect(
        draftLoaded, category, selectedClientId, title, projectType, priority,
        duration, projectDate, deadline, amountText, projectStatus, ratingText, feedback, notes
    ) {
        if (!draftLoaded) return@LaunchedEffect
        delay(500)
        val hasInput = title.isNotBlank() || feedback.isNotBlank() || notes.isNotBlank() ||
            amountText.isNotBlank() || duration.isNotBlank()
        if (hasInput) {
            viewModel.saveProjectDraft(
                ProjectFormDraft(
                    targetProjectId = projectId,
                    category = category.name,
                    clientId = selectedClientId,
                    title = title,
                    projectType = projectType.name,
                    priority = priority.name,
                    duration = duration,
                    projectDateEpochDay = projectDate,
                    deadlineEpochDay = deadline,
                    amount = amountText,
                    projectStatus = projectStatus.name,
                    rating = ratingText,
                    feedback = feedback,
                    notes = notes
                )
            )
        }
    }

    val clientOptions = remember(uiState.clientsWithStats, category) {
        val base = uiState.clientsWithStats.map { it.client.id to it.client.name }
        if (category == VideoCategory.CLIENT_VIDEO) {
            base
        } else {
            listOf<Pair<Long?, String>>(null to "No Client (Personal / Standalone)") + base
        }
    }

    val selectedClientLabel = clientOptions.firstOrNull { it.first == selectedClientId }?.second
        ?: if (category == VideoCategory.CLIENT_VIDEO) "Select Client (Required)" else "No Client"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("project_form_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (restoredFromDraft) {
            item(key = "draft_banner") {
                StudioCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unsaved draft restored automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.clearProjectDraft(projectId)
                            category = if (existing != null) VideoCategory.fromName(existing.category) else VideoCategory.CLIENT_VIDEO
                            selectedClientId = existing?.clientId ?: preselectedClientId
                            title = existing?.title.orEmpty()
                            projectType = ProjectType.fromName(existing?.projectType)
                            priority = ProjectPriority.fromName(existing?.priority)
                            duration = existing?.videoDuration.orEmpty()
                            projectDate = existing?.projectDateEpochDay ?: uiState.today.toEpochDay()
                            deadline = existing?.deadlineEpochDay
                            amountText = existing?.projectAmount?.takeIf { it > 0 }?.let {
                                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                            }.orEmpty()
                            projectStatus = ProjectStatus.fromName(existing?.projectStatus)
                            ratingText = existing?.clientRating?.toString().orEmpty()
                            feedback = existing?.clientFeedback.orEmpty()
                            notes = existing?.additionalNotes.orEmpty()
                            restoredFromDraft = false
                        }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }

        item(key = "category") {
            DropdownSelectorField(
                label = "Video Category / Purpose",
                selectedLabel = category.label,
                options = VideoCategory.entries.map { it to it.label },
                onOptionSelected = {
                    category = it
                    errorMessage = null
                },
                testTag = "project_category_selector"
            )
        }

        item(key = "client") {
            DropdownSelectorField(
                label = if (category == VideoCategory.CLIENT_VIDEO) "Client *" else "Client (Optional)",
                selectedLabel = selectedClientLabel,
                options = clientOptions,
                onOptionSelected = { cid ->
                    selectedClientId = cid
                    errorMessage = null
                    if (existing == null && amountText.isBlank() && cid != null) {
                        val c = uiState.clientsWithStats.firstOrNull { it.client.id == cid }?.client
                        if (c != null && c.perVideoPrice > 0) {
                            amountText = if (c.perVideoPrice % 1.0 == 0.0) {
                                c.perVideoPrice.toInt().toString()
                            } else {
                                c.perVideoPrice.toString()
                            }
                        }
                    }
                },
                testTag = "project_client_selector"
            )
            if (category == VideoCategory.CLIENT_VIDEO && uiState.clientsWithStats.isEmpty()) {
                Text(
                    text = "Tip: Add a client first to create a commissioned Client Video, or choose Personal/Portfolio/Practice Video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item(key = "title") {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = { Text("Project / Video Title *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_title_input")
            )
        }

        item(key = "type") {
            DropdownSelectorField(
                label = "Project Type",
                selectedLabel = projectType.label,
                options = ProjectType.entries.map { it to it.label },
                onOptionSelected = { projectType = it },
                testTag = "project_type_selector"
            )
        }

        item(key = "priority") {
            DropdownSelectorField(
                label = "Priority",
                selectedLabel = priority.label,
                options = ProjectPriority.entries.map { it to it.label },
                onOptionSelected = { priority = it },
                testTag = "project_priority_selector"
            )
        }

        item(key = "duration") {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Video Duration") },
                placeholder = { Text("e.g. 00:45 or 8m 20s") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_duration_input")
            )
        }

        item(key = "project_date") {
            DatePickerField(
                label = "Project Date",
                epochDay = projectDate,
                dateFormat = dateFormat,
                onDateSelected = { if (it != null) projectDate = it },
                testTag = "project_date_input"
            )
        }

        item(key = "deadline") {
            DatePickerField(
                label = "Deadline (Optional)",
                epochDay = deadline,
                dateFormat = dateFormat,
                allowClear = true,
                onDateSelected = { deadline = it },
                testTag = "project_deadline_input"
            )
        }

        item(key = "amount") {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; errorMessage = null },
                label = { Text("Project Amount ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                supportingText = {
                    val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                    val paid = existing?.amountPaid ?: 0.0
                    val derivedStatus = CareerRepository.computePaymentStatus(parsedAmt, paid)
                    Text("Payment Status (${derivedStatus.label}) updates automatically from recorded payments in Budget.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_amount_input")
            )
        }

        item(key = "status") {
            DropdownSelectorField(
                label = "Project Status",
                selectedLabel = projectStatus.label,
                options = ProjectStatus.entries.map { it to it.label },
                onOptionSelected = { projectStatus = it },
                testTag = "project_status_selector"
            )
        }

        item(key = "rating") {
            OutlinedTextField(
                value = ratingText,
                onValueChange = { ratingText = it; errorMessage = null },
                label = { Text("Client Review Rating (1-10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_rating_input")
            )
        }

        item(key = "feedback") {
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Client Review Feedback") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_feedback_input")
            )
        }

        item(key = "notes") {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_notes_input")
            )
        }

        if (errorMessage != null) {
            item(key = "error") {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item(key = "save") {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        errorMessage = "Project title is required."
                        return@Button
                    }
                    if (category == VideoCategory.CLIENT_VIDEO && selectedClientId == null) {
                        errorMessage = "Please select an existing client for a commissioned Client Video."
                        return@Button
                    }
                    val parsedAmount = if (amountText.isBlank()) 0.0 else amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount < 0.0) {
                        errorMessage = "Project amount cannot be negative or invalid."
                        return@Button
                    }
                    if (deadline != null && deadline!! < projectDate) {
                        errorMessage = "Deadline cannot be earlier than the project date."
                        return@Button
                    }
                    val parsedRating = if (ratingText.isBlank()) null else ratingText.toIntOrNull()
                    if (ratingText.isNotBlank() && (parsedRating == null || parsedRating !in 1..10)) {
                        errorMessage = "Rating must be a number between 1 and 10."
                        return@Button
                    }

                    val entity = ProjectEntity(
                        id = existing?.id ?: 0L,
                        clientId = selectedClientId,
                        category = category.name,
                        title = trimmedTitle,
                        projectType = projectType.name,
                        priority = priority.name,
                        videoDuration = duration.trim(),
                        projectDateEpochDay = projectDate,
                        deadlineEpochDay = deadline,
                        projectAmount = parsedAmount,
                        amountPaid = existing?.amountPaid ?: 0.0,
                        paymentStatus = existing?.paymentStatus ?: PaymentStatus.UNPAID.name,
                        projectStatus = projectStatus.name,
                        clientRating = parsedRating,
                        clientFeedback = feedback.trim(),
                        additionalNotes = notes.trim(),
                        createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                    )
                    viewModel.saveProject(entity) {
                        viewModel.clearProjectDraft(projectId)
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("project_save_button")
            ) {
                Text(if (existing == null) "Save Project" else "Update Project")
            }
        }
    }
}
