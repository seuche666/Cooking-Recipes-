package de.rezeptbuch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.rezeptbuch.app.data.local.dao.CategoryDao
import de.rezeptbuch.app.data.local.dao.RecipeDao
import de.rezeptbuch.app.data.local.entity.CategoryEntity
import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeCategoryCrossRef
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.StepEntity

/**
 * Versionen:
 * 1 – Rezepte, Zutaten, Schritte, Notizen
 * 2 – Kategorien (n:m-Zuordnung) und „zuletzt gekocht“, siehe [Migrations.MIGRATION_1_2]
 */
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        NoteEntity::class,
        CategoryEntity::class,
        RecipeCategoryCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "rezeptbuch.db"
    }
}
