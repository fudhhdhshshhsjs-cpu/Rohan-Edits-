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
import com.example.data.local.entity.ClientEntity
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.PlatformType
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.state.MainUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ClientFormScreen(
    clientId: Long?,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existing = remember(clientId, uiState.clientsWithStats) {
        clientId?.let { id -> uiState.clientsWithStats.firstOrNull { it.client.id == id }?.client }
    }
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val currency = uiState.settings.currencySymbol

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var platform by remember(existing) { mutableStateOf(PlatformType.fromName(existing?.platform)) }
    var clientType by remember(existing) { mutableStateOf(ClientType.fromName(existing?.clientType)) }
    var mobileNumber by remember(existing) { mutableStateOf(existing?.mobileNumber.orEmpty()) }
    var firstProjectDate by remember(existing) { mutableStateOf(existing?.firstProjectDateEpochDay) }
    var instructions by remember(existing) { mutableStateOf(existing?.instructions.orEmpty()) }
    var preferredStyle by remember(existing) { mutableStateOf(existing?.preferredEditingStyle.orEmpty()) }
    var perVideoPrice by remember(existing) {
        mutableStateOf(
            existing?.perVideoPrice?.takeIf { it > 0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
        )
    }
    var notes by remember(existing) { mutableStateOf(existing?.additionalNotes.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
            .testTag("client_form_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        DropdownSelectorField(
            label = "Platform",
            selectedLabel = platform.label,
            options = PlatformType.entries.map { it to it.label },
            onOptionSelected = { platform = it },
            testTag = "client_platform_selector"
        )

        DropdownSelectorField(
            label = "Client Type",
            selectedLabel = clientType.label,
            options = ClientType.entries.map { it to it.label },
            onOptionSelected = { clientType = it },
            testTag = "client_type_selector"
        )

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

        DatePickerField(
            label = "First Project Date",
            epochDay = firstProjectDate,
            dateFormat = dateFormat,
            allowClear = true,
            onDateSelected = { firstProjectDate = it },
            testTag = "client_first_date_input"
        )

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

        OutlinedTextField(
            value = preferredStyle,
            onValueChange = { preferredStyle = it },
            label = { Text("Preferred Editing Style") },
            placeholder = { Text("e.g. Fast-paced SaaS motion, clean captions") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("client_style_input")
        )

        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("Client Instructions") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("client_instructions_input")
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional Notes") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("client_notes_input")
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
