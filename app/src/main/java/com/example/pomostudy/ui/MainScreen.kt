package com.example.pomostudy.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pomostudy.data.repository.AppSettings
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.ThemeMode
import com.example.pomostudy.ui.calendar.CalendarScreen
import com.example.pomostudy.ui.settings.SettingsScreen
import com.example.pomostudy.ui.stats.StatsScreen
import com.example.pomostudy.ui.tags.TagScreen
import com.example.pomostudy.ui.timer.TimerScreen
import com.example.pomostudy.ui.todo.TodoScreen
import kotlinx.coroutines.launch

/** 路由常量。 */
object Routes {
    const val TIMER = "timer"
    const val TODO = "todo"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val TAGS = "tags"
    const val SETTINGS = "settings"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.TIMER, "计时", Icons.Filled.Timer),
    BottomItem(Routes.TODO, "待办", Icons.Filled.Checklist),
    BottomItem(Routes.CALENDAR, "日历", Icons.Filled.CalendarMonth),
    BottomItem(Routes.STATS, "统计", Icons.Filled.PieChart)
)

private val mainRoutes = bottomItems.map { it.route }.toSet()

private fun titleFor(route: String): String = when (route) {
    Routes.TIMER -> "计时"
    Routes.TODO -> "待办"
    Routes.CALENDAR -> "日历"
    Routes.STATS -> "统计"
    Routes.TAGS -> "标签管理"
    Routes.SETTINGS -> "设置"
    else -> "PomoTic"
}

/** 根界面：底部 4 Tab 导航 + 顶栏（主题切换 / 标签 / 设置） + NavHost。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TIMER
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isMainRoute = currentRoute in mainRoutes

    NotificationPermissionRequester()

    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.MACARON -> false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titleFor(currentRoute)) },
                navigationIcon = {
                    if (!isMainRoute) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (isMainRoute) {
                        IconButton(onClick = { navController.navigate(Routes.TAGS) }) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "标签管理")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            container.settingsRepository.setThemeMode(
                                if (effectiveDark) ThemeMode.LIGHT else ThemeMode.DARK
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (effectiveDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "切换主题"
                        )
                    }
                    if (isMainRoute) {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMainRoute) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.TIMER) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TIMER,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally { it / 14 }
            },
            exitTransition = { fadeOut(animationSpec = tween(160)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = {
                fadeOut(animationSpec = tween(160)) + slideOutHorizontally { it / 14 }
            }
        ) {
            composable(Routes.TIMER) { TimerScreen(container = container, snackbarHostState = snackbarHostState) }
            composable(Routes.TODO) { TodoScreen(container = container) }
            composable(Routes.CALENDAR) { CalendarScreen(container = container) }
            composable(Routes.STATS) { StatsScreen(container = container) }
            composable(Routes.TAGS) { TagScreen(container = container) }
            composable(Routes.SETTINGS) { SettingsScreen(container = container) }
        }
    }
}

/** Android 13+ 通知权限申请（用于计时结束提醒与前台服务通知）。 */
@Composable
private fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 结果无需处理，未授权时结束提醒不会弹出 */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
