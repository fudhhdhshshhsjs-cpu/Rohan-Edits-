package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarStatus
import com.example.data.model.DateFormatOption
import com.example.ui.components.MetricTile
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainerDark
import com.example.ui.theme.StatusGreenContainerLight
import com.example.ui.theme.StatusGrey
import com.example.ui.theme.StatusGreyContainerDark
import com.example.ui.theme.StatusGreyContainerLight
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainerDark
import com.example.ui.theme.StatusRedContainerLight
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val today = uiState.today
    var viewMode by remember { mutableStateOf("Monthly") }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var displayedYear by remember { mutableIntStateOf(today.year) }
    var selectedDateForEdit by remember { mutableStateOf<LocalDate?>(null) }

    val overview = uiState.calendarOverview
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val monthStats = remember(displayedMonth, uiState.calendarOverview, today) {
        viewModel.getMonthActivityStats(displayedMonth, uiState)
    }
    val yearStats = remember(displayedYear, uiState.calendarOverview, today) {
        viewModel.getYearActivityStats(displayedYear, uiState)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // View switcher: Monthly vs Yearly
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewMode == "Monthly",
                    onClick = { viewMode = "Monthly" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("calendar_mode_monthly")
                ) {
                    Text("Monthly Calendar")
                }
                SegmentedButton(
                    selected = viewMode == "Yearly",
                    onClick = { viewMode = "Yearly" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("calendar_mode_yearly")
                ) {
                    Text("Yearly Overview")
                }
            }
        }

        // Overall Career Calendar Statistics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Editing Done",
                        value = overview.totalEditingDays.toString(),
                        accentColor = StatusGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_total_done")
                    )
                    MetricTile(
                        label = "Missed Days",
                        value = overview.totalMissedDays.toString(),
                        accentColor = StatusRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_total_missed")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Not Marked",
                        value = overview.totalNotMarkedDays.toString(),
                        subtitle = "Since ${Formatters.formatDate(overview.officialStartDate, dateFormat)}",
                        accentColor = StatusGrey,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_total_not_marked")
                    )
                    MetricTile(
                        label = "Consistency",
                        value = Formatters.formatPercent(overview.consistencyPercent),
                        subtitle = "Done / (Done + Missed)",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_consistency")
                    )
                }
            }
        }

        if (viewMode == "Monthly") {
            // Month Controls
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Formatters.formatMonthYear(displayedMonth.year, displayedMonth.monthValue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${monthStats.doneDays} done · ${monthStats.missedDays} missed · ${Formatters.formatPercent(monthStats.consistencyPercent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FilterChip(
                            selected = displayedMonth == YearMonth.from(today),
                            onClick = { displayedMonth = YearMonth.from(today) },
                            label = { Text("Today") }
                        )
                        FilterChip(
                            selected = displayedMonth == YearMonth.from(overview.officialStartDate),
                            onClick = { displayedMonth = YearMonth.from(overview.officialStartDate) },
                            label = { Text("Start (Jul 2026)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weekday headers Mon..Sun
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DayOfWeek.entries.forEach { dow ->
                            Text(
                                text = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid
                    val firstDay = displayedMonth.atDay(1)
                    val leadingEmpty = firstDay.dayOfWeek.value - 1 // Monday = 1 -> 0
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7
                    val weeks = totalCells / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (week in 0 until weeks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = week * 7 + col
                                    val dayNumber = cellIndex - leadingEmpty + 1
                                    if (dayNumber in 1..daysInMonth) {
                                        val date = displayedMonth.atDay(dayNumber)
                                        val record = overview.daysByEpoch[date.toEpochDay()]
                                        val status = CalendarStatus.fromName(record?.status)
                                        val isToday = date == today
                                        val isFuture = date.isAfter(today)
                                        CalendarDayCell(
                                            dayNumber = dayNumber,
                                            status = status,
                                            isToday = isToday,
                                            isFuture = isFuture,
                                            hasNote = !record?.note.isNullOrBlank(),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("calendar_day_${date}"),
                                            onClick = { selectedDateForEdit = date }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend Card
            item {
                StudioCard {
                    Text(
                        text = "Status Legend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = StatusGreen, label = "Editing Done")
                        LegendItem(color = StatusRed, label = "Editing Missed")
                        LegendItem(color = StatusGrey, label = "Not Marked")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap any date to mark or update its status. Future and Not Marked dates are excluded from consistency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Yearly Activity Overview
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { displayedYear-- },
                            modifier = Modifier.testTag("calendar_prev_year")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Year")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$displayedYear Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${yearStats.doneDays} done · ${yearStats.missedDays} missed · ${Formatters.formatPercent(yearStats.consistencyPercent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { displayedYear++ },
                            modifier = Modifier.testTag("calendar_next_year")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year")
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (rowIndex in 0 until 6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (col in 0 until 2) {
                                val month = rowIndex * 2 + col + 1
                                val ym = YearMonth.of(displayedYear, month)
                                val mStats = viewModel.getMonthActivityStats(ym, uiState)
                                StudioCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        displayedMonth = ym
                                        viewMode = "Monthly"
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = Formatters.formatPercent(mStats.consistencyPercent),
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val totalMarked = mStats.doneDays + mStats.missedDays
                                    val ratio = if (totalMarked > 0) mStats.doneDays.toFloat() / totalMarked else 0f
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = StatusGreen,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${mStats.doneDays} Done · ${mStats.missedDays} Missed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDateForEdit?.let { date ->
        val existing = overview.daysByEpoch[date.toEpochDay()]
        CalendarStatusDialog(
            date = date,
            dateFormat = dateFormat,
            currentStatus = CalendarStatus.fromName(existing?.status),
            currentNote = existing?.note.orEmpty(),
            isFuture = date.isAfter(today),
            onSave = { status, note ->
                viewModel.setCalendarDayStatus(date.toEpochDay(), status, note)
                selectedDateForEdit = null
            },
            onDismiss = { selectedDateForEdit = null }
        )
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    status: CalendarStatus,
    isToday: Boolean,
    isFuture: Boolean,
    hasNote: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val (bgColor, textColor) = when (status) {
        CalendarStatus.DONE -> (if (isDark) StatusGreenContainerDark else StatusGreenContainerLight) to (if (isDark) Color(0xFF86EFAC) else Color(0xFF14532D))
        CalendarStatus.MISSED -> (if (isDark) StatusRedContainerDark else StatusRedContainerLight) to (if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D))
        CalendarStatus.NOT_MARKED -> (if (isDark) StatusGreyContainerDark else StatusGreyContainerLight) to MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFuture) 0.45f else 0.85f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
            if (hasNote) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CalendarStatusDialog(
    date: LocalDate,
    dateFormat: DateFormatOption,
    currentStatus: CalendarStatus,
    currentNote: String,
    isFuture: Boolean,
    onSave: (CalendarStatus, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var note by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(Formatters.formatDate(date, dateFormat), style = MaterialTheme.typography.titleLarge)
                if (isFuture) {
                    Text(
                        text = "Future date (excluded from consistency)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CalendarStatusOptionButton(
                    status = CalendarStatus.DONE,
                    selected = selectedStatus == CalendarStatus.DONE,
                    color = StatusGreen,
                    icon = Icons.Default.CheckCircle,
                    tag = "status_option_done",
                    onClick = { selectedStatus = CalendarStatus.DONE }
                )
                CalendarStatusOptionButton(
                    status = CalendarStatus.MISSED,
                    selected = selectedStatus == CalendarStatus.MISSED,
                    color = StatusRed,
                    icon = Icons.Default.Cancel,
                    tag = "status_option_missed",
                    onClick = { selectedStatus = CalendarStatus.MISSED }
                )
                CalendarStatusOptionButton(
                    status = CalendarStatus.NOT_MARKED,
                    selected = selectedStatus == CalendarStatus.NOT_MARKED,
                    color = StatusGrey,
                    icon = Icons.Default.RadioButtonUnchecked,
                    tag = "status_option_not_marked",
                    onClick = { selectedStatus = CalendarStatus.NOT_MARKED }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Editing Note (Optional)") },
                    placeholder = { Text("e.g. Color graded SaaS intro") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedStatus, note) },
                modifier = Modifier.testTag("calendar_save_status")
            ) {
                Text("Save")
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
private fun CalendarStatusOptionButton(
    status: CalendarStatus,
    selected: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = MaterialTheme.shapes.small,
        color = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
