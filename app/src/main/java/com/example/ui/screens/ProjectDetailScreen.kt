package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.VideoCategory
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onEditProject: (Long) -> Unit,
    onOpenClient: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = uiState.projectsWithClient.firstOrNull { it.project.id == projectId }
    if (item == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Project not found.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onNavigateBack) { Text("Back") }
        }
        return
    }

    val p = item.project
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val linkedPayments = remember(uiState.incomeTransactions, projectId) {
        uiState.incomeTransactions.filter { it.projectId == projectId }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRecordPayment by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("project_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overview Header
        item {
            StudioCard {
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${VideoCategory.fromName(p.category).label} · ${ProjectType.fromName(p.projectType).label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.clientName.isNullOrBlank() && p.clientId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onOpenClient(p.clientId) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Client: ${item.clientName}")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectStatusBadge(ProjectStatus.fromName(p.projectStatus), item.isOverdue)
                    PriorityBadge(ProjectPriority.fromName(p.priority))
                    PaymentStatusBadge(PaymentStatus.fromName(p.paymentStatus))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditProject(projectId) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = {
                            if (uiState.settings.confirmBeforeDelete) {
                                showDeleteConfirm = true
                            } else {
                                viewModel.deleteProject(projectId) { onNavigateBack() }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }

        // Financial Payment Card (Ledger-backed, zero double-counting)
        item {
            StudioCard {
                Text(
                    text = "Payment Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Project Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(p.projectAmount, currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold)
                        )
                    }
                    Column {
                        Text("Amount Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(p.amountPaid, currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                            color = StatusGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(item.remainingAmount, currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                            color = if (item.remainingAmount > 0) StatusAmber else StatusGreen
                        )
                    }
                }

                if (p.projectAmount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = (p.amountPaid / p.projectAmount).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showRecordPayment = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_record_payment_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Payment")
                    }
                    if (ProjectStatus.fromName(p.projectStatus) != ProjectStatus.COMPLETED) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.saveProject(p.copy(projectStatus = ProjectStatus.COMPLETED.name))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("project_mark_completed_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete")
                        }
                    }
                }
            }
        }

        // Metadata & Feedback
        item {
            StudioCard {
                Text("Project Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailInfoRow("Project Date", Formatters.formatDate(p.projectDateEpochDay, dateFormat))
                DetailInfoRow(
                    "Deadline",
                    p.deadlineEpochDay?.let { Formatters.formatDate(it, dateFormat) } ?: "No deadline"
                )
                DetailInfoRow("Video Duration", p.videoDuration.ifBlank { "Not specified" })
                DetailInfoRow("Client Rating", p.clientRating?.let { "$it / 10" } ?: "Not rated")
                DetailInfoRow("Client Feedback", p.clientFeedback.ifBlank { "None" })
                DetailInfoRow("Notes", p.additionalNotes.ifBlank { "None" })
            }
        }

        // Linked Payment History
        item {
            SectionHeader(title = "Payment History (${linkedPayments.size})")
        }
        if (linkedPayments.isEmpty()) {
            item {
                StudioCard {
                    Text(
                        text = "No payments recorded for this project yet. Marking a project completed does not add income until payment is recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(linkedPayments, key = { it.id }) { tx ->
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.formatDate(tx.dateEpochDay, dateFormat),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${PaymentMethod.fromName(tx.paymentMethod).label}${if (tx.notes.isNotBlank()) " · ${tx.notes}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${Formatters.formatCurrency(tx.amount, currency)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = StatusGreen
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete ${p.title}?",
            message = "This removes the project record. Existing income transactions will be safely preserved in your financial ledger.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteProject(projectId) { onNavigateBack() }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showRecordPayment) {
        var amountText by remember {
            mutableStateOf(
                if (item.remainingAmount > 0.0) {
                    if (item.remainingAmount % 1.0 == 0.0) item.remainingAmount.toInt().toString()
                    else item.remainingAmount.toString()
                } else ""
            )
        }
        var dateEpoch by remember { mutableStateOf(uiState.today.toEpochDay()) }
        var method by remember { mutableStateOf(PaymentMethod.UPI) }
        var notes by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRecordPayment = false },
            title = { Text("Record Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.remainingAmount > 0) {
                        Text(
                            text = "Remaining balance: ${Formatters.formatCurrency(item.remainingAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; error = null },
                        label = { Text("Amount Received ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_payment_amount_input")
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
                        label = { Text("Notes (e.g. Advance / Final)") },
                        singleLine = true,
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
                            error = "Amount must be greater than zero."
                            return@Button
                        }
                        viewModel.recordProjectPayment(projectId, amt, dateEpoch, method, notes) {
                            showRecordPayment = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_record_payment_button")
                ) { Text("Save Payment") }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPayment = false }) { Text("Cancel") }
            }
        )
    }
}
