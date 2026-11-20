package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFormatOption
import com.example.data.model.PaymentStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectSortOption
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectType
import com.example.data.model.VideoCategory
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusRed
import com.example.util.Formatters

@Composable
fun ProjectsScreen(
    uiState: MainUiState,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    initialClientFilterId: Long? = null,
    onClearClientFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterClientId by remember(initialClientFilterId) { mutableStateOf(initialClientFilterId) }
    var filterCategory by remember { mutableStateOf<VideoCategory?>(null) }
    var filterType by remember { mutableStateOf<ProjectType?>(null) }
    var filterPriority by remember { mutableStateOf<ProjectPriority?>(null) }
    var filterPaymentStatus by remember { mutableStateOf<PaymentStatus?>(null) }
    var filterProjectStatus by remember { mutableStateOf<ProjectStatus?>(null) }
    var filterProjectDate by remember { mutableStateOf<Long?>(null) }
    var filterDeadlineBefore by remember { mutableStateOf<Long?>(null) }
    var onlyOverdue by remember { mutableStateOf(false) }

    var sortOption by remember { mutableStateOf(ProjectSortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    val activeFilterCount = listOfNotNull(
        filterClientId,
        filterCategory,
        filterType,
        filterPriority,
        filterPaymentStatus,
        filterProjectStatus,
        filterProjectDate,
        filterDeadlineBefore
    ).size + if (onlyOverdue) 1 else 0

    val filteredProjects = remember(
        uiState.projectsWithClient,
        searchQuery,
        filterClientId,
        filterCategory,
        filterType,
        filterPriority,
        filterPaymentStatus,
        filterProjectStatus,
        filterProjectDate,
        filterDeadlineBefore,
        onlyOverdue,
        sortOption
    ) {
        uiState.projectsWithClient
            .filter { item ->
                val p = item.project
                val matchesQuery = searchQuery.isBlank() ||
                    p.title.contains(searchQuery, ignoreCase = true) ||
                    (item.clientName?.contains(searchQuery, ignoreCase = true) == true)
                val matchesClient = filterClientId == null || p.clientId == filterClientId
                val matchesCat = filterCategory == null || p.category == filterCategory!!.name
                val matchesType = filterType == null || p.projectType == filterType!!.name
                val matchesPriority = filterPriority == null || p.priority == filterPriority!!.name
                val matchesPay = filterPaymentStatus == null || p.paymentStatus == filterPaymentStatus!!.name
                val matchesStatus = filterProjectStatus == null || p.projectStatus == filterProjectStatus!!.name
                val matchesDate = filterProjectDate == null || p.projectDateEpochDay == filterProjectDate
                val matchesDeadline = filterDeadlineBefore == null ||
                    (p.deadlineEpochDay != null && p.deadlineEpochDay <= filterDeadlineBefore!!)
                val matchesOverdue = !onlyOverdue || item.isOverdue

                matchesQuery && matchesClient && matchesCat && matchesType &&
                    matchesPriority && matchesPay && matchesStatus &&
                    matchesDate && matchesDeadline && matchesOverdue
            }
            .let { list ->
                when (sortOption) {
                    ProjectSortOption.DATE_DESC -> list.sortedByDescending { it.project.projectDateEpochDay }
                    ProjectSortOption.DEADLINE_ASC -> list.sortedWith(
                        compareBy<com.example.ui.state.ProjectWithClient> { it.project.deadlineEpochDay ?: Long.MAX_VALUE }
                    )
                    ProjectSortOption.AMOUNT_DESC -> list.sortedByDescending { it.project.projectAmount }
                    ProjectSortOption.PRIORITY_DESC -> list.sortedByDescending {
                        ProjectPriority.fromName(it.project.priority).sortWeight
                    }
                }
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("projects_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search projects...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("projects_search_input")
                )
                Button(
                    onClick = onAddProject,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("projects_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilterCount > 0,
                        onClick = { showFilterDialog = true },
                        label = {
                            Text(if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = onlyOverdue,
                        onClick = { onlyOverdue = !onlyOverdue },
                        label = { Text("Overdue") }
                    )
                    ProjectStatus.entries.forEach { st ->
                        FilterChip(
                            selected = filterProjectStatus == st,
                            onClick = {
                                filterProjectStatus = if (filterProjectStatus == st) null else st
                            },
                            label = { Text(st.label) }
                        )
                    }
                }
                TextButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort projects", modifier = Modifier.size(18.dp))
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        ProjectSortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    sortOption = opt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (activeFilterCount > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val clientLabel = filterClientId?.let { id ->
                        uiState.clientsWithStats.firstOrNull { it.client.id == id }?.client?.name
                    }
                    Text(
                        text = buildString {
                            append("Showing ${filteredProjects.size} filtered project(s)")
                            if (clientLabel != null) append(" · Client: $clientLabel")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = {
                        filterClientId = null
                        filterCategory = null
                        filterType = null
                        filterPriority = null
                        filterPaymentStatus = null
                        filterProjectStatus = null
                        filterProjectDate = null
                        filterDeadlineBefore = null
                        onlyOverdue = false
                        onClearClientFilter()
                    }) {
                        Text("Clear All")
                    }
                }
            }
        }

        if (filteredProjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.FolderCopy,
                    title = if (uiState.projectsWithClient.isEmpty()) "No projects available" else "No matching projects",
                    subtitle = if (uiState.projectsWithClient.isEmpty()) {
                        "Create your first commissioned client project, portfolio video, or practice edit."
                    } else {
                        "Try clearing filters or adjusting your search."
                    },
                    actionLabel = if (uiState.projectsWithClient.isEmpty()) "Add Project" else null,
                    actionTag = "projects_empty_add",
                    onActionClick = if (uiState.projectsWithClient.isEmpty()) onAddProject else null
                )
            }
        } else {
            items(filteredProjects, key = { it.project.id }) { item ->
                val p = item.project
                StudioCard(
                    onClick = { onOpenProject(p.id) },
                    modifier = Modifier.testTag("project_card_${p.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(VideoCategory.fromName(p.category).label)
                                    if (!item.clientName.isNullOrBlank()) {
                                        append(" · ${item.clientName}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ProjectStatusBadge(
                            status = ProjectStatus.fromName(p.projectStatus),
                            isOverdue = item.isOverdue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = ProjectType.fromName(p.projectType).label,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PriorityBadge(priority = ProjectPriority.fromName(p.priority))
                        PaymentStatusBadge(status = PaymentStatus.fromName(p.paymentStatus))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.deadlineEpochDay?.let {
                                "Due ${Formatters.formatDate(it, dateFormat)}"
                            } ?: "Date ${Formatters.formatDate(p.projectDateEpochDay, dateFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (p.projectAmount > 0) {
                                "${Formatters.formatCurrency(p.amountPaid, currency)} / ${Formatters.formatCurrency(p.projectAmount, currency)}"
                            } else {
                                "Non-billed"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Projects") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val clientOptions = listOf<Pair<Long?, String>>(null to "All Clients") +
                        uiState.clientsWithStats.map { it.client.id to it.client.name }
                    DropdownSelectorField(
                        label = "Client",
                        selectedLabel = clientOptions.firstOrNull { it.first == filterClientId }?.second ?: "All Clients",
                        options = clientOptions,
                        onOptionSelected = { filterClientId = it }
                    )
                    DropdownSelectorField(
                        label = "Category",
                        selectedLabel = filterCategory?.label ?: "All Categories",
                        options = listOf<Pair<VideoCategory?, String>>(null to "All Categories") +
                            VideoCategory.entries.map { it to it.label },
                        onOptionSelected = { filterCategory = it }
                    )
                    DropdownSelectorField(
                        label = "Project Type",
                        selectedLabel = filterType?.label ?: "All Types",
                        options = listOf<Pair<ProjectType?, String>>(null to "All Types") +
                            ProjectType.entries.map { it to it.label },
                        onOptionSelected = { filterType = it }
                    )
                    DropdownSelectorField(
                        label = "Priority",
                        selectedLabel = filterPriority?.label ?: "All Priorities",
                        options = listOf<Pair<ProjectPriority?, String>>(null to "All Priorities") +
                            ProjectPriority.entries.map { it to it.label },
                        onOptionSelected = { filterPriority = it }
                    )
                    DropdownSelectorField(
                        label = "Payment Status",
                        selectedLabel = filterPaymentStatus?.label ?: "All Payment Statuses",
                        options = listOf<Pair<PaymentStatus?, String>>(null to "All Payment Statuses") +
                            PaymentStatus.entries.map { it to it.label },
                        onOptionSelected = { filterPaymentStatus = it }
                    )
                    DatePickerField(
                        label = "Exact Project Date",
                        epochDay = filterProjectDate,
                        dateFormat = dateFormat,
                        allowClear = true,
                        onDateSelected = { filterProjectDate = it }
                    )
                    DatePickerField(
                        label = "Deadline On or Before",
                        epochDay = filterDeadlineBefore,
                        dateFormat = dateFormat,
                        allowClear = true,
                        onDateSelected = { filterDeadlineBefore = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) { Text("Apply") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    filterClientId = null
                    filterCategory = null
                    filterType = null
                    filterPriority = null
                    filterPaymentStatus = null
                    filterProjectStatus = null
                    filterProjectDate = null
                    filterDeadlineBefore = null
                    onlyOverdue = false
                    onClearClientFilter()
                    showFilterDialog = false
                }) { Text("Reset") }
            }
        )
    }
}
