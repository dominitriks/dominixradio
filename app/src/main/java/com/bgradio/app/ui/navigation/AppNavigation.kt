package com.bgradio.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.MiniPlayer
import com.bgradio.app.ui.components.StationActionsHost
import com.bgradio.app.ui.components.UpdateDialog
import com.bgradio.app.ui.screens.FavoritesScreen
import com.bgradio.app.ui.screens.HomeScreen
import com.bgradio.app.ui.screens.PlayerScreen
import com.bgradio.app.ui.screens.SettingsScreen
import com.bgradio.app.ui.screens.StationsScreen
import com.bgradio.app.ui.screens.StatsScreen

private object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val STATIONS = "stations?search={search}"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val STATS = "stats"

    fun stations(search: Boolean = false) = "stations?search=$search"
}

private data class Tab(val route: String, val navRoute: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, Routes.HOME, "Начало", Icons.Rounded.Home),
    Tab(Routes.FAVORITES, Routes.FAVORITES, "Любими", Icons.Rounded.Favorite),
    Tab(Routes.STATIONS, Routes.stations(), "Радиостанции", Icons.Rounded.Radio),
    Tab(Routes.SETTINGS, Routes.SETTINGS, "Настройки", Icons.Rounded.Settings),
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val player by vm.playerState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val showBars = currentRoute != Routes.PLAYER

    // Show each new playback error once as a snackbar.
    var lastShownError by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(player.errorSeq) {
        val msg = player.errorMessage
        if (player.errorSeq != 0 && player.errorSeq != lastShownError && msg != null) {
            lastShownError = player.errorSeq
            val result = snackbar.showSnackbar(msg, actionLabel = "Опитай отново", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) vm.retry()
        }
    }

    val updateDialog by vm.updateDialog.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()
    updateDialog?.let {
        UpdateDialog(
            result = it.result,
            installed = vm.installedVersion,
            download = download,
            onDownload = vm::downloadUpdate,
            onInstall = vm::installUpdate,
            onOpenInstallSettings = { vm.openInstallPermissionSettings() },
            onCancelDownload = vm::cancelUpdateDownload,
            onDismiss = vm::dismissUpdateDialog,
        )
    }
    StationActionsHost(vm)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBars,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    AnimatedVisibility(
                        visible = player.hasStation,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        MiniPlayer(
                            state = player,
                            onOpen = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } },
                            onPlayPause = { if (player.playWhenReady) vm.pause() else vm.resume() },
                            onNext = vm::next,
                            onStop = vm::stop,
                        )
                    }
                    BottomBar(nav, currentRoute)
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    contentPadding = padding,
                    onOpenCategory = { genre ->
                        vm.setQuery("")
                        vm.selectGenre(genre)
                        nav.navigateToTab(Routes.stations())
                    },
                    onOpenSearch = {
                        vm.selectGenre(null)
                        nav.navigateToTab(Routes.stations(search = true))
                    },
                    onOpenFavorites = { nav.navigateToTab(Routes.FAVORITES) },
                    onOpenStats = { nav.navigate(Routes.STATS) { launchSingleTop = true } },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(vm, padding, onOpenPlayer = { nav.navigate(Routes.PLAYER) })
            }
            composable(
                Routes.STATIONS,
                arguments = listOf(navArgument("search") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                StationsScreen(
                    vm = vm,
                    contentPadding = padding,
                    focusSearch = entry.arguments?.getBoolean("search") == true,
                    onOpenPlayer = { nav.navigate(Routes.PLAYER) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm, padding, onOpenStats = { nav.navigate(Routes.STATS) { launchSingleTop = true } })
            }
            composable(Routes.STATS) { StatsScreen(vm, padding, onBack = { nav.popBackStack() }) }
            composable(
                Routes.PLAYER,
                enterTransition = { slideInVertically(tween(320)) { it } + fadeIn() },
                exitTransition = { fadeOut(tween(150)) },
                popExitTransition = { slideOutVertically(tween(280)) { it } + fadeOut() },
            ) {
                PlayerScreen(vm, onClose = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(nav: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { if (currentRoute != tab.route) nav.navigateToTab(tab.navRoute) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
