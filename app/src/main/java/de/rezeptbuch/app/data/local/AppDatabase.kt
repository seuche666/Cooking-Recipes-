package de.rezeptbuch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.rezeptbuch.app.data.local.dao.RecipeDao
import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.StepEntity

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val NAME = "rezeptbuch.db"
    }
}
