package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClientType
import com.example.data.model.ProjectStatus
import com.example.data.model.VideoCategory
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.state.FinancialPeriodPoint
import com.example.ui.state.MainUiState
import com.example.ui.state.VideoOutputPeriodPoint
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val home = uiState.home
    val currency = uiState.settings.currencySymbol

    var earningsMode by remember { mutableStateOf("Monthly") }
    var selectedFinancialIndex by remember { mutableIntStateOf(-1) }
    var videoCategoryFilter by remember { mutableStateOf<VideoCategory?>(null) }

    val financialSeries = remember(earningsMode, uiState.incomeTransactions, uiState.expenseTransactions, uiState.today) {
        viewModel.computeFinancialSeries(earningsMode, uiState)
    }
    val videoOutputSeries = remember(videoCategoryFilter, uiState.projectsWithClient, uiState.today) {
        viewModel.computeVideoOutputSeries(videoCategoryFilter, uiState)
    }
    val avgVideosPerMonth = remember(uiState.home.totalVideosCompleted, uiState.today, uiState.home.officialStartDate) {
        viewModel.computeAverageVideosPerMonth(uiState)
    }

    val newClientsCount = uiState.clientsWithStats.count { it.client.clientType == ClientType.NEW.name }
    val oldClientsCount = uiState.clientsWithStats.count { it.client.clientType == ClientType.OLD.name }
    val regularClientsCount = uiState.clientsWithStats.count { it.client.clientType == ClientType.REGULAR.name }
    val topClients = remember(uiState.clientsWithStats) {
        uiState.clientsWithStats.sortedByDescending { it.totalProjects }.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overview Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Overview")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Clients", home.totalClients.toString(), modifier = Modifier.weight(1f))
                    MetricTile("Projects", home.totalProjects.toString(), modifier = Modifier.weight(1f))
                    MetricTile("Completed", home.totalVideosCompleted.toString(), accentColor = StatusGreen, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Earnings",
                        value = Formatters.formatCurrency(home.totalEarnings, currency),
                        accentColor = StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Expenses",
                        value = Formatters.formatCurrency(home.totalExpenses, currency),
                        accentColor = StatusRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Net Balance",
                        value = Formatters.formatCurrency(home.netBalance, currency),
                        accentColor = if (home.netBalance >= 0) MaterialTheme.colorScheme.primary else StatusRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Pending",
                        value = Formatters.formatCurrency(home.pendingClientPayments, currency),
                        accentColor = StatusAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Career Performance
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Career Performance")
                StudioCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Editing Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${home.totalEditingDays}",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                color = StatusGreen
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Missed Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${home.totalMissedDays}",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                color = StatusRed
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consistency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Formatters.formatPercent(home.consistencyPercent),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Completed Projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${uiState.projectsWithClient.count { it.project.projectStatus == ProjectStatus.COMPLETED.name }}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Avg Videos / Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(java.util.Locale.ENGLISH, "%.1f", avgVideosPerMonth),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Earnings Analytics Chart
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Earnings vs Expenses")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Monthly", "Yearly", "All Time").forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = earningsMode == mode,
                            onClick = {
                                earningsMode = mode
                                selectedFinancialIndex = -1
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                        ) {
                            Text(mode)
                        }
                    }
                }
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChartLegendDot(StatusGreen, "Income")
                        ChartLegendDot(StatusRed, "Expenses")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InteractiveFinancialBarChart(
                        points = financialSeries,
                        selectedIndex = selectedFinancialIndex,
                        onSelect = { selectedFinancialIndex = it }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val inspectPoint = financialSeries.getOrNull(selectedFinancialIndex) ?: financialSeries.lastOrNull()
                    if (inspectPoint != null) {
                        Text(
                            text = "${inspectPoint.label}: Income ${Formatters.formatCurrency(inspectPoint.income, currency)} · Expenses ${Formatters.formatCurrency(inspectPoint.expense, currency)} · Net ${Formatters.formatCurrency(inspectPoint.net, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Video Output Analytics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Video Output Analytics")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = videoCategoryFilter == null,
                        onClick = { videoCategoryFilter = null },
                        label = { Text("All Videos") }
                    )
                    VideoCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = videoCategoryFilter == cat,
                            onClick = { videoCategoryFilter = if (videoCategoryFilter == cat) null else cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                StudioCard {
                    VideoOutputBarChart(points = videoOutputSeries)
                }
            }
        }

        // Client Analytics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Client Analytics")
                StudioCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${uiState.clientsWithStats.size}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$newClientsCount", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Old", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$oldClientsCount", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Regular", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$regularClientsCount", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold))
                        }
                    }

                    if (topClients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Most Active Clients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        topClients.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = c.client.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${c.completedVideos}/${c.totalProjects} done · ${Formatters.formatCurrency(c.totalEarningsReceived, currency)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Add clients and complete projects to view client breakdowns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InteractiveFinancialBarChart(
    points: List<FinancialPeriodPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    if (points.isEmpty()) return
    val maxVal = max(1.0, points.maxOf { max(it.income, it.expense) })
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val groupCount = points.size
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.28f).coerceAtMost(32.dp.toPx())
            val gap = 4.dp.toPx()

            // Horizontal baseline & mid gridline
            drawLine(gridColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 1.dp.toPx())

            points.forEachIndexed { index, point ->
                val centerX = groupWidth * index + groupWidth / 2f
                val incHeight = ((point.income / maxVal) * (size.height - 12.dp.toPx())).toFloat().coerceAtLeast(if (point.income > 0) 6f else 0f)
                val expHeight = ((point.expense / maxVal) * (size.height - 12.dp.toPx())).toFloat().coerceAtLeast(if (point.expense > 0) 6f else 0f)

                val incAlpha = if (selectedIndex == -1 || selectedIndex == index) 1f else 0.45f
                if (incHeight > 0f) {
                    drawRoundRect(
                        color = StatusGreen.copy(alpha = incAlpha),
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                if (expHeight > 0f) {
                    drawRoundRect(
                        color = StatusRed.copy(alpha = incAlpha),
                        topLeft = Offset(centerX + gap / 2f, size.height - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VideoOutputBarChart(points: List<VideoOutputPeriodPoint>) {
    if (points.isEmpty()) return
    val maxCount = max(1, points.maxOf { it.completedCount })
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val groupWidth = size.width / points.size
            val barWidth = (groupWidth * 0.42f).coerceAtMost(36.dp.toPx())
            drawLine(gridColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1.dp.toPx())

            points.forEachIndexed { index, point ->
                val centerX = groupWidth * index + groupWidth / 2f
                val h = ((point.completedCount.toFloat() / maxCount.toFloat()) * (size.height - 14.dp.toPx())).coerceAtLeast(if (point.completedCount > 0) 8f else 0f)
                if (h > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${point.completedCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
