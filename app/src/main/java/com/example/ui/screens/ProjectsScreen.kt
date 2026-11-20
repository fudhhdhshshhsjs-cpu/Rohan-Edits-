package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.components.PaginationFooter
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.PickerItem
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SearchableEntityPickerField
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    initialClientFilterId: Long? = null,
    onClearClientFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val projectsState by viewModel.projectsListState.collectAsStateWithLifecycle()
    var searchText by remember(projectsState.query) { mutableStateOf(projectsState.query) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedClientName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialClientFilterId) {
        if (initialClientFilterId != null && projectsState.clientId != initialClientFilterId) {
            viewModel.updateProjectFilter(clientId = initialClientFilterId)
        }
    }

    LaunchedEffect(projectsState.clientId) {
        selectedClientName = projectsState.clientId?.let { cid ->
            viewModel.getClientFullSafe(cid)?.name
        }
    }

    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    val activeFilterCount = listOfNotNull(
        projectsState.clientId,
        projectsState.category,
        projectsState.projectType,
        projectsState.priority,
        projectsState.paymentStatus,
        projectsState.projectStatus,
        projectsState.exactDate,
        projectsState.deadlineBefore
    ).size + if (projectsState.onlyOverdue) 1 else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("projects_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "projects_search_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.updateProjectFilter(query = it)
                    },
                    placeholder = { Text("Search projects...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.updateProjectFilter(query = "")
                            }) {
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

        item(key = "projects_filter_row") {
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
                        selected = projectsState.onlyOverdue,
                        onClick = {
                            viewModel.updateProjectFilter(onlyOverdue = !projectsState.onlyOverdue)
                        },
                        label = { Text("Overdue") }
                    )
                    ProjectStatus.entries.forEach { st ->
                        FilterChip(
                            selected = projectsState.projectStatus == st,
                            onClick = {
                                val next = if (projectsState.projectStatus == st) null else st
                                viewModel.updateProjectFilter(projectStatus = next)
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
                                    viewModel.updateProjectFilter(sortOption = opt)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (activeFilterCount > 0) {
            item(key = "projects_active_filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append("Showing ${projectsState.totalCount} filtered project(s)")
                            if (!selectedClientName.isNullOrBlank()) append(" · Client: $selectedClientName")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = {
                        viewModel.updateProjectFilter(
                            clientId = null,
                            category = null,
                            projectType = null,
                            priority = null,
                            paymentStatus = null,
                            projectStatus = null,
                            exactDate = null,
                            deadlineBefore = null,
                            onlyOverdue = false
                        )
                        onClearClientFilter()
                    }) {
                        Text("Clear All")
                    }
                }
            }
        }

        if (projectsState.items.isEmpty() && !projectsState.isLoading) {
            item(key = "projects_empty") {
                val hasNoProjectsAtAll = uiState.home.totalProjects == 0
                EmptyStateCard(
                    icon = Icons.Default.FolderCopy,
                    title = if (hasNoProjectsAtAll) "No projects available" else "No matching projects",
                    subtitle = if (hasNoProjectsAtAll) {
                        "Create your first commissioned client project, portfolio video, or practice edit."
                    } else {
                        "Try clearing filters or adjusting your search."
                    },
                    actionLabel = if (hasNoProjectsAtAll) "Add Project" else null,
                    actionTag = "projects_empty_add",
                    onActionClick = if (hasNoProjectsAtAll) onAddProject else null
                )
            }
        } else {
            itemsIndexed(
                items = projectsState.items,
                key = { _, item -> item.project.id },
                contentType = { _, _ -> "project_card" }
            ) { index, item ->
                if (index >= projectsState.items.lastIndex - 5 && projectsState.hasMore && !projectsState.isLoading) {
                    LaunchedEffect(projectsState.items.size) {
                        viewModel.loadMoreProjects()
                    }
                }
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

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            color = if (item.isOverdue) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (p.projectAmount > 0) {
                                "${Formatters.formatCurrency(p.amountPaid, currency)} / ${Formatters.formatCurrency(p.projectAmount, currency)}"
                            } else {
                                "Non-billed"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item(key = "projects_pagination") {
            PaginationFooter(
                shownCount = projectsState.items.size,
                totalCount = projectsState.totalCount,
                hasMore = projectsState.hasMore,
                isLoading = projectsState.isLoading,
                itemLabel = "projects",
                onLoadMore = { viewModel.loadMoreProjects() }
            )
        }
    }

    if (showFilterDialog) {
        var draftClientId by remember(projectsState.clientId) { mutableStateOf(projectsState.clientId) }
        var draftClientLabel by remember(selectedClientName) { mutableStateOf(selectedClientName ?: "All Clients") }
        var draftCategory by remember(projectsState.category) { mutableStateOf(projectsState.category) }
        var draftType by remember(projectsState.projectType) { mutableStateOf(projectsState.projectType) }
        var draftPriority by remember(projectsState.priority) { mutableStateOf(projectsState.priority) }
        var draftPayStatus by remember(projectsState.paymentStatus) { mutableStateOf(projectsState.paymentStatus) }
        var draftExactDate by remember(projectsState.exactDate) { mutableStateOf(projectsState.exactDate) }
        var draftDeadlineBefore by remember(projectsState.deadlineBefore) { mutableStateOf(projectsState.deadlineBefore) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Projects") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SearchableEntityPickerField(
                        label = "Client",
                        selectedLabel = if (draftClientId == null) "All Clients" else draftClientLabel,
                        noneOptionLabel = "All Clients",
                        onSelectNone = {
                            draftClientId = null
                            draftClientLabel = "All Clients"
                        },
                        onSearch = { q ->
                            viewModel.searchClientsForPicker(q).map {
                                PickerItem(value = it.id, title = it.name, subtitle = it.platform)
                            }
                        },
                        onItemSelected = { picked ->
                            draftClientId = picked.value
                            draftClientLabel = picked.title
                        }
                    )
                    DropdownSelectorField(
                        label = "Category",
                        selectedLabel = draftCategory?.label ?: "All Categories",
                        options = listOf<Pair<VideoCategory?, String>>(null to "All Categories") +
                            VideoCategory.entries.map { it to it.label },
                        onOptionSelected = { draftCategory = it }
                    )
                    DropdownSelectorField(
                        label = "Project Type",
                        selectedLabel = draftType?.label ?: "All Types",
                        options = listOf<Pair<ProjectType?, String>>(null to "All Types") +
                            ProjectType.entries.map { it to it.label },
                        onOptionSelected = { draftType = it }
                    )
                    DropdownSelectorField(
                        label = "Priority",
                        selectedLabel = draftPriority?.label ?: "All Priorities",
                        options = listOf<Pair<ProjectPriority?, String>>(null to "All Priorities") +
                            ProjectPriority.entries.map { it to it.label },
                        onOptionSelected = { draftPriority = it }
                    )
                    DropdownSelectorField(
                        label = "Payment Status",
                        selectedLabel = draftPayStatus?.label ?: "All Payment Statuses",
                        options = listOf<Pair<PaymentStatus?, String>>(null to "All Payment Statuses") +
                            PaymentStatus.entries.map { it to it.label },
                        onOptionSelected = { draftPayStatus = it }
                    )
                    DatePickerField(
                        label = "Exact Project Date",
                        epochDay = draftExactDate,
                        dateFormat = dateFormat,
                        allowClear = true,
                        onDateSelected = { draftExactDate = it }
                    )
                    DatePickerField(
                        label = "Deadline On or Before",
                        epochDay = draftDeadlineBefore,
                        dateFormat = dateFormat,
                        allowClear = true,
                        onDateSelected = { draftDeadlineBefore = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProjectFilter(
                        clientId = draftClientId,
                        category = draftCategory,
                        projectType = draftType,
                        priority = draftPriority,
                        paymentStatus = draftPayStatus,
                        exactDate = draftExactDate,
                        deadlineBefore = draftDeadlineBefore
                    )
                    showFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.updateProjectFilter(
                        clientId = null,
                        category = null,
                        projectType = null,
                        priority = null,
                        paymentStatus = null,
                        projectStatus = null,
                        exactDate = null,
                        deadlineBefore = null,
                        onlyOverdue = false
                    )
                    onClearClientFilter()
                    showFilterDialog = false
                }) { Text("Reset") }
            }
        )
    }
}
