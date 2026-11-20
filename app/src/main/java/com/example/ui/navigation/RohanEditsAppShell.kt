package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.ThemeMode
import com.example.ui.screens.AboutAppScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.ClientFormScreen
import com.example.ui.screens.ClientsHubScreen
import com.example.ui.screens.EditorInfoScreen
import com.example.ui.screens.ExportDataScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreMenuScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectFormScreen
import com.example.ui.theme.RohanEditsTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.util.DeadlineNotificationHelper

object Routes {
    const val HOME = "home"
    const val CLIENTS = "clients"
    const val CALENDAR = "calendar"
    const val BUDGET = "budget"
    const val MORE = "more"

    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val CLIENT_FORM = "client_form?clientId={clientId}"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val PROJECT_FORM = "project_form?projectId={projectId}&clientId={clientId}"

    const val ANALYTICS = "analytics"
    const val EDITOR_INFO = "editor_info"
    const val SETTINGS = "settings"
    const val BACKUP_RESTORE = "backup_restore"
    const val EXPORT_DATA = "export_data"
    const val ABOUT_APP = "about_app"
    const val GLOBAL_SEARCH = "global_search"

    fun clientDetail(clientId: Long) = "client_detail/$clientId"
    fun clientForm(clientId: Long? = null) = if (clientId != null) "client_form?clientId=$clientId" else "client_form"
    fun projectDetail(projectId: Long) = "project_detail/$projectId"
    fun projectForm(projectId: Long? = null, clientId: Long? = null): String {
        val params = buildList {
            if (projectId != null) add("projectId=$projectId")
            if (clientId != null) add("clientId=$clientId")
        }
        return if (params.isEmpty()) "project_form" else "project_form?${params.joinToString("&")}"
    }
}

data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelTabs = listOf(
    TopLevelTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelTab(Routes.CLIENTS, "Clients", Icons.Filled.Group, Icons.Outlined.Group, "nav_clients"),
    TopLevelTab(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    TopLevelTab(Routes.BUDGET, "Budget", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_budget"),
    TopLevelTab(Routes.MORE, "More", Icons.Filled.GridView, Icons.Outlined.GridView, "nav_more")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RohanEditsAppShell(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode = ThemeMode.fromName(uiState.settings.themeMode)
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Shared cross-tab intents for Clients/Projects hub & Budget quick actions
    var showProjectsInHub by remember { mutableStateOf(false) }
    var hubClientFilterId by remember { mutableStateOf<Long?>(null) }
    var pendingBudgetDialog by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.settings.deadlineRemindersEnabled, uiState.projectsWithClient.size) {
        if (uiState.settings.deadlineRemindersEnabled && uiState.projectsWithClient.isNotEmpty()) {
            DeadlineNotificationHelper.notifyUpcomingDeadlines(
                context = context,
                projects = uiState.projectsWithClient.map { it.project },
                today = uiState.today
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    RohanEditsTheme(themeMode = themeMode) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        val isTopLevel = topLevelTabs.any { it.route == currentRoute }

        val topBarTitle = when {
            currentRoute == Routes.HOME -> "Rohan Edits"
            currentRoute == Routes.CLIENTS -> if (showProjectsInHub) "Projects" else "Clients"
            currentRoute == Routes.CALENDAR -> "Editing Calendar"
            currentRoute == Routes.BUDGET -> "Budget & Ledger"
            currentRoute == Routes.MORE -> "More"
            currentRoute?.startsWith("client_detail") == true -> "Client Profile"
            currentRoute?.startsWith("client_form") == true -> {
                val cid = navBackStackEntry?.arguments?.getLong("clientId") ?: -1L
                if (cid > 0L) "Edit Client" else "Add Client"
            }
            currentRoute?.startsWith("project_detail") == true -> "Project Details"
            currentRoute?.startsWith("project_form") == true -> {
                val pid = navBackStackEntry?.arguments?.getLong("projectId") ?: -1L
                if (pid > 0L) "Edit Project" else "Add Project"
            }
            currentRoute == Routes.ANALYTICS -> "Analytics Dashboard"
            currentRoute == Routes.EDITOR_INFO -> "Editor Info"
            currentRoute == Routes.SETTINGS -> "App Settings"
            currentRoute == Routes.BACKUP_RESTORE -> "Data Backup & Restore"
            currentRoute == Routes.EXPORT_DATA -> "Export Data"
            currentRoute == Routes.ABOUT_APP -> "About App"
            currentRoute == Routes.GLOBAL_SEARCH -> "Search"
            else -> "Rohan Edits"
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("topbar_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (isTopLevel) {
                            IconButton(
                                onClick = { navController.navigate(Routes.GLOBAL_SEARCH) },
                                modifier = Modifier.testTag("topbar_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Global Search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == Routes.CLIENTS) {
                                        showProjectsInHub = false
                                    }
                                    navigateTopLevel(tab.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            uiState = uiState,
                            onAddClient = { navController.navigate(Routes.clientForm()) },
                            onAddProject = { navController.navigate(Routes.projectForm()) },
                            onAddIncome = {
                                pendingBudgetDialog = "income"
                                navigateTopLevel(Routes.BUDGET)
                            },
                            onAddExpense = {
                                pendingBudgetDialog = "expense"
                                navigateTopLevel(Routes.BUDGET)
                            },
                            onOpenCalendar = { navigateTopLevel(Routes.CALENDAR) },
                            onViewAllProjects = {
                                showProjectsInHub = true
                                hubClientFilterId = null
                                navigateTopLevel(Routes.CLIENTS)
                            },
                            onOpenProject = { projectId ->
                                navController.navigate(Routes.projectDetail(projectId))
                            }
                        )
                    }

                    composable(Routes.CLIENTS) {
                        ClientsHubScreen(
                            uiState = uiState,
                            showProjectsTab = showProjectsInHub,
                            onTabChange = { showProjectsInHub = it },
                            onAddClient = { navController.navigate(Routes.clientForm()) },
                            onOpenClient = { clientId ->
                                navController.navigate(Routes.clientDetail(clientId))
                            },
                            onAddProject = { navController.navigate(Routes.projectForm()) },
                            onOpenProject = { projectId ->
                                navController.navigate(Routes.projectDetail(projectId))
                            },
                            initialClientFilterId = hubClientFilterId,
                            onClearClientFilter = { hubClientFilterId = null }
                        )
                    }

                    composable(Routes.CALENDAR) {
                        CalendarScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.BUDGET) {
                        BudgetScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            initialDialogType = pendingBudgetDialog,
                            onConsumeInitialDialog = { pendingBudgetDialog = null }
                        )
                    }

                    composable(Routes.MORE) {
                        MoreMenuScreen(
                            uiState = uiState,
                            onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                            onOpenEditorInfo = { navController.navigate(Routes.EDITOR_INFO) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) },
                            onOpenExportData = { navController.navigate(Routes.EXPORT_DATA) },
                            onOpenAboutApp = { navController.navigate(Routes.ABOUT_APP) }
                        )
                    }

                    composable(
                        route = Routes.CLIENT_DETAIL,
                        arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                    ) { entry ->
                        val clientId = entry.arguments?.getLong("clientId") ?: 0L
                        ClientDetailScreen(
                            clientId = clientId,
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditClient = { id -> navController.navigate(Routes.clientForm(id)) },
                            onAddProjectForClient = { id -> navController.navigate(Routes.projectForm(clientId = id)) },
                            onViewAllClientProjects = { id ->
                                hubClientFilterId = id
                                showProjectsInHub = true
                                navigateTopLevel(Routes.CLIENTS)
                            },
                            onOpenProject = { pid -> navController.navigate(Routes.projectDetail(pid)) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.CLIENT_FORM,
                        arguments = listOf(
                            navArgument("clientId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { entry ->
                        val cid = entry.arguments?.getLong("clientId")?.takeIf { it > 0L }
                        ClientFormScreen(
                            clientId = cid,
                            uiState = uiState,
                            viewModel = viewModel,
                            onSaved = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.PROJECT_DETAIL,
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                    ) { entry ->
                        val projectId = entry.arguments?.getLong("projectId") ?: 0L
                        ProjectDetailScreen(
                            projectId = projectId,
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditProject = { id -> navController.navigate(Routes.projectForm(projectId = id)) },
                            onOpenClient = { cid -> navController.navigate(Routes.clientDetail(cid)) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.PROJECT_FORM,
                        arguments = listOf(
                            navArgument("projectId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            },
                            navArgument("clientId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { entry ->
                        val pid = entry.arguments?.getLong("projectId")?.takeIf { it > 0L }
                        val cid = entry.arguments?.getLong("clientId")?.takeIf { it > 0L }
                        ProjectFormScreen(
                            projectId = pid,
                            preselectedClientId = cid,
                            uiState = uiState,
                            viewModel = viewModel,
                            onSaved = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.ANALYTICS) {
                        AnalyticsScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.EDITOR_INFO) {
                        EditorInfoScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.SETTINGS) {
                        AppSettingsScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.BACKUP_RESTORE) {
                        BackupRestoreScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.EXPORT_DATA) {
                        ExportDataScreen(uiState = uiState, viewModel = viewModel)
                    }

                    composable(Routes.ABOUT_APP) {
                        AboutAppScreen(uiState = uiState)
                    }

                    composable(Routes.GLOBAL_SEARCH) {
                        GlobalSearchScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenClient = { cid -> navController.navigate(Routes.clientDetail(cid)) },
                            onOpenProject = { pid -> navController.navigate(Routes.projectDetail(pid)) },
                            onOpenBudget = { navigateTopLevel(Routes.BUDGET) }
                        )
                    }
                }
            }
        }
    }
}
