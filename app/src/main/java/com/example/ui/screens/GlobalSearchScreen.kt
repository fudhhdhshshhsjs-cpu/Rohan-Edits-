package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
import com.example.data.model.GoalStatus
import com.example.data.model.NoteCategory
import com.example.data.model.ProjectStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProjectStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@Composable
fun GlobalSearchScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onOpenClient: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenNotes: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.globalSearchState.collectAsStateWithLifecycle()
    var query by remember(searchState.query) { mutableStateOf(searchState.query) }
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_screen")
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.updateGlobalSearch(it)
            },
            placeholder = { Text("Search clients, projects, income, expenses, notes, goals...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.updateGlobalSearch("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("global_search_input")
        )

        if (query.isBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Search,
                    title = "Global Search",
                    subtitle = "Search directly across SQLite tables for clients, projects, transactions, notes, and career goals."
                )
            }
        } else if (searchState.isSearching && searchState.isEmptyResults) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        } else if (searchState.isEmptyResults) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Search,
                    title = "No results for \"$query\"",
                    subtitle = "Try a different client name, project title, note keyword, or transaction description."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchState.clients.isNotEmpty()) {
                    item(key = "header_clients", contentType = "header") {
                        SectionHeader(title = "Clients (${searchState.clientsTotal})")
                    }
                    items(searchState.clients, key = { "c_${it.client.id}" }, contentType = { "client" }) { c ->
                        StudioCard(onClick = { onOpenClient(c.client.id) }) {
                            Text(
                                text = c.client.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${ClientType.fromName(c.client.clientType).label} · ${c.totalProjects} projects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (searchState.projects.isNotEmpty()) {
                    item(key = "header_projects", contentType = "header") {
                        SectionHeader(title = "Projects (${searchState.projectsTotal})")
                    }
                    items(searchState.projects, key = { "p_${it.project.id}" }, contentType = { "project" }) { p ->
                        StudioCard(onClick = { onOpenProject(p.project.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.project.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = p.clientName ?: "Standalone project",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                ProjectStatusBadge(ProjectStatus.fromName(p.project.projectStatus), p.isOverdue)
                            }
                        }
                    }
                }

                if (searchState.income.isNotEmpty()) {
                    item(key = "header_income", contentType = "header") {
                        SectionHeader(title = "Income (${searchState.incomeTotal})")
                    }
                    items(searchState.income, key = { "i_${it.id}" }, contentType = { "income" }) { tx ->
                        StudioCard(onClick = onOpenBudget) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.source,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        Formatters.formatDate(tx.dateEpochDay, dateFormat),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "+${Formatters.formatCurrency(tx.amount, currency)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }

                if (searchState.expenses.isNotEmpty()) {
                    item(key = "header_expenses", contentType = "header") {
                        SectionHeader(title = "Expenses (${searchState.expensesTotal})")
                    }
                    items(searchState.expenses, key = { "e_${it.id}" }, contentType = { "expense" }) { ex ->
                        StudioCard(onClick = onOpenBudget) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.description,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        Formatters.formatDate(ex.dateEpochDay, dateFormat),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "-${Formatters.formatCurrency(ex.amount, currency)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = StatusRed
                                )
                            }
                        }
                    }
                }

                if (searchState.notes.isNotEmpty()) {
                    item(key = "header_notes", contentType = "header") {
                        SectionHeader(title = "Notes (${searchState.notesTotal})")
                    }
                    items(searchState.notes, key = { "n_${it.id}" }, contentType = { "note" }) { n ->
                        StudioCard(onClick = onOpenNotes) {
                            Text(
                                text = n.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${NoteCategory.fromName(n.category).label} · ${n.contentLength} chars",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (searchState.goals.isNotEmpty()) {
                    item(key = "header_goals", contentType = "header") {
                        SectionHeader(title = "Career Goals (${searchState.goalsTotal})")
                    }
                    items(searchState.goals, key = { "g_${it.id}" }, contentType = { "goal" }) { g ->
                        StudioCard(onClick = onOpenGoals) {
                            Text(
                                text = g.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = GoalStatus.fromName(g.status).label,
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
