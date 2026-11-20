package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFormatOption
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.Formatters

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onAddClient: () -> Unit,
    onAddProject: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenCalendar: () -> Unit,
    onViewAllProjects: () -> Unit,
    onOpenProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val home = uiState.home
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lightweight Modern SaaS Career Summary Card (zero heavy bitmap decode)
        item(key = "hero_card", contentType = "hero") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = home.brandName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Welcome back, ${home.editorName}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${Formatters.formatPercent(home.consistencyPercent)} Consistency",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Editing Experience",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${home.experience.totalDays} Days (${home.experience.summaryText})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Started ${Formatters.formatDate(home.officialStartDate, dateFormat)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        item(key = "quick_actions", contentType = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(
                        label = "Add Client",
                        icon = Icons.Default.PersonAdd,
                        tag = "quick_add_client",
                        isPrimary = true,
                        onClick = onAddClient
                    )
                    QuickActionPill(
                        label = "Add Project",
                        icon = Icons.Default.MovieCreation,
                        tag = "quick_add_project",
                        onClick = onAddProject
                    )
                    QuickActionPill(
                        label = "Add Income",
                        icon = Icons.Default.AddCircleOutline,
                        tag = "quick_add_income",
                        onClick = onAddIncome
                    )
                    QuickActionPill(
                        label = "Add Expense",
                        icon = Icons.Default.ReceiptLong,
                        tag = "quick_add_expense",
                        onClick = onAddExpense
                    )
                    QuickActionPill(
                        label = "Open Calendar",
                        icon = Icons.Default.CalendarMonth,
                        tag = "quick_open_calendar",
                        onClick = onOpenCalendar
                    )
                }
            }
        }

        // Career Overview
        item(key = "career_overview", contentType = "metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Career Overview")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Editing Days",
                        value = home.totalEditingDays.toString(),
                        subtitle = "${home.totalMissedDays} missed",
                        icon = Icons.Default.CheckCircle,
                        accentColor = StatusGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_editing_days")
                    )
                    MetricTile(
                        label = "Consistency",
                        value = Formatters.formatPercent(home.consistencyPercent),
                        subtitle = "Done vs Marked",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_consistency")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Total Clients",
                        value = home.totalClients.toString(),
                        subtitle = "${home.commissionedProjectsCount} client projects",
                        icon = Icons.Default.Group,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_total_clients")
                    )
                    MetricTile(
                        label = "Completed Videos",
                        value = home.totalVideosCompleted.toString(),
                        subtitle = "${home.totalProjects} total projects",
                        icon = Icons.Default.VideoLibrary,
                        accentColor = StatusGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_completed_videos")
                    )
                }
                MetricTile(
                    label = "Total Experience",
                    value = "${home.experience.totalDays} days (${home.experience.summaryText})",
                    subtitle = "Official Start: ${Formatters.formatDate(home.officialStartDate, dateFormat)}",
                    icon = Icons.Default.Schedule,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("home_experience")
                )
            }
        }

        // Financial Overview
        item(key = "financial_overview", contentType = "metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Financial Overview")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Total Earnings",
                        value = Formatters.formatCurrency(home.totalEarnings, currency),
                        subtitle = "Recorded income",
                        icon = Icons.Default.Payments,
                        accentColor = StatusGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_total_earnings")
                    )
                    MetricTile(
                        label = "Total Expenses",
                        value = Formatters.formatCurrency(home.totalExpenses, currency),
                        subtitle = "Studio costs",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = StatusRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_total_expenses")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Net Balance",
                        value = Formatters.formatCurrency(home.netBalance, currency),
                        subtitle = "Income - Expenses",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (home.netBalance >= 0) MaterialTheme.colorScheme.primary else StatusRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_net_balance")
                    )
                    MetricTile(
                        label = "Pending Payments",
                        value = Formatters.formatCurrency(home.pendingClientPayments, currency),
                        subtitle = "Awaiting client dues",
                        icon = Icons.Default.WarningAmber,
                        accentColor = StatusAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_pending_payments")
                    )
                }
            }
        }

        // Monthly Performance
        item(key = "monthly_perf", contentType = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Monthly Performance (${Formatters.formatMonthYear(uiState.today.year, uiState.today.monthValue)})"
                )
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonthlyStatColumn(
                            label = "Videos Done",
                            value = home.currentMonthCompletedVideos.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyStatColumn(
                            label = "Editing Days",
                            value = home.currentMonthEditingDays.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyStatColumn(
                            label = "Consistency",
                            value = Formatters.formatPercent(home.currentMonthConsistency),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonthlyStatColumn(
                            label = "Month Earnings",
                            value = Formatters.formatCurrency(home.currentMonthEarnings, currency),
                            valueColor = StatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyStatColumn(
                            label = "Month Expenses",
                            value = Formatters.formatCurrency(home.currentMonthExpenses, currency),
                            valueColor = StatusRed,
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyStatColumn(
                            label = "Overdue",
                            value = home.overdueProjectsCount.toString(),
                            valueColor = if (home.overdueProjectsCount > 0) StatusRed else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Active Projects Preview
        item(key = "active_projects_header", contentType = "header") {
            SectionHeader(
                title = "Active Projects",
                actionLabel = "View All",
                actionTag = "home_view_all_projects",
                onActionClick = onViewAllProjects
            )
        }

        if (home.activeProjects.isEmpty()) {
            item(key = "active_projects_empty", contentType = "empty") {
                EmptyStateCard(
                    icon = Icons.Default.FolderCopy,
                    title = "No active projects",
                    subtitle = "Create a commissioned client video, portfolio reel, or practice edit to track progress and deadlines.",
                    actionLabel = "Add Project",
                    actionTag = "home_empty_add_project",
                    onActionClick = onAddProject
                )
            }
        } else {
            items(
                items = home.activeProjects,
                key = { it.project.id },
                contentType = { "project_card" }
            ) { item ->
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
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.clientName ?: "No client linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ProjectStatusBadge(
                            status = ProjectStatus.fromName(p.projectStatus),
                            isOverdue = item.isOverdue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityBadge(priority = ProjectPriority.fromName(p.priority))
                        Text(
                            text = p.deadlineEpochDay?.let { "Due ${Formatters.formatDate(it, dateFormat)}" }
                                ?: "No deadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    tag: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(tag),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(tag),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun MonthlyStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
