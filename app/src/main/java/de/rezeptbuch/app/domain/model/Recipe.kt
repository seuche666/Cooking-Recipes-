package de.rezeptbuch.app.domain.model

import java.time.Instant

/** Vollständiges Rezept, wie es Detail-, Bearbeiten- und Kochmodus-Screen verwenden. */
data class Recipe(
    val id: Long = 0,
    /** Stabile ID über Geräte/Backups hinweg (wichtig für "Zusammenführen" beim Import). */
    val uuid: String,
    val title: String,
    /** Dateiname im internen Foto-Verzeichnis, `null` wenn kein Foto vorhanden. */
    val photoFileName: String?,
    val baseServings: Int,
    val cookTimeMinutes: Int?,
    val ingredients: List<Ingredient>,
    val steps: List<CookingStep>,
    val notes: List<RecipeNote>,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** IDs der zugeordneten Kategorien (beliebig viele). */
    val categoryIds: Set<Long> = emptySet(),
    /** Wann der Kochmodus zuletzt für dieses Rezept gestartet wurde. */
    val lastCookedAt: Instant? = null,
)

/** Schlanke Variante für Listen und Karten. */
data class RecipeSummary(
    val id: Long,
    val title: String,
    val photoFileName: String?,
    val baseServings: Int,
    val cookTimeMinutes: Int?,
    val createdAt: Instant = Instant.EPOCH,
    val lastCookedAt: Instant? = null,
    /** Zutaten, die zum Suchbegriff passen (für den Hinweis „enthält …“). */
    val matchedIngredients: List<String> = emptyList(),
)

data class Ingredient(
    /** `null` = ohne Mengenangabe, z. B. „Salz nach Geschmack“. */
    val amount: Double?,
    val unit: IngredientUnit,
    val name: String,
)

data class CookingStep(
    val text: String,
    /** Optionale Timer-Dauer in Sekunden. */
    val timerSeconds: Int?,
)

data class RecipeNote(
    val id: Long = 0,
    val recipeId: Long,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
