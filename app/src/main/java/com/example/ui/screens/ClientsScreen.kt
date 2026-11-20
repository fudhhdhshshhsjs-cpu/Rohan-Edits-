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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.ClientSortOption
import com.example.data.model.ClientType
import com.example.data.model.PlatformType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.util.Formatters

@Composable
fun ClientsHubScreen(
    uiState: MainUiState,
    showProjectsTab: Boolean,
    onTabChange: (Boolean) -> Unit,
    onAddClient: () -> Unit,
    onOpenClient: (Long) -> Unit,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    initialClientFilterId: Long? = null,
    onClearClientFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = !showProjectsTab,
                onClick = { onTabChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("hub_tab_clients")
            ) {
                Text("Clients (${uiState.clientsWithStats.size})")
            }
            SegmentedButton(
                selected = showProjectsTab,
                onClick = { onTabChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("hub_tab_projects")
            ) {
                Text("Projects (${uiState.projectsWithClient.size})")
            }
        }

        if (showProjectsTab) {
            ProjectsScreen(
                uiState = uiState,
                initialClientFilterId = initialClientFilterId,
                onClearClientFilter = onClearClientFilter,
                onAddProject = onAddProject,
                onOpenProject = onOpenProject
            )
        } else {
            ClientsListContent(
                uiState = uiState,
                onAddClient = onAddClient,
                onOpenClient = onOpenClient
            )
        }
    }
}

@Composable
private fun ClientsListContent(
    uiState: MainUiState,
    onAddClient: () -> Unit,
    onOpenClient: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ClientType?>(null) }
    var sortOption by remember { mutableStateOf(ClientSortOption.RECENT_ACTIVITY) }
    var showSortMenu by remember { mutableStateOf(false) }

    val currency = uiState.settings.currencySymbol

    val filteredClients = remember(uiState.clientsWithStats, searchQuery, selectedType, sortOption) {
        uiState.clientsWithStats
            .filter { item ->
                val matchesSearch = searchQuery.isBlank() ||
                    item.client.name.contains(searchQuery, ignoreCase = true) ||
                    item.client.preferredEditingStyle.contains(searchQuery, ignoreCase = true)
                val matchesType = selectedType == null || item.client.clientType == selectedType!!.name
                matchesSearch && matchesType
            }
            .let { list ->
                when (sortOption) {
                    ClientSortOption.NAME -> list.sortedBy { it.client.name.lowercase() }
                    ClientSortOption.RECENT_ACTIVITY -> list.sortedByDescending { it.lastActivityMillis }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clients_screen"),
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
                    placeholder = { Text("Search clients...") },
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
                        .testTag("clients_search_input")
                )
                Button(
                    onClick = onAddClient,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("clients_add_button")
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
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All") }
                    )
                    ClientType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = { Text(type.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort clients", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (sortOption == ClientSortOption.NAME) "Name" else "Recent")
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        ClientSortOption.entries.forEach { opt ->
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

        if (filteredClients.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Group,
                    title = if (uiState.clientsWithStats.isEmpty()) "No clients added yet" else "No matching clients",
                    subtitle = if (uiState.clientsWithStats.isEmpty()) {
                        "Add your first editing client to track projects, per-video rates, and pending payments."
                    } else {
                        "Try adjusting your search or filter criteria."
                    },
                    actionLabel = if (uiState.clientsWithStats.isEmpty()) "Add Client" else null,
                    actionTag = "clients_empty_add",
                    onActionClick = if (uiState.clientsWithStats.isEmpty()) onAddClient else null
                )
            }
        } else {
            items(filteredClients, key = { it.client.id }) { item ->
                val client = item.client
                val platform = PlatformType.fromName(client.platform)
                val type = ClientType.fromName(client.clientType)
                StudioCard(
                    onClick = { onOpenClient(client.id) },
                    modifier = Modifier.testTag("client_card_${client.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(
                                    text = platform.label,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                StatusPill(
                                    text = type.label,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatCurrency(item.pendingPayments, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (item.pendingPayments > 0) StatusAmber else StatusGreen
                            )
                            Text(
                                text = if (item.pendingPayments > 0) "Pending" else "Settled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.totalProjects} Projects · ${item.completedVideos} Completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (client.perVideoPrice > 0) {
                            Text(
                                text = "${Formatters.formatCurrency(client.perVideoPrice, currency)} / video",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
