package de.rezeptbuch.app.data.local

import de.rezeptbuch.app.data.local.entity.CategoryEntity
import de.rezeptbuch.app.data.local.entity.CategoryWithCountEntity
import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.RecipeWithDetails
import de.rezeptbuch.app.data.local.entity.StepEntity
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryColor
import de.rezeptbuch.app.domain.model.CategoryIcon
import de.rezeptbuch.app.domain.model.CategoryWithCount
import de.rezeptbuch.app.domain.model.CookingStep
import de.rezeptbuch.app.domain.model.Ingredient
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.model.RecipeNote
import de.rezeptbuch.app.domain.model.RecipeSummary
import java.time.Instant

fun RecipeEntity.toSummary() = RecipeSummary(
    id = id,
    title = title,
    photoFileName = photoFileName,
    baseServings = baseServings,
    cookTimeMinutes = cookTimeMinutes,
    createdAt = Instant.ofEpochMilli(createdAt),
    lastCookedAt = lastCookedAt?.let(Instant::ofEpochMilli),
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    uuid = uuid,
    name = name,
    icon = CategoryIcon.fromName(icon),
    color = CategoryColor.fromName(color),
    position = position,
)

fun Category.toEntity(createdAt: Long) = CategoryEntity(
    id = id,
    uuid = uuid,
    name = name.trim(),
    icon = icon.name,
    color = color.name,
    position = position,
    createdAt = createdAt,
)

fun CategoryWithCountEntity.toDomain() = CategoryWithCount(category.toDomain(), recipeCount)

fun RecipeWithDetails.toDomain() = Recipe(
    id = recipe.id,
    uuid = recipe.uuid,
    title = recipe.title,
    photoFileName = recipe.photoFileName,
    baseServings = recipe.baseServings,
    cookTimeMinutes = recipe.cookTimeMinutes,
    ingredients = ingredients.sortedBy { it.position }.map { Ingredient(it.amount, it.unit, it.name) },
    steps = steps.sortedBy { it.position }.map { CookingStep(it.text, it.timerSeconds) },
    notes = notes.sortedByDescending { it.createdAt }.map { it.toDomain() },
    createdAt = Instant.ofEpochMilli(recipe.createdAt),
    updatedAt = Instant.ofEpochMilli(recipe.updatedAt),
    categoryIds = categoryRefs.map { it.categoryId }.toSet(),
    lastCookedAt = recipe.lastCookedAt?.let(Instant::ofEpochMilli),
)

fun NoteEntity.toDomain() = RecipeNote(
    id = id,
    recipeId = recipeId,
    text = text,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Recipe.toEntity() = RecipeEntity(
    id = id,
    uuid = uuid,
    title = title.trim(),
    photoFileName = photoFileName,
    baseServings = baseServings,
    cookTimeMinutes = cookTimeMinutes,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    lastCookedAt = lastCookedAt?.toEpochMilli(),
)

fun List<Ingredient>.toEntities(recipeId: Long) = mapIndexed { index, it ->
    IngredientEntity(recipeId = recipeId, position = index, amount = it.amount, unit = it.unit, name = it.name.trim())
}

fun List<CookingStep>.toStepEntities(recipeId: Long) = mapIndexed { index, it ->
    StepEntity(recipeId = recipeId, position = index, text = it.text.trim(), timerSeconds = it.timerSeconds)
}
