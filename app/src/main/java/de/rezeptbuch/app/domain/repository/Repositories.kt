package de.rezeptbuch.app.domain.repository

import android.net.Uri
import de.rezeptbuch.app.domain.model.ExportResult
import de.rezeptbuch.app.domain.model.ImportMode
import de.rezeptbuch.app.domain.model.ImportResult
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(query: String): Flow<List<RecipeSummary>>
    fun observeRecipe(id: Long): Flow<Recipe?>
    suspend fun getRecipe(id: Long): Recipe?

    /** Legt das Rezept an (id == 0) oder aktualisiert es. Notizen werden hier nicht verändert. */
    suspend fun saveRecipe(recipe: Recipe): Long
    suspend fun deleteRecipe(id: Long)

    suspend fun addNote(recipeId: Long, text: String)
    suspend fun updateNote(noteId: Long, text: String)
    suspend fun deleteNote(noteId: Long)
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

interface BackupRepository {
    /** Schreibt das ZIP-Backup nach [target]. [onProgress] erhält Werte von 0f bis 1f. */
    suspend fun export(target: Uri, onProgress: (Float) -> Unit): ExportResult

    suspend fun import(source: Uri, mode: ImportMode, onProgress: (Float) -> Unit): ImportResult
}
