package de.rezeptbuch.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.rezeptbuch.app.R
import de.rezeptbuch.app.ui.category.CategoryRecipesScreen
import de.rezeptbuch.app.ui.cooking.CookingModeScreen
import de.rezeptbuch.app.ui.detail.RecipeDetailScreen
import de.rezeptbuch.app.ui.edit.RecipeEditScreen
import de.rezeptbuch.app.ui.home.HomeScreen
import de.rezeptbuch.app.ui.recipes.RecipesScreen
import de.rezeptbuch.app.ui.settings.SettingsScreen
import kotlin.reflect.KClass

private enum class TopLevel(
    val routeClass: KClass<out Route>,
    val route: Route,
    val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(Route.Home::class, Route.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    RECIPES(Route.Recipes::class, Route.Recipes(), R.string.nav_recipes, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS(Route.Settings::class, Route.Settings, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}

private fun NavDestination?.isTopLevel(item: TopLevel) = this?.hasRoute(item.routeClass) == true

private fun NavHostController.navigateToTab(route: Route) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// --- Übergänge: Unterseiten gleiten weich herein, Reiter blenden über
private const val DURATION = 320

private val enterDetail: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(DURATION)) + scaleIn(tween(DURATION, easing = FastOutSlowInEasing), initialScale = 0.94f) +
        slideInVertically(tween(DURATION, easing = FastOutSlowInEasing)) { it / 12 }
}
private val exitDetail: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(DURATION / 2)) + scaleOut(tween(DURATION), targetScale = 0.94f) +
        slideOutVertically(tween(DURATION)) { it / 12 }
}
private val fadeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(DURATION)) }
private val fadeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(DURATION / 2)) }

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TopLevel.entries.any { destination.isTopLevel(it) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    TopLevel.entries.forEach { item ->
                        val selected = destination.isTopLevel(item)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.navigateToTab(item.route) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, null) },
                            label = { Text(stringResource(item.label), style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(
                navController = nav,
                startDestination = Route.Home,
                enterTransition = enterDetail,
                exitTransition = fadeExit,
                popEnterTransition = fadeEnter,
                popExitTransition = exitDetail,
            ) {
                composable<Route.Home>(enterTransition = fadeEnter, popEnterTransition = fadeEnter) {
                    HomeScreen(
                        onOpenRecipe = { nav.navigate(Route.RecipeDetail(it)) },
                        onOpenCategory = { nav.navigate(Route.CategoryRecipes(it)) },
                        onAddRecipe = { nav.navigate(Route.RecipeEdit()) },
                        onSearch = {
                            // Ohne restoreState, damit das Suchfeld sicher den Fokus bekommt
                            nav.navigate(Route.Recipes(focusSearch = true)) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onShowAllRecipes = { nav.navigateToTab(Route.Recipes()) },
                    )
                }
                composable<Route.Recipes>(enterTransition = fadeEnter, popEnterTransition = fadeEnter) {
                    RecipesScreen(
                        snackbarHostState = snackbar,
                        onOpenRecipe = { nav.navigate(Route.RecipeDetail(it)) },
                        onOpenCategory = { nav.navigate(Route.CategoryRecipes(it)) },
                        onAddRecipe = { nav.navigate(Route.RecipeEdit()) },
                    )
                }
                composable<Route.Settings>(enterTransition = fadeEnter, popEnterTransition = fadeEnter) {
                    SettingsScreen()
                }
                composable<Route.CategoryRecipes> {
                    CategoryRecipesScreen(
                        onBack = { nav.popBackStack() },
                        onOpenRecipe = { nav.navigate(Route.RecipeDetail(it)) },
                        onAddRecipe = { categoryId -> nav.navigate(Route.RecipeEdit(categoryId = categoryId)) },
                    )
                }
                composable<Route.RecipeDetail> {
                    RecipeDetailScreen(
                        onBack = { nav.popBackStack() },
                        onEdit = { id -> nav.navigate(Route.RecipeEdit(id)) },
                        onStartCooking = { id, servings -> nav.navigate(Route.CookingMode(id, servings)) },
                    )
                }
                composable<Route.RecipeEdit> {
                    RecipeEditScreen(
                        onBack = { nav.popBackStack() },
                        onSaved = { id, isNew ->
                            if (isNew) {
                                // Neues Rezept: Edit-Screen durch Detailansicht ersetzen
                                nav.navigate(Route.RecipeDetail(id)) {
                                    popUpTo<Route.RecipeEdit> { inclusive = true }
                                }
                            } else {
                                nav.popBackStack()
                            }
                        },
                    )
                }
                composable<Route.CookingMode> {
                    CookingModeScreen(onClose = { nav.popBackStack() })
                }
            }
        }
    }
}
