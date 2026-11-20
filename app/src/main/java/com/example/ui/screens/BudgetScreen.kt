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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.dao.ExpenseSummaryRow
import com.example.data.local.dao.IncomeSummaryRow
import com.example.data.local.entity.ExpenseTransactionEntity
import com.example.data.local.entity.IncomeTransactionEntity
import com.example.data.model.DateFormatOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinancialDateFilter
import com.example.data.model.PaymentMethod
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PaginationFooter
import com.example.ui.components.PickerItem
import com.example.ui.components.SearchableEntityPickerField
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    initialDialogType: String? = null,
    onConsumeInitialDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val budgetState by viewModel.budgetListState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = uiState.today
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    var ledgerTab by remember { mutableStateOf("Income") }
    var customStartEpoch by remember(budgetState.customStartEpoch, today) {
        mutableStateOf(budgetState.customStartEpoch ?: today.withDayOfMonth(1).toEpochDay())
    }
    var customEndEpoch by remember(budgetState.customEndEpoch, today) {
        mutableStateOf(budgetState.customEndEpoch ?: today.toEpochDay())
    }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    var editingIncome by remember { mutableStateOf<IncomeTransactionEntity?>(null) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseTransactionEntity?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    var pendingDeleteIncome by remember { mutableStateOf<IncomeSummaryRow?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseSummaryRow?>(null) }

    LaunchedEffect(initialDialogType) {
        when (initialDialogType) {
            "income" -> {
                ledgerTab = "Income"
                editingIncome = null
                showIncomeDialog = true
                onConsumeInitialDialog()
            }
            "expense" -> {
                ledgerTab = "Expenses"
                editingExpense = null
                showExpenseDialog = true
                onConsumeInitialDialog()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Financial Filter Chips
        item(key = "budget_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = budgetState.dateFilter == filter,
                        onClick = {
                            if (filter == FinancialDateFilter.CUSTOM) {
                                showCustomRangeDialog = true
                            } else {
                                viewModel.updateBudgetFilter(dateFilter = filter)
                            }
                        },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("budget_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Financial Summary Cards (SQL-computed totals over unlimited rows)
        item(key = "budget_summary") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = if (budgetState.dateFilter == FinancialDateFilter.ALL_TIME) "Total Income" else "${budgetState.dateFilter.label} Income",
                        value = Formatters.formatCurrency(budgetState.periodIncome, currency),
                        icon = Icons.Default.Payments,
                        accentColor = StatusGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_total_income")
                    )
                    MetricTile(
                        label = if (budgetState.dateFilter == FinancialDateFilter.ALL_TIME) "Total Expenses" else "${budgetState.dateFilter.label} Expenses",
                        value = Formatters.formatCurrency(budgetState.periodExpenses, currency),
                        icon = Icons.Default.ReceiptLong,
                        accentColor = StatusRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_total_expenses")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Net Balance",
                        value = Formatters.formatCurrency(budgetState.periodNet, currency),
                        subtitle = "Income - Expenses",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (budgetState.periodNet >= 0) MaterialTheme.colorScheme.primary else StatusRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_net_balance")
                    )
                    MetricTile(
                        label = "Pending Payments",
                        value = Formatters.formatCurrency(uiState.home.pendingClientPayments, currency),
                        subtitle = "Active project dues",
                        icon = Icons.Default.WarningAmber,
                        accentColor = StatusAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_pending_payments")
                    )
                }

                // Monthly & Yearly Quick Strip
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("This Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "+${Formatters.formatCurrency(uiState.home.currentMonthEarnings, currency)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = StatusGreen
                            )
                            Text(
                                "-${Formatters.formatCurrency(uiState.home.currentMonthExpenses, currency)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = StatusRed
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Year ${today.year}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "+${Formatters.formatCurrency(budgetState.yearlyIncome, currency)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = StatusGreen
                            )
                            Text(
                                "-${Formatters.formatCurrency(budgetState.yearlyExpenses, currency)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = StatusRed
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        item(key = "budget_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        editingIncome = null
                        showIncomeDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("budget_add_income_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Income")
                }
                FilledTonalButton(
                    onClick = {
                        editingExpense = null
                        showExpenseDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("budget_add_expense_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense")
                }
            }
        }

        // Income / Expenses Switcher
        item(key = "budget_tabs") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = ledgerTab == "Income",
                    onClick = { ledgerTab = "Income" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("budget_tab_income")
                ) {
                    Text("Income (${budgetState.incomeTotalCount})")
                }
                SegmentedButton(
                    selected = ledgerTab == "Expenses",
                    onClick = { ledgerTab = "Expenses" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("budget_tab_expenses")
                ) {
                    Text("Expenses (${budgetState.expenseTotalCount})")
                }
            }
        }

        if (ledgerTab == "Income") {
            if (budgetState.incomeItems.isEmpty() && !budgetState.isLoading) {
                item(key = "budget_income_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = "No income records found",
                        subtitle = "Record client payments or other editing revenue here. Only recorded payments count as income.",
                        actionLabel = "Add Income",
                        actionTag = "budget_empty_add_income",
                        onActionClick = {
                            editingIncome = null
                            showIncomeDialog = true
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = budgetState.incomeItems,
                    key = { _, tx -> tx.id },
                    contentType = { _, _ -> "income_card" }
                ) { index, tx ->
                    if (index >= budgetState.incomeItems.lastIndex - 5 && budgetState.incomeHasMore && !budgetState.isLoading) {
                        LaunchedEffect(budgetState.incomeItems.size) {
                            viewModel.loadMoreBudgetIncome()
                        }
                    }
                    StudioCard(modifier = Modifier.testTag("income_card_${tx.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.source,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = buildString {
                                    append(Formatters.formatDate(tx.dateEpochDay, dateFormat))
                                    append(" · ${PaymentMethod.fromName(tx.paymentMethod).label}")
                                    if (tx.clientNameSnapshot.isNotBlank()) append(" · ${tx.clientNameSnapshot}")
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tx.notesPreview.isNotBlank()) {
                                    Text(
                                        text = tx.notesPreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${Formatters.formatCurrency(tx.amount, currency)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = StatusGreen
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val full = viewModel.getIncomeByIdOnce(tx.id)
                                                if (full != null) {
                                                    editingIncome = full
                                                    showIncomeDialog = true
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit income", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (uiState.settings.confirmBeforeDelete) {
                                                pendingDeleteIncome = tx
                                            } else {
                                                viewModel.deleteIncome(tx.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete income",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "budget_income_pagination") {
                    PaginationFooter(
                        shownCount = budgetState.incomeItems.size,
                        totalCount = budgetState.incomeTotalCount,
                        hasMore = budgetState.incomeHasMore,
                        isLoading = budgetState.isLoading,
                        itemLabel = "income entries",
                        onLoadMore = { viewModel.loadMoreBudgetIncome() }
                    )
                }
            }
        } else {
            if (budgetState.expenseItems.isEmpty() && !budgetState.isLoading) {
                item(key = "budget_expenses_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = "No expenses recorded",
                        subtitle = "Track software subscriptions, editing assets, internet, or hardware upgrades.",
                        actionLabel = "Add Expense",
                        actionTag = "budget_empty_add_expense",
                        onActionClick = {
                            editingExpense = null
                            showExpenseDialog = true
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = budgetState.expenseItems,
                    key = { _, ex -> ex.id },
                    contentType = { _, _ -> "expense_card" }
                ) { index, ex ->
                    if (index >= budgetState.expenseItems.lastIndex - 5 && budgetState.expenseHasMore && !budgetState.isLoading) {
                        LaunchedEffect(budgetState.expenseItems.size) {
                            viewModel.loadMoreBudgetExpenses()
                        }
                    }
                    StudioCard(modifier = Modifier.testTag("expense_card_${ex.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPill(
                                    text = ExpenseCategory.fromName(ex.category).label,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ex.description,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = Formatters.formatDate(ex.dateEpochDay, dateFormat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (ex.notesPreview.isNotBlank()) {
                                    Text(
                                        text = ex.notesPreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "-${Formatters.formatCurrency(ex.amount, currency)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = StatusRed
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val full = viewModel.getExpenseByIdOnce(ex.id)
                                                if (full != null) {
                                                    editingExpense = full
                                                    showExpenseDialog = true
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit expense", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (uiState.settings.confirmBeforeDelete) {
                                                pendingDeleteExpense = ex
                                            } else {
                                                viewModel.deleteExpense(ex.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete expense",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "budget_expenses_pagination") {
                    PaginationFooter(
                        shownCount = budgetState.expenseItems.size,
                        totalCount = budgetState.expenseTotalCount,
                        hasMore = budgetState.expenseHasMore,
                        isLoading = budgetState.isLoading,
                        itemLabel = "expense entries",
                        onLoadMore = { viewModel.loadMoreBudgetExpenses() }
                    )
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("Custom Date Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DatePickerField(
                        label = "Start Date",
                        epochDay = customStartEpoch,
                        dateFormat = dateFormat,
                        onDateSelected = { if (it != null) customStartEpoch = it }
                    )
                    DatePickerField(
                        label = "End Date",
                        epochDay = customEndEpoch,
                        dateFormat = dateFormat,
                        onDateSelected = { if (it != null) customEndEpoch = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateBudgetFilter(
                        dateFilter = FinancialDateFilter.CUSTOM,
                        customStartEpoch = customStartEpoch,
                        customEndEpoch = customEndEpoch
                    )
                    showCustomRangeDialog = false
                }) { Text("Apply") }
            }
        )
    }

    if (showIncomeDialog) {
        IncomeFormDialog(
            existing = editingIncome,
            uiState = uiState,
            viewModel = viewModel,
            onSave = { tx ->
                viewModel.saveIncome(tx) { showIncomeDialog = false }
            },
            onDismiss = { showIncomeDialog = false }
        )
    }

    if (showExpenseDialog) {
        ExpenseFormDialog(
            existing = editingExpense,
            uiState = uiState,
            onSave = { ex ->
                viewModel.saveExpense(ex) { showExpenseDialog = false }
            },
            onDismiss = { showExpenseDialog = false }
        )
    }

    pendingDeleteIncome?.let { tx ->
        ConfirmDeleteDialog(
            title = "Delete Income Entry?",
            message = "Delete '${tx.source}' (${Formatters.formatCurrency(tx.amount, currency)})? Linked project payments will recalculate automatically.",
            onConfirm = {
                viewModel.deleteIncome(tx.id)
                pendingDeleteIncome = null
            },
            onDismiss = { pendingDeleteIncome = null }
        )
    }

    pendingDeleteExpense?.let { ex ->
        ConfirmDeleteDialog(
            title = "Delete Expense Entry?",
            message = "Delete '${ex.description}' (${Formatters.formatCurrency(ex.amount, currency)})?",
            onConfirm = {
                viewModel.deleteExpense(ex.id)
                pendingDeleteExpense = null
            },
            onDismiss = { pendingDeleteExpense = null }
        )
    }
}

@Composable
private fun IncomeFormDialog(
    existing: IncomeTransactionEntity?,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onSave: (IncomeTransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    var source by remember { mutableStateOf(existing?.source.orEmpty()) }
    var selectedClientId by remember { mutableStateOf(existing?.clientId) }
    var selectedClientLabel by remember { mutableStateOf(existing?.clientNameSnapshot?.ifBlank { "No Client" } ?: "No Client") }
    var selectedProjectId by remember { mutableStateOf(existing?.projectId) }
    var selectedProjectLabel by remember { mutableStateOf(existing?.projectTitleSnapshot?.ifBlank { "No Linked Project" } ?: "No Linked Project") }
    var amountText by remember {
        mutableStateOf(
            existing?.amount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
        )
    }
    var dateEpoch by remember { mutableStateOf(existing?.dateEpochDay ?: uiState.today.toEpochDay()) }
    var method by remember { mutableStateOf(PaymentMethod.fromName(existing?.paymentMethod)) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedClientId) {
        val cid = selectedClientId
        if (cid != null && selectedClientLabel == "No Client") {
            selectedClientLabel = viewModel.getClientFullSafe(cid)?.name ?: "Client #$cid"
        }
    }

    LaunchedEffect(selectedProjectId) {
        val pid = selectedProjectId
        if (pid != null && selectedProjectLabel == "No Linked Project") {
            selectedProjectLabel = viewModel.getProjectFullSafe(pid)?.title ?: "Project #$pid"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Income" else "Edit Income") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it; error = null },
                    label = { Text("Income Source *") },
                    placeholder = { Text("e.g. Client Reel Payment") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_source_input")
                )
                SearchableEntityPickerField(
                    label = "Client (Optional)",
                    selectedLabel = if (selectedClientId == null) "No Client" else selectedClientLabel,
                    noneOptionLabel = "No Client",
                    onSelectNone = {
                        selectedClientId = null
                        selectedClientLabel = "No Client"
                    },
                    onSearch = { q ->
                        viewModel.searchClientsForPicker(q).map {
                            PickerItem(value = it.id, title = it.name, subtitle = it.platform)
                        }
                    },
                    onItemSelected = { picked ->
                        selectedClientId = picked.value
                        selectedClientLabel = picked.title
                    }
                )
                SearchableEntityPickerField(
                    label = "Related Project (Optional)",
                    selectedLabel = if (selectedProjectId == null) "No Linked Project" else selectedProjectLabel,
                    noneOptionLabel = "No Linked Project",
                    onSelectNone = {
                        selectedProjectId = null
                        selectedProjectLabel = "No Linked Project"
                    },
                    onSearch = { q ->
                        viewModel.searchProjectsForPicker(q, selectedClientId).map { p ->
                            PickerItem(
                                value = p,
                                title = p.title,
                                subtitle = buildString {
                                    if (!p.clientName.isNullOrBlank()) append("${p.clientName} · ")
                                    append("Paid ${Formatters.formatCurrency(p.amountPaid, currency)} / ${Formatters.formatCurrency(p.projectAmount, currency)}")
                                }
                            )
                        }
                    },
                    onItemSelected = { picked ->
                        val p = picked.value
                        selectedProjectId = p.id
                        selectedProjectLabel = p.title
                        if (selectedClientId == null && p.clientId != null) {
                            selectedClientId = p.clientId
                            selectedClientLabel = p.clientName ?: "Client #${p.clientId}"
                        }
                        if (source.isBlank()) source = "Payment: ${p.title}"
                    }
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_amount_input")
                )
                DatePickerField(
                    label = "Date",
                    epochDay = dateEpoch,
                    dateFormat = dateFormat,
                    onDateSelected = { if (it != null) dateEpoch = it }
                )
                DropdownSelectorField(
                    label = "Payment Method",
                    selectedLabel = method.label,
                    options = PaymentMethod.entries.map { it to it.label },
                    onOptionSelected = { method = it }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
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
                    if (source.isBlank()) {
                        error = "Income source is required."
                        return@Button
                    }
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        error = "Amount must be greater than zero."
                        return@Button
                    }
                    onSave(
                        IncomeTransactionEntity(
                            id = existing?.id ?: 0L,
                            source = source.trim(),
                            clientId = selectedClientId,
                            projectId = selectedProjectId,
                            clientNameSnapshot = if (selectedClientId != null) selectedClientLabel else existing?.clientNameSnapshot.orEmpty(),
                            projectTitleSnapshot = if (selectedProjectId != null) selectedProjectLabel else existing?.projectTitleSnapshot.orEmpty(),
                            amount = amt,
                            dateEpochDay = dateEpoch,
                            paymentMethod = method.name,
                            notes = notes.trim(),
                            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("income_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExpenseFormDialog(
    existing: ExpenseTransactionEntity?,
    uiState: MainUiState,
    onSave: (ExpenseTransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = uiState.settings.currencySymbol
    val dateFormat = DateFormatOption.fromName(uiState.settings.dateFormat)

    var category by remember { mutableStateOf(ExpenseCategory.fromName(existing?.category)) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var amountText by remember {
        mutableStateOf(
            existing?.amount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
        )
    }
    var dateEpoch by remember { mutableStateOf(existing?.dateEpochDay ?: uiState.today.toEpochDay()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DropdownSelectorField(
                    label = "Expense Category",
                    selectedLabel = category.label,
                    options = ExpenseCategory.entries.map { it to it.label },
                    onOptionSelected = { category = it }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; error = null },
                    label = { Text("Description *") },
                    placeholder = { Text("e.g. Adobe Creative Cloud") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_description_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )
                DatePickerField(
                    label = "Date",
                    epochDay = dateEpoch,
                    dateFormat = dateFormat,
                    onDateSelected = { if (it != null) dateEpoch = it }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
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
                    if (description.isBlank()) {
                        error = "Description is required."
                        return@Button
                    }
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        error = "Amount must be greater than zero."
                        return@Button
                    }
                    onSave(
                        ExpenseTransactionEntity(
                            id = existing?.id ?: 0L,
                            category = category.name,
                            description = description.trim(),
                            amount = amt,
                            dateEpochDay = dateEpoch,
                            notes = notes.trim(),
                            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("expense_save_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
