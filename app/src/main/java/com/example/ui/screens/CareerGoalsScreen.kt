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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CareerGoalEntity
import com.example.data.model.DateFormatOption
import com.example.data.model.GoalMetric
import com.example.data.model.GoalStatus
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaginationFooter
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@Composable
fun CareerGoalsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val goalsState by viewModel.goalsListState.collectAsStateWithLifecycle()
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    val currency = uiState.settings.currencySymbol

    var searchText by remember(goalsState.query) { mutableStateOf(goalsState.query) }
    var editingGoal by remember { mutableStateOf<CareerGoalEntity?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var pendingDeleteGoal by remember { mutableStateOf<CareerGoalEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("career_goals_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "goals_search_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.updateGoalsFilter(query = it)
                    },
                    placeholder = { Text("Search career goals...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.updateGoalsFilter(query = "")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("goals_search_input")
                )
                Button(
                    onClick = {
                        editingGoal = null
                        showGoalDialog = true
                    },
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("goals_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        item(key = "goals_status_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = goalsState.status == null,
                    onClick = { viewModel.updateGoalsFilter(status = null) },
                    label = { Text("All") }
                )
                GoalStatus.entries.forEach { st ->
                    FilterChip(
                        selected = goalsState.status == st,
                        onClick = {
                            val next = if (goalsState.status == st) null else st
                            viewModel.updateGoalsFilter(status = next)
                        },
                        label = { Text(st.label) }
                    )
                }
            }
        }

        if (goalsState.items.isEmpty() && !goalsState.isLoading) {
            item(key = "goals_empty") {
                EmptyStateCard(
                    icon = Icons.Default.Flag,
                    title = "No career goals yet",
                    subtitle = "Set monthly income targets, video milestones, client acquisition goals, or consistency streaks.",
                    actionLabel = "Add Career Goal",
                    actionTag = "goals_empty_add",
                    onActionClick = {
                        editingGoal = null
                        showGoalDialog = true
                    }
                )
            }
        } else {
            itemsIndexed(
                items = goalsState.items,
                key = { _, goal -> goal.id },
                contentType = { _, _ -> "goal_card" }
            ) { index, goal ->
                if (index >= goalsState.items.lastIndex - 5 && goalsState.hasMore && !goalsState.isLoading) {
                    LaunchedEffect(goalsState.items.size) {
                        viewModel.loadMoreGoals()
                    }
                }
                val metric = GoalMetric.fromName(goal.targetMetric)
                val status = GoalStatus.fromName(goal.status)
                val liveCurrentValue = when (metric) {
                    GoalMetric.INCOME -> uiState.home.totalEarnings
                    GoalMetric.PROJECTS -> uiState.home.totalVideosCompleted.toDouble()
                    GoalMetric.CLIENTS -> uiState.home.totalClients.toDouble()
                    GoalMetric.EDITING_DAYS -> uiState.home.totalEditingDays.toDouble()
                    GoalMetric.CUSTOM -> goal.currentValue
                }
                val target = goal.targetValue.coerceAtLeast(1.0)
                val progress = (liveCurrentValue / target).toFloat().coerceIn(0f, 1f)

                StudioCard(
                    onClick = {
                        editingGoal = goal
                        showGoalDialog = true
                    },
                    modifier = Modifier.testTag("goal_card_${goal.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(
                                    text = status.label,
                                    containerColor = when (status) {
                                        GoalStatus.COMPLETED -> StatusGreen.copy(alpha = 0.16f)
                                        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                        GoalStatus.PAUSED -> StatusAmber.copy(alpha = 0.16f)
                                    },
                                    contentColor = when (status) {
                                        GoalStatus.COMPLETED -> StatusGreen
                                        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                                        GoalStatus.PAUSED -> StatusAmber
                                    }
                                )
                                StatusPill(
                                    text = metric.label,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (goal.description.isNotBlank()) {
                                Text(
                                    text = goal.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                editingGoal = goal
                                showGoalDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit goal", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                if (uiState.settings.confirmBeforeDelete) {
                                    pendingDeleteGoal = goal
                                } else {
                                    viewModel.deleteGoal(goal.id)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete goal",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 1f || status == GoalStatus.COMPLETED) StatusGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val valueLabel = if (metric == GoalMetric.INCOME) {
                            "${Formatters.formatCurrency(liveCurrentValue, currency)} / ${Formatters.formatCurrency(target, currency)}"
                        } else {
                            "${liveCurrentValue.toInt()} / ${target.toInt()}"
                        }
                        Text(
                            text = valueLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = goal.deadlineEpochDay?.let { "Target ${Formatters.formatDate(it, dateFormat)}" } ?: "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "goals_pagination") {
            PaginationFooter(
                shownCount = goalsState.items.size,
                totalCount = goalsState.totalCount,
                hasMore = goalsState.hasMore,
                isLoading = goalsState.isLoading,
                itemLabel = "goals",
                onLoadMore = { viewModel.loadMoreGoals() }
            )
        }
    }

    if (showGoalDialog) {
        GoalFormDialog(
            existing = editingGoal,
            uiState = uiState,
            onSave = { entity ->
                viewModel.saveGoal(entity) { showGoalDialog = false }
            },
            onDismiss = { showGoalDialog = false }
        )
    }

    pendingDeleteGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete Goal?",
            message = "Delete career goal '${goal.title}'?",
            onConfirm = {
                viewModel.deleteGoal(goal.id)
                pendingDeleteGoal = null
            },
            onDismiss = { pendingDeleteGoal = null }
        )
    }
}

@Composable
private fun GoalFormDialog(
    existing: CareerGoalEntity?,
    uiState: MainUiState,
    onSave: (CareerGoalEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var metric by remember { mutableStateOf(GoalMetric.fromName(existing?.targetMetric)) }
    var currentValText by remember {
        mutableStateOf(
            existing?.currentValue?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "0"
        )
    }
    var targetValText by remember {
        mutableStateOf(
            existing?.targetValue?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "10"
        )
    }
    var deadline by remember { mutableStateOf(existing?.deadlineEpochDay) }
    var status by remember { mutableStateOf(GoalStatus.fromName(existing?.status)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Career Goal" else "Edit Career Goal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Goal Title *") },
                    placeholder = { Text("e.g. Reach ₹1,00,000 Monthly Revenue") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )
                DropdownSelectorField(
                    label = "Tracked Metric",
                    selectedLabel = metric.label,
                    options = GoalMetric.entries.map { it to it.label },
                    onOptionSelected = { metric = it }
                )
                if (metric == GoalMetric.CUSTOM) {
                    OutlinedTextField(
                        value = currentValText,
                        onValueChange = { currentValText = it; error = null },
                        label = { Text("Current Progress") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = targetValText,
                    onValueChange = { targetValText = it; error = null },
                    label = { Text("Target Value *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                )
                DatePickerField(
                    label = "Target Date (Optional)",
                    epochDay = deadline,
                    dateFormat = dateFormat,
                    allowClear = true,
                    onDateSelected = { deadline = it }
                )
                DropdownSelectorField(
                    label = "Status",
                    selectedLabel = status.label,
                    options = GoalStatus.entries.map { it to it.label },
                    onOptionSelected = { status = it }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Milestone Notes") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "Goal title is required."
                        return@Button
                    }
                    val target = targetValText.toDoubleOrNull()
                    if (target == null || target <= 0.0) {
                        error = "Target value must be greater than zero."
                        return@Button
                    }
                    val current = currentValText.toDoubleOrNull() ?: 0.0
                    onSave(
                        CareerGoalEntity(
                            id = existing?.id ?: 0L,
                            title = title.trim(),
                            description = description.trim(),
                            targetMetric = metric.name,
                            currentValue = current,
                            targetValue = target,
                            deadlineEpochDay = deadline,
                            status = status.name,
                            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("goal_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
