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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainerDark
import com.example.ui.theme.StatusAmberContainerLight
import com.example.ui.theme.StatusAmberLight
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainerDark
import com.example.ui.theme.StatusGreenContainerLight
import com.example.ui.theme.StatusGreenLight
import com.example.ui.theme.StatusGrey
import com.example.ui.theme.StatusGreyContainerDark
import com.example.ui.theme.StatusGreyContainerLight
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainerDark
import com.example.ui.theme.StatusRedContainerLight
import com.example.ui.theme.StatusRedLight
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
    val shape = MaterialTheme.shapes.medium
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
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
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = if (useMono) "tnum" else null
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
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
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProjectStatusBadge(status: ProjectStatus, isOverdue: Boolean = false) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    if (isOverdue) {
        StatusPill(
            text = "Overdue",
            containerColor = if (isDark) StatusRedContainerDark else StatusRedContainerLight,
            contentColor = if (isDark) Color(0xFFFCA5A5) else StatusRedLight
        )
        return
    }
    when (status) {
        ProjectStatus.COMPLETED -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusGreenContainerDark else StatusGreenContainerLight,
            contentColor = if (isDark) Color(0xFF86EFAC) else StatusGreenLight
        )
        ProjectStatus.IN_PROGRESS -> StatusPill(
            text = status.label,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ProjectStatus.PENDING -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusAmberContainerDark else StatusAmberContainerLight,
            contentColor = if (isDark) Color(0xFFFCD34D) else StatusAmberLight
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    when (status) {
        PaymentStatus.PAID -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusGreenContainerDark else StatusGreenContainerLight,
            contentColor = if (isDark) Color(0xFF86EFAC) else StatusGreenLight
        )
        PaymentStatus.PARTIAL -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusAmberContainerDark else StatusAmberContainerLight,
            contentColor = if (isDark) Color(0xFFFCD34D) else StatusAmberLight
        )
        PaymentStatus.UNPAID -> StatusPill(
            text = status.label,
            containerColor = if (isDark) StatusRedContainerDark else StatusRedContainerLight,
            contentColor = if (isDark) Color(0xFFFCA5A5) else StatusRedLight
        )
    }
}

@Composable
fun PriorityBadge(priority: ProjectPriority) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val (bg, fg) = when (priority) {
        ProjectPriority.URGENT -> (if (isDark) StatusRedContainerDark else StatusRedContainerLight) to (if (isDark) Color(0xFFFCA5A5) else StatusRedLight)
        ProjectPriority.HIGH -> (if (isDark) StatusAmberContainerDark else StatusAmberContainerLight) to (if (isDark) Color(0xFFFCD34D) else StatusAmberLight)
        ProjectPriority.NORMAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ProjectPriority.LOW -> (if (isDark) StatusGreyContainerDark else StatusGreyContainerLight) to (if (isDark) Color(0xFFCBD5E1) else StatusGrey)
    }
    StatusPill(text = priority.label, containerColor = bg, contentColor = fg)
}

@Composable
fun ExpandableInfoRow(
    label: String,
    value: String,
    previewLimit: Int = 280
) {
    var expanded by remember(value) { mutableStateOf(false) }
    val isLong = value.length > previewLimit

    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        val displayedText = if (!isLong || expanded) {
            if (value.length > 4000) value.take(4000) + "\n… (${value.length} characters stored)" else value
        } else {
            value.take(previewLimit).trimEnd() + "…"
        }
        Text(
            text = displayedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isLong) {
            Text(
                text = if (expanded) "Show less" else "Read more (${value.length} chars)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
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
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(14.dp))
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

@Composable
fun PaginationFooter(
    shownCount: Int,
    totalCount: Int,
    hasMore: Boolean,
    isLoading: Boolean,
    itemLabel: String,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalCount <= 0 && !isLoading) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Showing $shownCount of $totalCount $itemLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (hasMore) {
            OutlinedButton(
                onClick = onLoadMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("load_more_${itemLabel.lowercase().replace(' ', '_')}")
            ) {
                Text("Load More (${(totalCount - shownCount).coerceAtLeast(0)} remaining)")
            }
        }
    }
}

data class PickerItem<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null
)

@Composable
fun <T> SearchableEntityPickerField(
    label: String,
    selectedLabel: String,
    noneOptionLabel: String? = null,
    onSelectNone: (() -> Unit)? = null,
    onSearch: suspend (String) -> List<PickerItem<T>>,
    onItemSelected: (PickerItem<T>) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

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
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<PickerItem<T>>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }

        LaunchedEffect(searchQuery) {
            loading = true
            delay(120)
            results = runCatching { onSearch(searchQuery) }.getOrDefault(emptyList())
            loading = false
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select $label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (noneOptionLabel != null && onSelectNone != null) {
                        TextButton(
                            onClick = {
                                onSelectNone()
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(noneOptionLabel, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider()
                    }
                    if (loading && results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (results.isEmpty()) {
                        Text(
                            text = "No matching entries found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            items(results) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onItemSelected(item)
                                            showDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!item.subtitle.isNullOrBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            }
        )
    }
}

