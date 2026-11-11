package de.rezeptbuch.app.data.repository

import androidx.room.withTransaction
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeCategoryCrossRef
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
import kotlinx.coroutines.flow.combine
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

    override fun observeRecipes(query: String): Flow<List<RecipeSummary>> {
        val q = query.trim()
        return combine(dao.observeRecipes(q), dao.observeIngredientMatches(q)) { recipes, matches ->
            val byRecipe = matches.groupBy({ it.recipeId }, { it.name })
            recipes.map { it.toSummary().copy(matchedIngredients = byRecipe[it.id].orEmpty().distinct()) }
        }
    }

    override fun observeRecipesInCategory(categoryId: Long): Flow<List<RecipeSummary>> =
        dao.observeRecipesInCategory(categoryId).map { list -> list.map { it.toSummary() } }

    override fun observeRecentlyCooked(limit: Int): Flow<List<RecipeSummary>> =
        dao.observeRecentlyCooked(limit).map { list -> list.map { it.toSummary() } }

    override fun observeRecentlyAdded(limit: Int): Flow<List<RecipeSummary>> =
        dao.observeRecentlyAdded(limit).map { list -> list.map { it.toSummary() } }

    override suspend fun markCooked(id: Long) = dao.setLastCooked(id, System.currentTimeMillis())

    override fun observeRecipe(id: Long): Flow<Recipe?> =
        dao.observeRecipeWithDetails(id).map { it?.toDomain() }

    override suspend fun getRecipe(id: Long): Recipe? = dao.getRecipeWithDetails(id)?.toDomain()

    override suspend fun saveRecipe(recipe: Recipe): Long = db.withTransaction {
        val now = Instant.now()
        if (recipe.id == 0L) {
            val id = dao.insertRecipe(recipe.copy(createdAt = now, updatedAt = now).toEntity())
            dao.insertIngredients(recipe.ingredients.toEntities(id))
            dao.insertSteps(recipe.steps.toStepEntities(id))
            dao.insertCategoryRefs(recipe.categoryIds.map { RecipeCategoryCrossRef(id, it) })
            id
        } else {
            val old = dao.getRecipe(recipe.id)
            // lastCookedAt aus der DB übernehmen, damit Bearbeiten den Verlauf nicht löscht
            dao.updateRecipe(recipe.copy(updatedAt = now).toEntity().copy(lastCookedAt = old?.lastCookedAt))
            dao.deleteIngredients(recipe.id)
            dao.deleteSteps(recipe.id)
            dao.insertIngredients(recipe.ingredients.toEntities(recipe.id))
            dao.insertSteps(recipe.steps.toStepEntities(recipe.id))
            dao.deleteCategoryRefs(recipe.id)
            dao.insertCategoryRefs(recipe.categoryIds.map { RecipeCategoryCrossRef(recipe.id, it) })
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
