package de.rezeptbuch.app.data.seed

import androidx.room.withTransaction
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.entity.NoteEntity
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
        db.withTransaction {
            if (dao.count() == 0) {
                SeedData.recipes().forEach { seed ->
                    val id = dao.insertRecipe(seed.recipe.toEntity())
                    dao.insertIngredients(seed.recipe.ingredients.toEntities(id))
                    dao.insertSteps(seed.recipe.steps.toStepEntities(id))
                    val now = System.currentTimeMillis()
                    dao.insertNotes(seed.notes.map { NoteEntity(recipeId = id, text = it, createdAt = now, updatedAt = now) })
                }
            }
        }
        settings.markSeeded()
    }
}
