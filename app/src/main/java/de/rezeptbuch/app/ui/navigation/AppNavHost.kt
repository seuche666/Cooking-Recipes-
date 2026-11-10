package de.rezeptbuch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.rezeptbuch.app.ui.cooking.CookingModeScreen
import de.rezeptbuch.app.ui.detail.RecipeDetailScreen
import de.rezeptbuch.app.ui.edit.RecipeEditScreen
import de.rezeptbuch.app.ui.list.RecipeListScreen
import de.rezeptbuch.app.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Route.RecipeList) {
        composable<Route.RecipeList> {
            RecipeListScreen(
                onOpenRecipe = { nav.navigate(Route.RecipeDetail(it)) },
                onAddRecipe = { nav.navigate(Route.RecipeEdit()) },
                onOpenSettings = { nav.navigate(Route.Settings) },
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
        composable<Route.Settings> {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
