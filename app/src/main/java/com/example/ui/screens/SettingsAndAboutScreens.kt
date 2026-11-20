package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.backup.BackupFileInspection
import com.example.data.backup.ExportTarget
import com.example.data.model.DateFormatOption
import com.example.data.model.RestoreMode
import com.example.data.model.ThemeMode
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DeadlineNotificationHelper
import com.example.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun AppSettingsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = uiState.settings
    val currentTheme = ThemeMode.fromName(settings.themeMode)
    val currentDateFormat = DateFormatOption.fromName(settings.dateFormat)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateSettings(settings.copy(deadlineRemindersEnabled = true))
            scope.launch {
                val projects = viewModel.getDueOrOverdueProjectsForReminder(uiState.today)
                DeadlineNotificationHelper.notifyUpcomingDeadlines(
                    context = context,
                    projects = projects,
                    today = uiState.today
                )
            }
            viewModel.notifyMessage("Deadline reminders enabled")
        } else {
            viewModel.updateSettings(settings.copy(deadlineRemindersEnabled = false))
            viewModel.notifyMessage("Notification permission denied", isError = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme
        item(key = "settings_theme", contentType = "card") {
            StudioCard {
                Text("Appearance Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                viewModel.updateSettings(settings.copy(themeMode = mode.name))
                            }
                            .testTag("settings_theme_${mode.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = {
                                viewModel.updateSettings(settings.copy(themeMode = mode.name))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // Date Format
        item(key = "settings_date_format", contentType = "card") {
            StudioCard {
                Text("Date Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                DateFormatOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                viewModel.updateSettings(settings.copy(dateFormat = option.name))
                            }
                            .testTag("settings_date_${option.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentDateFormat == option,
                            onClick = {
                                viewModel.updateSettings(settings.copy(dateFormat = option.name))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text("Example: ${option.example}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Currency Display
        item {
            StudioCard {
                Text("Currency Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Primary Currency: Indian Rupees (INR - ${settings.currencySymbol})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Safety & Notifications
        item {
            StudioCard {
                Text("Safety & Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confirmation Before Deletion", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Prompt before deleting projects, income, expenses, notes, or goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.confirmBeforeDelete,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(confirmBeforeDelete = it))
                        },
                        modifier = Modifier.testTag("settings_confirm_delete_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deadline Reminders", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Local notifications for projects due within 2 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.deadlineRemindersEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !DeadlineNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.updateSettings(settings.copy(deadlineRemindersEnabled = true))
                                    scope.launch {
                                        val projects = viewModel.getDueOrOverdueProjectsForReminder(uiState.today)
                                        DeadlineNotificationHelper.notifyUpcomingDeadlines(
                                            context = context,
                                            projects = projects,
                                            today = uiState.today
                                        )
                                    }
                                }
                            } else {
                                viewModel.updateSettings(settings.copy(deadlineRemindersEnabled = false))
                            }
                        },
                        modifier = Modifier.testTag("settings_reminders_switch")
                    )
                }

                if (settings.deadlineRemindersEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val projects = viewModel.getDueOrOverdueProjectsForReminder(uiState.today)
                                DeadlineNotificationHelper.notifyUpcomingDeadlines(
                                    context = context,
                                    projects = projects,
                                    today = uiState.today
                                )
                                viewModel.notifyMessage("Checked active project deadlines (${projects.size} due/overdue)")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Deadlines Now")
                    }
                }
            }
        }
    }
}

@Composable
fun BackupRestoreScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val storageState by viewModel.storageState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshStorageDiagnostics(context, runIntegrity = false)
    }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingInspection by remember { mutableStateOf<BackupFileInspection?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.MERGE_SKIP_DUPLICATES) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var isProcessingStream by remember { mutableStateOf(false) }

    var showClearAllConfirm by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingStream = true
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        viewModel.writeBackupStreaming(out)
                    } ?: error("Unable to open destination file.")
                }.onSuccess {
                    viewModel.notifyMessage("Streaming JSON backup saved successfully")
                }.onFailure {
                    viewModel.notifyMessage(it.message ?: "Backup failed", isError = true)
                }
                isProcessingStream = false
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingStream = true
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        viewModel.inspectBackupStreaming(input).getOrThrow()
                    } ?: error("Unable to read selected file.")
                }.onSuccess { inspection ->
                    restoreError = null
                    pendingRestoreUri = uri
                    pendingInspection = inspection
                }.onFailure {
                    restoreError = it.message ?: "Invalid or corrupted backup file."
                }
                isProcessingStream = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("backup_restore_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Streaming JSON Backup
        item(key = "backup_card") {
            StudioCard {
                Text("Streaming Backup (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Streams Editor Profile, Clients, Projects, Revisions, Notes, Career Goals, Calendar, Financial Ledgers, and Settings in constant memory — handles massive databases cleanly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    enabled = !isProcessingStream,
                    onClick = {
                        val fileName = "rohan_edits_backup_${uiState.today}.json"
                        backupLauncher.launch(fileName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("backup_data_button")
                ) {
                    if (isProcessingStream) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Data")
                }
            }
        }

        // Streaming JSON Restore with Duplicate Control
        item(key = "restore_card") {
            StudioCard {
                Text("Streaming Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inspects and validates any Rohan Edits JSON backup before writing. Supports merging with duplicate skipping or full replacement inside an atomic SQLite transaction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    enabled = !isProcessingStream,
                    onClick = {
                        restoreError = null
                        restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("restore_data_button")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Data")
                }
                if (restoreError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Restore failed: $restoreError",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Storage Management & SQLite Integrity Card
        item(key = "storage_management_card") {
            val diag = storageState.diagnostics
            val counts = diag.counts
            StudioCard(modifier = Modifier.testTag("storage_management_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Storage & Database Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))

                StorageStatRow("Database Storage Size", Formatters.formatFileSize(diag.databaseSizeBytes))
                StorageStatRow("Temporary Cache Size", Formatters.formatFileSize(diag.cacheSizeBytes))
                StorageStatRow("Available Device Space", Formatters.formatFileSize(diag.availableDeviceBytes))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                StorageStatRow("Total Stored Records", "${counts.totalRecords}")
                Text(
                    text = "Clients: ${counts.clients} · Projects: ${counts.projects} · Income: ${counts.incomeTransactions} · Expenses: ${counts.expenseTransactions} · Notes: ${counts.notes} · Revisions: ${counts.revisions} · Goals: ${counts.careerGoals} · Calendar: ${counts.calendarDays}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Integrity: ${diag.integrityDetail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (diag.integrityOk) {
                        true -> StatusGreen
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !storageState.isIntegrityChecking,
                        onClick = { viewModel.refreshStorageDiagnostics(context, runIntegrity = true) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("verify_integrity_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (storageState.isIntegrityChecking) "Checking..." else "Check DB")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearCacheOnly(context) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("clear_cache_button")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showClearAllConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("clear_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Application Data")
                }
            }
        }
    }

    pendingInspection?.let { insp ->
        val uri = pendingRestoreUri
        AlertDialog(
            onDismissRequest = {
                pendingInspection = null
                pendingRestoreUri = null
            },
            title = { Text("Confirm Backup Restore") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Backup v${insp.schemaVersion} exported on ${Formatters.formatTimestamp(insp.exportedAtMillis, dateFormat)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Contains: ${insp.clientCount} clients, ${insp.projectCount} projects, ${insp.incomeCount} income entries, ${insp.expenseCount} expenses, ${insp.noteCount} notes, ${insp.revisionCount} revisions, ${insp.goalCount} goals, ${insp.calendarDayCount} calendar entries.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Restore Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    RestoreMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRestoreMode = mode },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRestoreMode == mode,
                                onClick = { selectedRestoreMode = mode }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = uri != null && !isProcessingStream,
                    onClick = {
                        val targetUri = uri ?: return@Button
                        scope.launch {
                            isProcessingStream = true
                            runCatching {
                                context.contentResolver.openInputStream(targetUri)?.use { input ->
                                    viewModel.restoreBackupStreaming(input, selectedRestoreMode)
                                } ?: error("Unable to open backup stream.")
                            }.onSuccess { summary ->
                                pendingInspection = null
                                pendingRestoreUri = null
                                viewModel.refreshStorageDiagnostics(context, runIntegrity = false)
                                val msg = if (summary.skippedDuplicatesCount > 0) {
                                    "Restored ${summary.insertedCount} records (${summary.skippedDuplicatesCount} duplicates skipped)"
                                } else {
                                    "Restored ${summary.insertedCount} records successfully"
                                }
                                viewModel.notifyMessage(msg)
                            }.onFailure { err ->
                                restoreError = err.message ?: "Restore failed cleanly (existing data untouched)."
                                pendingInspection = null
                                pendingRestoreUri = null
                            }
                            isProcessingStream = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text(if (selectedRestoreMode == RestoreMode.REPLACE_ALL) "Replace & Restore" else "Merge & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingInspection = null
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All Application Data?") },
            text = {
                Text(
                    "This permanently deletes ALL clients, projects, transactions, notes, revisions, career goals, calendar entries, and saved form drafts, resetting the app to a fresh state.\n\nRecommendation: Export a JSON backup first if you ever want to restore these records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        viewModel.clearAllApplicationData(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_all_data_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

@Composable
fun ExportDataScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingTarget by remember { mutableStateOf<ExportTarget?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val target = pendingTarget
        pendingTarget = null
        if (uri != null && target != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        viewModel.writeCsvStreaming(target, out)
                    } ?: error("Unable to write CSV file.")
                }.onSuccess {
                    viewModel.notifyMessage("Exported ${target.label} to CSV")
                }.onFailure {
                    viewModel.notifyMessage(it.message ?: "CSV export failed", isError = true)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_data_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(ExportTarget.entries) { target ->
            StudioCard(
                onClick = {
                    pendingTarget = target
                    csvLauncher.launch("${target.filePrefix}_${uiState.today}.csv")
                },
                modifier = Modifier.testTag("export_${target.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(target.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Download, contentDescription = "Export ${target.label}", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AboutAppScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_app_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_editor_avatar),
                        contentDescription = "Rohan Edits",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rohan Edits App", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Version 2.0 · High-Scale Offline Storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))
                DetailInfoRow("App Name", "Rohan Edits App")
                DetailInfoRow("Owner", "Rohan")
                DetailInfoRow("Brand", "Rohan Edits")
                DetailInfoRow("Purpose", "Personal Editing Career Management")
                DetailInfoRow(
                    "Official Journey Start Date",
                    Formatters.formatDate(uiState.home.officialStartDate, dateFormat)
                )
                DetailInfoRow("Architecture", "100% Offline-First · Room SQLite (WAL) · Streaming Backup & CSV · INR (₹)")
            }
        }
    }
}
