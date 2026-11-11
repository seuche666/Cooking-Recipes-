package de.rezeptbuch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.rezeptbuch.app.data.local.entity.CategoryEntity
import de.rezeptbuch.app.data.local.entity.CategoryWithCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT categories.*, COUNT(recipe_categories.recipe_id) AS recipe_count
        FROM categories
        LEFT JOIN recipe_categories ON recipe_categories.category_id = categories.id
        GROUP BY categories.id
        ORDER BY categories.position, categories.name COLLATE NOCASE
        """,
    )
    fun observeWithCounts(): Flow<List<CategoryWithCountEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observe(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY position")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM categories")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
