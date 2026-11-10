package de.rezeptbuch.app.data.repository

import androidx.room.withTransaction
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.toDomain
import de.rezeptbuch.app.data.local.toEntities
import de.rezeptbuch.app.data.local.toEntity
import de.rezeptbuch.app.data.local.toStepEntities
import de.rezeptbuch.app.data.local.toSummary
import de.rezeptbuch.app.data.photo.PhotoStorage
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val photoStorage: PhotoStorage,
) : RecipeRepository {

    private val dao = db.recipeDao()

    override fun observeRecipes(query: String): Flow<List<RecipeSummary>> =
        dao.observeRecipes(query.trim()).map { list -> list.map { it.toSummary() } }

    override fun observeRecipe(id: Long): Flow<Recipe?> =
        dao.observeRecipeWithDetails(id).map { it?.toDomain() }

    override suspend fun getRecipe(id: Long): Recipe? = dao.getRecipeWithDetails(id)?.toDomain()

    override suspend fun saveRecipe(recipe: Recipe): Long = db.withTransaction {
        val now = Instant.now()
        if (recipe.id == 0L) {
            val id = dao.insertRecipe(recipe.copy(createdAt = now, updatedAt = now).toEntity())
            dao.insertIngredients(recipe.ingredients.toEntities(id))
            dao.insertSteps(recipe.steps.toStepEntities(id))
            id
        } else {
            val old = dao.getRecipe(recipe.id)
            dao.updateRecipe(recipe.copy(updatedAt = now).toEntity())
            dao.deleteIngredients(recipe.id)
            dao.deleteSteps(recipe.id)
            dao.insertIngredients(recipe.ingredients.toEntities(recipe.id))
            dao.insertSteps(recipe.steps.toStepEntities(recipe.id))
            // Ersetztes Foto aufräumen
            old?.photoFileName?.takeIf { it != recipe.photoFileName }?.let(photoStorage::delete)
            recipe.id
        }
    }

    override suspend fun deleteRecipe(id: Long) {
        val photo = dao.getRecipe(id)?.photoFileName
        dao.deleteRecipe(id) // Zutaten, Schritte und Notizen per CASCADE
        photo?.let(photoStorage::delete)
    }

    override suspend fun addNote(recipeId: Long, text: String) {
        val now = System.currentTimeMillis()
        dao.insertNote(NoteEntity(recipeId = recipeId, text = text.trim(), createdAt = now, updatedAt = now))
    }

    override suspend fun updateNote(noteId: Long, text: String) {
        dao.updateNote(noteId, text.trim(), System.currentTimeMillis())
    }

    override suspend fun deleteNote(noteId: Long) = dao.deleteNote(noteId)
}
