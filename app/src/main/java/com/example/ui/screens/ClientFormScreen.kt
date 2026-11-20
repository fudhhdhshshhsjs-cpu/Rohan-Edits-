package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.local.ClientFormDraft
import com.example.data.local.entity.ClientEntity
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.PlatformType
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ClientFormScreen(
    clientId: Long?,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var existing by remember(clientId) { mutableStateOf<ClientEntity?>(null) }
    var isLoadingExisting by remember(clientId) { mutableStateOf(clientId != null) }

    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val currency = uiState.settings.currencySymbol

    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(PlatformType.YOUTUBE) }
    var clientType by remember { mutableStateOf(ClientType.NEW) }
    var mobileNumber by remember { mutableStateOf("") }
    var firstProjectDate by remember { mutableStateOf<Long?>(null) }
    var instructions by remember { mutableStateOf("") }
    var preferredStyle by remember { mutableStateOf("") }
    var perVideoPrice by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var draftLoaded by remember { mutableStateOf(false) }
    var restoredFromDraft by remember { mutableStateOf(false) }

    // Load full existing record (safe for >2MB instructions/notes) then hydrate draft if present
    LaunchedEffect(clientId) {
        val loaded = clientId?.let { viewModel.getClientFullSafe(it) }
        existing = loaded
        if (loaded != null) {
            name = loaded.name
            platform = PlatformType.fromName(loaded.platform)
            clientType = ClientType.fromName(loaded.clientType)
            mobileNumber = loaded.mobileNumber
            firstProjectDate = loaded.firstProjectDateEpochDay
            instructions = loaded.instructions
            preferredStyle = loaded.preferredEditingStyle
            perVideoPrice = loaded.perVideoPrice.takeIf { it > 0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
            notes = loaded.additionalNotes
        }
        isLoadingExisting = false

        val draft = viewModel.getClientDraft(clientId)
        if (draft != null && (draft.name.isNotBlank() || draft.instructions.isNotBlank() || draft.additionalNotes.isNotBlank())) {
            name = draft.name
            if (draft.platform.isNotBlank()) platform = PlatformType.fromName(draft.platform)
            if (draft.clientType.isNotBlank()) clientType = ClientType.fromName(draft.clientType)
            mobileNumber = draft.mobileNumber
            firstProjectDate = draft.firstProjectDateEpochDay
            instructions = draft.instructions
            preferredStyle = draft.preferredEditingStyle
            perVideoPrice = draft.perVideoPrice
            notes = draft.additionalNotes
            restoredFromDraft = true
        }
        draftLoaded = true
    }

    // Debounced auto-save draft to SQLite so large entries are never lost
    LaunchedEffect(
        draftLoaded, name, platform, clientType, mobileNumber,
        firstProjectDate, instructions, preferredStyle, perVideoPrice, notes
    ) {
        if (!draftLoaded) return@LaunchedEffect
        delay(500)
        val hasInput = name.isNotBlank() || instructions.isNotBlank() || notes.isNotBlank() ||
            preferredStyle.isNotBlank() || mobileNumber.isNotBlank() || perVideoPrice.isNotBlank()
        if (hasInput) {
            viewModel.saveClientDraft(
                ClientFormDraft(
                    targetClientId = clientId,
                    name = name,
                    platform = platform.name,
                    clientType = clientType.name,
                    mobileNumber = mobileNumber,
                    firstProjectDateEpochDay = firstProjectDate,
                    instructions = instructions,
                    preferredEditingStyle = preferredStyle,
                    perVideoPrice = perVideoPrice,
                    additionalNotes = notes
                )
            )
        }
    }

    if (isLoadingExisting) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("client_form_screen"),
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
                            viewModel.clearClientDraft(clientId)
                            name = existing?.name.orEmpty()
                            platform = PlatformType.fromName(existing?.platform)
                            clientType = ClientType.fromName(existing?.clientType)
                            mobileNumber = existing?.mobileNumber.orEmpty()
                            firstProjectDate = existing?.firstProjectDateEpochDay
                            instructions = existing?.instructions.orEmpty()
                            preferredStyle = existing?.preferredEditingStyle.orEmpty()
                            perVideoPrice = existing?.perVideoPrice?.takeIf { it > 0 }?.let {
                                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                            }.orEmpty()
                            notes = existing?.additionalNotes.orEmpty()
                            restoredFromDraft = false
                        }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }

        item(key = "name") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Client Name *") },
                singleLine = true,
                isError = errorMessage != null && name.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_name_input")
            )
        }

        item(key = "platform") {
            DropdownSelectorField(
                label = "Platform",
                selectedLabel = platform.label,
                options = PlatformType.entries.map { it to it.label },
                onOptionSelected = { platform = it },
                testTag = "client_platform_selector"
            )
        }

        item(key = "client_type") {
            DropdownSelectorField(
                label = "Client Type",
                selectedLabel = clientType.label,
                options = ClientType.entries.map { it to it.label },
                onOptionSelected = { clientType = it },
                testTag = "client_type_selector"
            )
        }

        item(key = "mobile") {
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_mobile_input")
            )
        }

        item(key = "first_date") {
            DatePickerField(
                label = "First Project Date",
                epochDay = firstProjectDate,
                dateFormat = dateFormat,
                allowClear = true,
                onDateSelected = { firstProjectDate = it },
                testTag = "client_first_date_input"
            )
        }

        item(key = "price") {
            OutlinedTextField(
                value = perVideoPrice,
                onValueChange = { perVideoPrice = it; errorMessage = null },
                label = { Text("Per Video Price ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_price_input")
            )
        }

        item(key = "style") {
            OutlinedTextField(
                value = preferredStyle,
                onValueChange = { preferredStyle = it },
                label = { Text("Preferred Editing Style") },
                placeholder = { Text("e.g. Fast-paced SaaS motion, clean captions") },
                minLines = 1,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_style_input")
            )
        }

        item(key = "instructions") {
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Client Instructions") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_instructions_input")
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
                    .testTag("client_notes_input")
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
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Client name is required."
                        return@Button
                    }
                    val parsedPrice = if (perVideoPrice.isBlank()) 0.0 else perVideoPrice.toDoubleOrNull()
                    if (parsedPrice == null || parsedPrice < 0.0) {
                        errorMessage = "Per video price cannot be negative or invalid."
                        return@Button
                    }
                    val entity = ClientEntity(
                        id = existing?.id ?: 0L,
                        name = trimmedName,
                        platform = platform.name,
                        clientType = clientType.name,
                        mobileNumber = mobileNumber.trim(),
                        firstProjectDateEpochDay = firstProjectDate,
                        instructions = instructions.trim(),
                        preferredEditingStyle = preferredStyle.trim(),
                        perVideoPrice = parsedPrice,
                        additionalNotes = notes.trim(),
                        createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                    )
                    viewModel.saveClient(entity) {
                        viewModel.clearClientDraft(clientId)
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("client_save_button")
            ) {
                Text(if (existing == null) "Save Client" else "Update Client")
            }
        }
    }
}
