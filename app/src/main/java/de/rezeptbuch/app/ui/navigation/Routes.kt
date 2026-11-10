package de.rezeptbuch.app.ui.navigation

import kotlinx.serialization.Serializable

/** Typsichere Navigationsziele (Navigation Compose 2.8+). */
sealed interface Route {
    @Serializable data object RecipeList : Route
    @Serializable data class RecipeDetail(val recipeId: Long) : Route

    /** recipeId == 0 → neues Rezept. */
    @Serializable data class RecipeEdit(val recipeId: Long = 0) : Route
    @Serializable data class CookingMode(val recipeId: Long, val servings: Int) : Route
    @Serializable data object Settings : Route
}
