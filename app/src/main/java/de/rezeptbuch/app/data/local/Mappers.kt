package de.rezeptbuch.app.data.local

import de.rezeptbuch.app.data.local.entity.IngredientEntity
import de.rezeptbuch.app.data.local.entity.NoteEntity
import de.rezeptbuch.app.data.local.entity.RecipeEntity
import de.rezeptbuch.app.data.local.entity.RecipeWithDetails
import de.rezeptbuch.app.data.local.entity.StepEntity
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
)

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
)

fun List<Ingredient>.toEntities(recipeId: Long) = mapIndexed { index, it ->
    IngredientEntity(recipeId = recipeId, position = index, amount = it.amount, unit = it.unit, name = it.name.trim())
}

fun List<CookingStep>.toStepEntities(recipeId: Long) = mapIndexed { index, it ->
    StepEntity(recipeId = recipeId, position = index, text = it.text.trim(), timerSeconds = it.timerSeconds)
}
