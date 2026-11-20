package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateFormatOption
import com.example.ui.components.DatePickerField
import com.example.ui.components.MetricTile
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun EditorInfoScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val profile = uiState.editorProfile
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val startDate = LocalDate.ofEpochDay(profile.officialStartDateEpochDay)
    val experience = Formatters.calculateExperience(startDate, uiState.today)

    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var brandName by remember(profile.id) { mutableStateOf(profile.brandName) }
    var profession by remember(profile.id) { mutableStateOf(profile.profession) }
    var officialStartEpoch by remember(profile.id) { mutableStateOf(profile.officialStartDateEpochDay) }
    var isStartDateUnlocked by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }

    var professionalExp by remember(profile.id) { mutableStateOf(profile.professionalExperience) }
    var skills by remember(profile.id) { mutableStateOf(profile.skills) }
    var editingSoftware by remember(profile.id) { mutableStateOf(profile.editingSoftware) }
    var specializations by remember(profile.id) { mutableStateOf(profile.specializations) }
    var portfolioLink by remember(profile.id) { mutableStateOf(profile.portfolioLink) }
    var personalNotes by remember(profile.id) { mutableStateOf(profile.personalNotes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoSavedVisible by remember { mutableStateOf(false) }

    // Keep local fields synced if backup restore replaces profile from outside
    LaunchedEffect(profile) {
        if (profile.name != name && name == "Rohan" && profile.name != "Rohan") name = profile.name
        if (profile.brandName != brandName && brandName == "Rohan Edits" && profile.brandName != "Rohan Edits") brandName = profile.brandName
    }

    // Automatic debounced persistence so large profile notes never get lost on navigation or closure
    LaunchedEffect(
        name,
        brandName,
        profession,
        officialStartEpoch,
        professionalExp,
        skills,
        editingSoftware,
        specializations,
        portfolioLink,
        personalNotes
    ) {
        val candidate = profile.copy(
            name = name.trim(),
            brandName = brandName.trim(),
            profession = profession.trim(),
            officialStartDateEpochDay = officialStartEpoch,
            professionalExperience = professionalExp.trim(),
            skills = skills.trim(),
            editingSoftware = editingSoftware.trim(),
            specializations = specializations.trim(),
            portfolioLink = portfolioLink.trim(),
            personalNotes = personalNotes.trim()
        )
        if (candidate != profile && candidate.name.isNotBlank() && candidate.brandName.isNotBlank()) {
            delay(800L)
            viewModel.autoSaveEditorProfile(candidate) {
                autoSavedVisible = true
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("editor_info_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Banner & Live Experience Counter
        item(key = "profile_banner", contentType = "banner") {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_editor_avatar),
                        contentDescription = "Editor Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile.brandName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile.profession,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Experience (Days)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${experience.totalDays} days",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Months & Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            experience.summaryText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Official Start Date: ${Formatters.formatDate(startDate, dateFormat)} (Asia/Kolkata)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar Integration (Read directly from Calendar database)
        item(key = "calendar_metrics", contentType = "metrics") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Editing Days",
                    value = uiState.calendarOverview.totalEditingDays.toString(),
                    accentColor = StatusGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Missed Days",
                    value = uiState.calendarOverview.totalMissedDays.toString(),
                    accentColor = StatusRed,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Consistency",
                    value = Formatters.formatPercent(uiState.calendarOverview.consistencyPercent),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Editable Fields + Protected Start Date
        item(key = "profile_form", contentType = "form") {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Professional Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (autoSavedVisible) {
                        Text(
                            text = "Auto-saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null; autoSavedVisible = false },
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("editor_name_input")
                    )
                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it; errorMessage = null; autoSavedVisible = false },
                        label = { Text("Brand Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("editor_brand_input")
                    )
                    OutlinedTextField(
                        value = profession,
                        onValueChange = { profession = it; autoSavedVisible = false },
                        label = { Text("Profession") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Protected Official Start Date
                    if (!isStartDateUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Official Editing Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Formatters.formatDate(officialStartEpoch, dateFormat),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showUnlockConfirm = true },
                                modifier = Modifier.testTag("editor_unlock_start_date")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Start Date")
                            }
                        }
                    } else {
                        DatePickerField(
                            label = "Official Start Date (Unlocked)",
                            epochDay = officialStartEpoch,
                            dateFormat = dateFormat,
                            onDateSelected = { if (it != null) officialStartEpoch = it }
                        )
                        TextButton(onClick = { isStartDateUnlocked = false }) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock Start Date")
                        }
                    }

                    OutlinedTextField(
                        value = professionalExp,
                        onValueChange = { professionalExp = it; autoSavedVisible = false },
                        label = { Text("Professional Experience") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = skills,
                        onValueChange = { skills = it; autoSavedVisible = false },
                        label = { Text("Skills") },
                        placeholder = { Text("e.g. Motion Graphics, Keyframing, Sound Design") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingSoftware,
                        onValueChange = { editingSoftware = it; autoSavedVisible = false },
                        label = { Text("Editing Software") },
                        placeholder = { Text("e.g. After Effects, Premiere Pro, DaVinci Resolve") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specializations,
                        onValueChange = { specializations = it; autoSavedVisible = false },
                        label = { Text("Specializations") },
                        placeholder = { Text("e.g. SaaS Explainers, Retention Reels") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = portfolioLink,
                        onValueChange = { portfolioLink = it; autoSavedVisible = false },
                        label = { Text("Portfolio Link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = personalNotes,
                        onValueChange = { personalNotes = it; autoSavedVisible = false },
                        label = { Text("Personal Notes") },
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank() || brandName.isBlank()) {
                                errorMessage = "Name and Brand Name are required."
                                return@Button
                            }
                            viewModel.saveEditorProfile(
                                profile.copy(
                                    name = name.trim(),
                                    brandName = brandName.trim(),
                                    profession = profession.trim(),
                                    officialStartDateEpochDay = officialStartEpoch,
                                    professionalExperience = professionalExp.trim(),
                                    skills = skills.trim(),
                                    editingSoftware = editingSoftware.trim(),
                                    specializations = specializations.trim(),
                                    portfolioLink = portfolioLink.trim(),
                                    personalNotes = personalNotes.trim()
                                )
                            ) {
                                isStartDateUnlocked = false
                                autoSavedVisible = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("editor_save_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }

    if (showUnlockConfirm) {
        AlertDialog(
            onDismissRequest = { showUnlockConfirm = false },
            title = { Text("Edit Official Start Date?") },
            text = {
                Text("Your official editing journey start date (06 Jul 2026) is protected to prevent accidental changes to your experience counter. Unlock it for editing?")
            },
            confirmButton = {
                Button(onClick = {
                    isStartDateUnlocked = true
                    showUnlockConfirm = false
                }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
