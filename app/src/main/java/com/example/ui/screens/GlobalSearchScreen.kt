package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.ClientType
import com.example.data.model.DateFormatOption
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
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, uiState) {
        viewModel.searchAll(query, uiState)
    }
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_screen")
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search clients, projects, income, expenses...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
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
                    subtitle = "Type to search across all saved clients, video projects, income entries, and expenses."
                )
            }
        } else if (
            results.clients.isEmpty() &&
            results.projects.isEmpty() &&
            results.income.isEmpty() &&
            results.expenses.isEmpty()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Search,
                    title = "No results for \"$query\"",
                    subtitle = "Try a different client name, project title, or transaction keyword."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (results.clients.isNotEmpty()) {
                    item(key = "header_clients", contentType = "header") { SectionHeader(title = "Clients (${results.clients.size})") }
                    items(results.clients, key = { "c_${it.client.id}" }, contentType = { "client" }) { c ->
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

                if (results.projects.isNotEmpty()) {
                    item(key = "header_projects", contentType = "header") { SectionHeader(title = "Projects (${results.projects.size})") }
                    items(results.projects, key = { "p_${it.project.id}" }, contentType = { "project" }) { p ->
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

                if (results.income.isNotEmpty()) {
                    item(key = "header_income", contentType = "header") { SectionHeader(title = "Income (${results.income.size})") }
                    items(results.income, key = { "i_${it.id}" }, contentType = { "income" }) { tx ->
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
                                    Text(Formatters.formatDate(tx.dateEpochDay, dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                if (results.expenses.isNotEmpty()) {
                    item(key = "header_expenses", contentType = "header") { SectionHeader(title = "Expenses (${results.expenses.size})") }
                    items(results.expenses, key = { "e_${it.id}" }, contentType = { "expense" }) { ex ->
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
                                    Text(Formatters.formatDate(ex.dateEpochDay, dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }
        }
    }
}
