package de.rezeptbuch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.RecipeWithDetails
import de.rezeptbuch.app.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query(
        """
        SELECT * FROM recipes
        WHERE :query = '' OR title LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE
        """,
    )
    fun observeRecipes(query: String): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipeWithDetails(id: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeWithDetails(id: Long): RecipeWithDetails?

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY id")
    suspend fun getAllRecipesWithDetails(): List<RecipeWithDetails>

    @Query("SELECT * FROM recipes WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): RecipeEntity?

    @Query("SELECT photo_file_name FROM recipes WHERE photo_file_name IS NOT NULL")
    suspend fun getAllPhotoFileNames(): List<String>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Insert
    suspend fun insertIngredients(items: List<IngredientEntity>)

    @Insert
    suspend fun insertSteps(items: List<StepEntity>)

    @Query("DELETE FROM ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipe_id = :recipeId")
    suspend fun deleteSteps(recipeId: Long)

    // --- Notizen ---

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Insert
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("UPDATE notes SET text = :text, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateNote(id: Long, text: String, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM notes WHERE recipe_id = :recipeId")
    suspend fun deleteNotes(recipeId: Long)
}
