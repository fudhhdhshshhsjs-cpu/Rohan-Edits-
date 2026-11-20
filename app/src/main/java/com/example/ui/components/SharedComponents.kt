package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainerDark
import com.example.ui.theme.StatusAmberContainerLight
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainerDark
import com.example.ui.theme.StatusGreenContainerLight
import com.example.ui.theme.StatusGrey
import com.example.ui.theme.StatusGreyContainerDark
import com.example.ui.theme.StatusGreyContainerLight
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainerDark
import com.example.ui.theme.StatusRedContainerLight
import com.example.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTag: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    useMono: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    StudioCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = if (useMono) MonoFontFamily else MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProjectStatusBadge(status: ProjectStatus, isOverdue: Boolean = false) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    if (isOverdue) {
        StatusPill(
            text = "Overdue",
            containerColor = if (isDark) StatusRedContainerDark else StatusRedContainerLight,
            contentColor = if (isDark) Color(0xFFFCA5A5) else StatusRed
        )
        return
    }
    when (status) {
        ProjectStatus.COMPLETED -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusGreenContainerDark else StatusGreenContainerLight,
            contentColor = if (isDark) Color(0xFF86EFAC) else StatusGreen
        )
        ProjectStatus.IN_PROGRESS -> StatusPill(
            text = status.label,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ProjectStatus.PENDING -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusAmberContainerDark else StatusAmberContainerLight,
            contentColor = if (isDark) Color(0xFFFCD34D) else StatusAmber
        )
        ProjectStatus.CANCELLED -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusGreyContainerDark else StatusGreyContainerLight,
            contentColor = if (isDark) Color(0xFFCBD5E1) else StatusGrey
        )
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    when (status) {
        PaymentStatus.PAID -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusGreenContainerDark else StatusGreenContainerLight,
            contentColor = if (isDark) Color(0xFF86EFAC) else StatusGreen
        )
        PaymentStatus.PARTIAL -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusAmberContainerDark else StatusAmberContainerLight,
            contentColor = if (isDark) Color(0xFFFCD34D) else StatusAmber
        )
        PaymentStatus.UNPAID -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusRedContainerDark else StatusRedContainerLight,
            contentColor = if (isDark) Color(0xFFFCA5A5) else StatusRed
        )
    }
}

@Composable
fun PriorityBadge(priority: ProjectPriority) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val (bg, fg) = when (priority) {
        ProjectPriority.URGENT -> (if (isDark) StatusRedContainerDark else StatusRedContainerLight) to (if (isDark) Color(0xFFFCA5A5) else StatusRed)
        ProjectPriority.HIGH -> (if (isDark) StatusAmberContainerDark else StatusAmberContainerLight) to (if (isDark) Color(0xFFFCD34D) else StatusAmber)
        ProjectPriority.NORMAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ProjectPriority.LOW -> (if (isDark) StatusGreyContainerDark else StatusGreyContainerLight) to (if (isDark) Color(0xFFCBD5E1) else StatusGrey)
    }
    StatusPill(text = priority.label, containerColor = bg, contentColor = fg)
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTag: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    StudioCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .then(if (actionTag != null) Modifier.testTag(actionTag) else Modifier)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun <T> DropdownSelectorField(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select $label")
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onOptionSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    epochDay: Long?,
    dateFormat: DateFormatOption,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    testTag: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayText = epochDay?.let { Formatters.formatDate(it, dateFormat) } ?: "Not set"

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (allowClear && epochDay != null) {
                    IconButton(onClick = { onDateSelected(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear date")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (allowClear && epochDay != null) 48.dp else 0.dp)
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        val initialMillis = (epochDay ?: LocalDate.now().toEpochDay()) * 86_400_000L
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateSelected(selectedLocalDate.toEpochDay())
                        }
                        showPicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
