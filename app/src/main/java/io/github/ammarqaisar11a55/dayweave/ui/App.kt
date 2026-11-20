package io.github.ammarqaisar11a55.dayweave.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.ammarqaisar11a55.dayweave.core.Settings
import io.github.ammarqaisar11a55.dayweave.core.TaskInput
import io.github.ammarqaisar11a55.dayweave.ui.components.Actions
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.screens.AnalyticsScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.CalendarScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.FocusScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.HabitDetailScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.HabitEditorScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.HabitsScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.NewTaskSheet
import io.github.ammarqaisar11a55.dayweave.ui.screens.ProjectsScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.SettingsScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.TaskDetailScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.TasksScreen
import io.github.ammarqaisar11a55.dayweave.ui.screens.TodayScreen
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.DayweaveTheme
import io.github.ammarqaisar11a55.dayweave.update.UpdateCheck

sealed interface Destination {
    data object Focus : Destination
    data object NewTask : Destination
    data class Task(val id: Long) : Destination
    data class Habit(val id: Long) : Destination
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab("today", "Today", Icons.Filled.WbSunny),
    Tab("tasks", "Tasks", Icons.Filled.CheckCircle),
    Tab("habits", "Habits", Icons.Filled.Repeat),
    Tab("calendar", "Calendar", Icons.Filled.CalendarMonth),
    Tab("focus", "Focus", Icons.Filled.Timer),
)

private val Titles = mapOf(
    "today" to "Today", "tasks" to "Tasks", "habits" to "Habits", "calendar" to "Calendar", "focus" to "Focus",
    "projects" to "Projects", "analytics" to "Analytics", "settings" to "Settings",
    "task/{id}" to "Task", "habit/{id}" to "Habit", "habitEdit?id={id}" to "Habit",
)

/** Opens the new-task sheet from anywhere, with optional starting values. */
val LocalNewTask = staticCompositionLocalOf<(TaskInput?) -> Unit> { {} }

/** The app-wide settings, read once here and passed down. */
val LocalSettings = staticCompositionLocalOf { Settings() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(pending: Destination?, onHandled: () -> Unit) {
    val settings = rememberRpc<Settings>("settings.get") ?: Settings()
    DayweaveTheme(settings.theme) {
        val navigation = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val actions = remember { Actions(scope, snackbar) }
        var newTask by remember { mutableStateOf<TaskInput?>(null) }
        val entry by navigation.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: "today"
        val isTab = Tabs.any { it.route == route.substringBefore('?') }

        LaunchedEffect(pending) {
            when (pending) {
                Destination.Focus -> navigation.navigate("focus")
                Destination.NewTask -> newTask = TaskInput(title = "")
                is Destination.Task -> navigation.navigate("task/${pending.id}")
                is Destination.Habit -> navigation.navigate("habit/${pending.id}")
                null -> return@LaunchedEffect
            }
            onHandled()
        }

        CompositionLocalProvider(
            LocalActions provides actions,
            LocalSettings provides settings,
            LocalNewTask provides { defaults -> newTask = defaults ?: TaskInput(title = "") },
        ) {
            Scaffold(
                containerColor = Dw.colors.window,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        title = { Text(Titles[route] ?: Titles[route.substringBefore('?')] ?: "Dayweave") },
                        navigationIcon = {
                            if (!isTab) {
                                IconButton(onClick = { navigation.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = { if (isTab) MoreMenu(navigation) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Dw.colors.window),
                    )
                },
                bottomBar = { if (isTab) BottomBar(navigation, route) },
            ) { padding ->
                NavHost(navigation, startDestination = "today", modifier = Modifier.padding(padding)) {
                    composable("today") { TodayScreen(navigation) }
                    composable(
                        "tasks?view={view}&project={project}",
                        arguments = listOf(
                            navArgument("view") { defaultValue = "all" },
                            navArgument("project") { type = NavType.LongType; defaultValue = -1L },
                        ),
                    ) { back ->
                        TasksScreen(
                            navigation,
                            back.arguments?.getString("view") ?: "all",
                            back.arguments?.getLong("project")?.takeIf { it >= 0 },
                        )
                    }
                    composable("task/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { back ->
                        TaskDetailScreen(navigation, back.arguments?.getLong("id") ?: 0)
                    }
                    composable("habits") { HabitsScreen(navigation) }
                    composable("habit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { back ->
                        HabitDetailScreen(navigation, back.arguments?.getLong("id") ?: 0)
                    }
                    composable(
                        "habitEdit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                    ) { back ->
                        HabitEditorScreen(navigation, back.arguments?.getLong("id")?.takeIf { it >= 0 })
                    }
                    composable("calendar") { CalendarScreen(navigation) }
                    composable(
                        "focus?task={task}",
                        arguments = listOf(navArgument("task") { type = NavType.LongType; defaultValue = -1L }),
                    ) { back ->
                        FocusScreen(back.arguments?.getLong("task")?.takeIf { it >= 0 })
                    }
                    composable("projects") { ProjectsScreen(navigation) }
                    composable("analytics") { AnalyticsScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
            newTask?.let { defaults -> NewTaskSheet(defaults, onClose = { newTask = null }) }
            UpdateCheck()
        }
    }
}

@Composable
private fun BottomBar(navigation: NavHostController, route: String) {
    NavigationBar(containerColor = Dw.colors.card) {
        Tabs.forEach { tab ->
            val selected = route.substringBefore('?') == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navigation.navigate(tab.route) {
                        popUpTo("today") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Dw.colors.accentText,
                    selectedTextColor = Dw.colors.accentText,
                    indicatorColor = Dw.colors.accentSoft,
                ),
            )
        }
    }
}

@Composable
private fun MoreMenu(navigation: NavHostController) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        listOf(
            Triple("projects", "Projects", Icons.Filled.Folder),
            Triple("analytics", "Analytics", Icons.Filled.BarChart),
            Triple("settings", "Settings", Icons.Filled.Settings),
        ).forEach { (route, label, icon) ->
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { Icon(icon, contentDescription = null) },
                onClick = {
                    open = false
                    navigation.navigate(route)
                },
            )
        }
    }
}
