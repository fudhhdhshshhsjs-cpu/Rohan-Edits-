package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PlatformType
import com.example.data.model.ProjectStatus
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandableInfoRow
import com.example.ui.components.MetricTile
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.PickerItem
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SearchableEntityPickerField
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.ClientDetailState
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@Composable
fun ClientDetailScreen(
    clientId: Long,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onEditClient: (Long) -> Unit,
    onAddProjectForClient: (Long) -> Unit,
    onViewAllClientProjects: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var detailState by remember(clientId) { mutableStateOf(ClientDetailState(isLoading = true)) }

    LaunchedEffect(
        clientId,
        refreshTrigger,
        uiState.home.totalProjects,
        uiState.home.totalEarnings,
        uiState.home.pendingClientPayments
    ) {
        detailState = viewModel.loadClientDetail(clientId)
    }

    if (detailState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("client_detail_screen"),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val client = detailState.client
    if (client == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Client not found.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onNavigateBack) { Text("Back") }
        }
        return
    }

    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val clientProjects = detailState.projects
    val clientPayments = detailState.payments

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("client_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = PlatformType.fromName(client.platform).label,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            StatusPill(
                                text = ClientType.fromName(client.clientType).label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (client.perVideoPrice > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatCurrency(client.perVideoPrice, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("per video", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditClient(clientId) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("client_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("client_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }

        // Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Total Projects",
                        value = detailState.totalProjects.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Completed Videos",
                        value = detailState.completedVideos.toString(),
                        accentColor = StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Earnings Received",
                        value = Formatters.formatCurrency(detailState.totalEarningsReceived, currency),
                        accentColor = StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Pending Payments",
                        value = Formatters.formatCurrency(detailState.pendingPayments, currency),
                        accentColor = StatusAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAddProjectForClient(clientId) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("client_add_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Project")
                }
                FilledTonalButton(
                    onClick = { showRecordPaymentDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("client_record_payment_button")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Payment")
                }
            }
        }

        // Client Information
        item(key = "client_info", contentType = "info") {
            StudioCard {
                Text("Client Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                ExpandableInfoRow("Mobile", client.mobileNumber.ifBlank { "Not provided" })
                ExpandableInfoRow(
                    "First Project",
                    client.firstProjectDateEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: "Not set"
                )
                ExpandableInfoRow("Editing Style", client.preferredEditingStyle.ifBlank { "Not specified" })
                ExpandableInfoRow("Instructions", client.instructions.ifBlank { "None" })
                ExpandableInfoRow("Notes", client.additionalNotes.ifBlank { "None" })
            }
        }

        // Project History
        item {
            SectionHeader(
                title = "Project History (${detailState.totalProjects})",
                actionLabel = if (detailState.totalProjects > 0) "View All" else null,
                onActionClick = { onViewAllClientProjects(clientId) }
            )
        }

        if (clientProjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.FolderCopy,
                    title = "No projects for ${client.name}",
                    subtitle = "Add a commissioned project to track deadlines and payments."
                )
            }
        } else {
            items(clientProjects, key = { it.project.id }, contentType = { "project" }) { item ->
                val p = item.project
                StudioCard(onClick = { onOpenProject(p.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.formatDate(p.projectDateEpochDay, dateFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ProjectStatusBadge(ProjectStatus.fromName(p.projectStatus), item.isOverdue)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaymentStatusBadge(PaymentStatus.fromName(p.paymentStatus))
                        Text(
                            text = "${Formatters.formatCurrency(p.amountPaid, currency)} / ${Formatters.formatCurrency(p.projectAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
            }
        }

        // Recent Payments
        item(key = "payments_header", contentType = "header") {
            SectionHeader(title = "Recent Payments (${clientPayments.size})")
        }
        if (clientPayments.isEmpty()) {
            item(key = "payments_empty", contentType = "empty") {
                StudioCard {
                    Text(
                        text = "No payments recorded for this client yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(clientPayments, key = { it.id }, contentType = { "payment" }) { tx ->
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.source,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.formatDate(tx.dateEpochDay, dateFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+${Formatters.formatCurrency(tx.amount, currency)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = StatusGreen
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        var alsoDeleteProjects by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${client.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Financial income records are ALWAYS preserved safely so your ledger totals stay accurate. Linked projects will be unlinked by default.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (detailState.totalProjects > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = alsoDeleteProjects,
                                onCheckedChange = { alsoDeleteProjects = it }
                            )
                            Text(
                                text = "Also delete ${detailState.totalProjects} associated project(s) (income history remains intact)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteClient(clientId, deleteAssociatedProjects = alsoDeleteProjects) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_client")
                ) {
                    Text("Delete Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRecordPaymentDialog) {
        val unpaidOrPartial = clientProjects.filter {
            ProjectStatus.fromName(it.project.projectStatus) != ProjectStatus.CANCELLED && it.remainingAmount > 0.0
        }
        var selectedProjectId by remember { mutableStateOf(unpaidOrPartial.firstOrNull()?.project?.id) }
        var selectedProjectLabel by remember {
            mutableStateOf(
                unpaidOrPartial.firstOrNull()?.let {
                    "${it.project.title} (Due ${Formatters.formatCurrency(it.remainingAmount, currency)})"
                } ?: "General Client Income (No Project)"
            )
        }
        var amountText by remember {
            mutableStateOf(unpaidOrPartial.firstOrNull()?.remainingAmount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "")
        }
        var dateEpoch by remember { mutableStateOf(uiState.today.toEpochDay()) }
        var method by remember { mutableStateOf(PaymentMethod.UPI) }
        var notes by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRecordPaymentDialog = false },
            title = { Text("Record Client Payment") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (detailState.totalProjects > 0) {
                        SearchableEntityPickerField(
                            label = "Link to Project",
                            selectedLabel = if (selectedProjectId == null) "General Client Income (No Project)" else selectedProjectLabel,
                            noneOptionLabel = "General Client Income (No Project)",
                            onSelectNone = {
                                selectedProjectId = null
                                selectedProjectLabel = "General Client Income (No Project)"
                            },
                            onSearch = { q ->
                                viewModel.searchProjectsForPicker(q, clientId).map { opt ->
                                    val rem = (opt.projectAmount - opt.amountPaid).coerceAtLeast(0.0)
                                    PickerItem(
                                        value = opt,
                                        title = opt.title,
                                        subtitle = "Due ${Formatters.formatCurrency(rem, currency)}"
                                    )
                                }
                            },
                            onItemSelected = { picked ->
                                val opt = picked.value
                                val rem = (opt.projectAmount - opt.amountPaid).coerceAtLeast(0.0)
                                selectedProjectId = opt.id
                                selectedProjectLabel = "${opt.title} (Due ${Formatters.formatCurrency(rem, currency)})"
                                if (rem > 0.0) {
                                    amountText = if (rem % 1.0 == 0.0) rem.toInt().toString() else rem.toString()
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; error = null },
                        label = { Text("Amount ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DatePickerField(
                        label = "Payment Date",
                        epochDay = dateEpoch,
                        dateFormat = dateFormat,
                        onDateSelected = { if (it != null) dateEpoch = it }
                    )
                    DropdownSelectorField(
                        label = "Payment Method",
                        selectedLabel = method.label,
                        options = PaymentMethod.entries.map { it to it.label },
                        onOptionSelected = { method = it }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            error = "Enter a valid positive amount."
                            return@Button
                        }
                        val pid = selectedProjectId
                        if (pid != null) {
                            viewModel.recordProjectPayment(pid, amt, dateEpoch, method, notes) {
                                showRecordPaymentDialog = false
                                refreshTrigger++
                            }
                        } else {
                            viewModel.saveIncome(
                                com.example.data.local.entity.IncomeTransactionEntity(
                                    source = "Client Payment: ${client.name}",
                                    clientId = clientId,
                                    clientNameSnapshot = client.name,
                                    amount = amt,
                                    dateEpochDay = dateEpoch,
                                    paymentMethod = method.name,
                                    notes = notes
                                )
                            ) {
                                showRecordPaymentDialog = false
                                refreshTrigger++
                            }
                        }
                    }
                ) { Text("Record") }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    ExpandableInfoRow(label = label, value = value)
}
