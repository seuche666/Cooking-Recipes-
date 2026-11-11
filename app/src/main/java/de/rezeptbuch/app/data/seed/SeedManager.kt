package de.rezeptbuch.app.data.seed

import androidx.room.withTransaction
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeCategoryCrossRef
import de.rezeptbuch.app.data.local.toEntities
import de.rezeptbuch.app.data.local.toEntity
import de.rezeptbuch.app.data.local.toStepEntities
import de.rezeptbuch.app.data.settings.SettingsRepositoryImpl
import javax.inject.Inject
import javax.inject.Singleton

/** Legt beim ersten Start die Beispielrezepte an – genau einmal pro Installation. */
@Singleton
class SeedManager @Inject constructor(
    private val db: AppDatabase,
    private val settings: SettingsRepositoryImpl,
) {
    suspend fun seedIfNeeded() {
        if (settings.isSeeded()) return
        val dao = db.recipeDao()
        val categoryDao = db.categoryDao()
        db.withTransaction {
            // Standard-Kategorien (bei Updates legt sie bereits die Migration 1 → 2 an)
            val now = System.currentTimeMillis()
            val categoryIds = DefaultCategories.all.associate { c ->
                c.uuid to (categoryDao.findByUuid(c.uuid)?.id ?: categoryDao.insert(c.toEntity(now)))
            }
            if (dao.count() == 0) {
                SeedData.recipes().forEach { seed ->
                    val id = dao.insertRecipe(seed.recipe.toEntity())
                    dao.insertIngredients(seed.recipe.ingredients.toEntities(id))
                    dao.insertSteps(seed.recipe.steps.toStepEntities(id))
                    dao.insertCategoryRefs(seed.categoryUuids.mapNotNull { categoryIds[it] }.map { RecipeCategoryCrossRef(id, it) })
                    dao.insertNotes(seed.notes.map { NoteEntity(recipeId = id, text = it, createdAt = now, updatedAt = now) })
                }
            }
        }
        settings.markSeeded()
    }
}
