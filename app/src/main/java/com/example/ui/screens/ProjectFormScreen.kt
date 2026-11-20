package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.example.ui.state.MainUiState
import com.example.ui.viewmodel.MainViewModel

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
            .testTag("project_form_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        DropdownSelectorField(
            label = if (category == VideoCategory.CLIENT_VIDEO) "Client *" else "Client (Optional)",
            selectedLabel = selectedClientLabel,
            options = clientOptions,
            onOptionSelected = { cid ->
                selectedClientId = cid
                errorMessage = null
                // Pre-fill client's per-video price if amount is empty on new project
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

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; errorMessage = null },
            label = { Text("Project / Video Title *") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project_title_input")
        )

        DropdownSelectorField(
            label = "Project Type",
            selectedLabel = projectType.label,
            options = ProjectType.entries.map { it to it.label },
            onOptionSelected = { projectType = it },
            testTag = "project_type_selector"
        )

        DropdownSelectorField(
            label = "Priority",
            selectedLabel = priority.label,
            options = ProjectPriority.entries.map { it to it.label },
            onOptionSelected = { priority = it },
            testTag = "project_priority_selector"
        )

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

        DatePickerField(
            label = "Project Date",
            epochDay = projectDate,
            dateFormat = dateFormat,
            onDateSelected = { if (it != null) projectDate = it },
            testTag = "project_date_input"
        )

        DatePickerField(
            label = "Deadline (Optional)",
            epochDay = deadline,
            dateFormat = dateFormat,
            allowClear = true,
            onDateSelected = { deadline = it },
            testTag = "project_deadline_input"
        )

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

        DropdownSelectorField(
            label = "Project Status",
            selectedLabel = projectStatus.label,
            options = ProjectStatus.entries.map { it to it.label },
            onOptionSelected = { projectStatus = it },
            testTag = "project_status_selector"
        )

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

        OutlinedTextField(
            value = feedback,
            onValueChange = { feedback = it },
            label = { Text("Client Review Feedback") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project_feedback_input")
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional Notes") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project_notes_input")
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

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
