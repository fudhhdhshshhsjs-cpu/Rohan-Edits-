package com.example.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.data.backup.BackupPayloadDto
import com.example.data.backup.ExportTarget
import com.example.data.model.DateFormatOption
import com.example.data.model.ThemeMode
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
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
    val settings = uiState.settings
    val currentTheme = ThemeMode.fromName(settings.themeMode)
    val currentDateFormat = DateFormatOption.fromName(settings.dateFormat)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateSettings(settings.copy(deadlineRemindersEnabled = true))
            DeadlineNotificationHelper.notifyUpcomingDeadlines(
                context = context,
                projects = uiState.projectsWithClient.map { it.project },
                today = uiState.today
            )
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
                            "Prompt before deleting projects, income, or expenses",
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
                                    DeadlineNotificationHelper.notifyUpcomingDeadlines(
                                        context = context,
                                        projects = uiState.projectsWithClient.map { it.project },
                                        today = uiState.today
                                    )
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
                            DeadlineNotificationHelper.notifyUpcomingDeadlines(
                                context = context,
                                projects = uiState.projectsWithClient.map { it.project },
                                today = uiState.today
                            )
                            viewModel.notifyMessage("Checked active project deadlines")
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

    var pendingRestoreDto by remember { mutableStateOf<BackupPayloadDto?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.buildBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file.")
                }.onSuccess {
                    viewModel.notifyMessage("JSON backup saved successfully")
                }.onFailure {
                    viewModel.notifyMessage(it.message ?: "Backup failed", isError = true)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    } ?: error("Unable to read selected file.")
                    viewModel.parseBackupJson(json).getOrThrow()
                }.onSuccess { dto ->
                    restoreError = null
                    pendingRestoreDto = dto
                }.onFailure {
                    restoreError = it.message ?: "Invalid backup file."
                }
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
        item {
            StudioCard {
                Text("Backup All Data (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Exports your Editor Profile, Clients, Projects, Editing Calendar, Income & Expense Ledgers, and App Settings into one portable JSON file using Android Storage Access Framework.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val fileName = "rohan_edits_backup_${uiState.today}.json"
                        backupLauncher.launch(fileName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("backup_data_button")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Data")
                }
            }
        }

        item {
            StudioCard {
                Text("Restore From Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Import a previously exported Rohan Edits JSON backup file. Records are validated before any data changes are applied.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
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
    }

    pendingRestoreDto?.let { dto ->
        AlertDialog(
            onDismissRequest = { pendingRestoreDto = null },
            title = { Text("Confirm Backup Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Backup exported on ${Formatters.formatTimestamp(dto.exportedAtMillis, dateFormat)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Contains: ${dto.clients.size} clients, ${dto.projects.size} projects, ${dto.calendarDays.size} calendar entries, ${dto.incomeTransactions.size} income records, ${dto.expenseTransactions.size} expenses.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Warning: Restoring will REPLACE all existing data in the app with this backup. Create a backup first if you want to keep current records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(dto) {
                            pendingRestoreDto = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Replace & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreDto = null }) {
                    Text("Cancel")
                }
            }
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
                    val csv = viewModel.buildCsvExport(target)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
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
                    Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                DetailInfoRow("Architecture", "100% Offline-First · Room SQLite + DataStore · Asia/Kolkata · INR (₹)")
            }
        }
    }
}
