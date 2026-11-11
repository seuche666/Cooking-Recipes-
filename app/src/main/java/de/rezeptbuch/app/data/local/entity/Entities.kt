package de.rezeptbuch.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import de.rezeptbuch.app.domain.model.IngredientUnit

@Entity(
    tableName = "recipes",
    indices = [Index(value = ["uuid"], unique = true), Index("title")],
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val title: String,
    @ColumnInfo(name = "photo_file_name") val photoFileName: String?,
    @ColumnInfo(name = "base_servings") val baseServings: Int,
    @ColumnInfo(name = "cook_time_minutes") val cookTimeMinutes: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Seit DB-Version 2. */
    @ColumnInfo(name = "last_cooked_at") val lastCookedAt: Long? = null,
)

/** Seit DB-Version 2. [icon] und [color] speichern Enum-Namen. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["uuid"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val icon: String,
    val color: String,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Zuordnung Rezept ↔ Kategorie (n:m). Seit DB-Version 2. */
@Entity(
    tableName = "recipe_categories",
    primaryKeys = ["recipe_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id"), Index("category_id")],
)
data class RecipeCategoryCrossRef(
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
)

/** Kategorie samt Anzahl zugeordneter Rezepte. */
data class CategoryWithCountEntity(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "recipe_count") val recipeCount: Int,
)

/** Treffer der Zutatensuche: welches Rezept enthält welche passende Zutat. */
data class IngredientMatch(
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val name: String,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val position: Int,
    val amount: Double?,
    val unit: IngredientUnit,
    val name: String,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val position: Int,
    val text: String,
    @ColumnInfo(name = "timer_seconds") val timerSeconds: Int?,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Rezept inkl. aller Kind-Tabellen, von Room per @Relation geladen. */
data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    val steps: List<StepEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    val notes: List<NoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    val categoryRefs: List<RecipeCategoryCrossRef>,
)
