package de.rezeptbuch.app.ui.navigation

import kotlinx.serialization.Serializable

/** Typsichere Navigationsziele (Navigation Compose 2.8+). */
sealed interface Route {
    // --- Hauptbereiche mit unterer Navigationsleiste
    @Serializable data object Home : Route

    /** [focusSearch]: Suchfeld direkt aktivieren (Sprung von der Startseite). */
    @Serializable data class Recipes(val focusSearch: Boolean = false) : Route
    @Serializable data object Settings : Route

    // --- Unterseiten (ohne Navigationsleiste)
    @Serializable data class CategoryRecipes(val categoryId: Long) : Route
    @Serializable data class RecipeDetail(val recipeId: Long) : Route

    /** recipeId == 0 → neues Rezept; [categoryId] wird bei neuen Rezepten vorausgewählt. */
    @Serializable data class RecipeEdit(val recipeId: Long = 0, val categoryId: Long = 0) : Route
    @Serializable data class CookingMode(val recipeId: Long, val servings: Int) : Route
}
