package de.rezeptbuch.app.ui.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.data.photo.PhotoStorage
import de.rezeptbuch.app.domain.model.CookingStep
import de.rezeptbuch.app.domain.model.Ingredient
import de.rezeptbuch.app.domain.model.IngredientUnit
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.portion.PortionCalculator
import de.rezeptbuch.app.domain.portion.TimerInput
import de.rezeptbuch.app.domain.repository.RecipeRepository
import de.rezeptbuch.app.ui.navigation.Route
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

data class IngredientDraft(
    val key: Long,
    val amount: String = "",
    val unit: IngredientUnit = IngredientUnit.GRAM,
    val name: String = "",
)

data class StepDraft(
    val key: Long,
    val text: String = "",
    /** Eingabe in Minuten, z. B. „5“, „1,5“ oder „1:30“. Leer = kein Timer. */
    val timer: String = "",
)

data class RecipeEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val photoFileName: String? = null,
    val isImportingPhoto: Boolean = false,
    val baseServings: String = "2",
    val cookTimeMinutes: String = "",
    val ingredients: List<IngredientDraft> = emptyList(),
    val steps: List<StepDraft> = emptyList(),
    val showTitleError: Boolean = false,
    val showServingsError: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val photoError: Boolean = false,
)

sealed interface EditEvent {
    data class Saved(val id: Long, val isNew: Boolean) : EditEvent
}

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<Route.RecipeEdit>().recipeId
    private val keys = AtomicLong(1)
    private var original: Recipe? = null
    private var saved = false

    private val _state = MutableStateFlow(RecipeEditUiState())
    val state: StateFlow<RecipeEditUiState> = _state.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val recipe = if (recipeId != 0L) repository.getRecipe(recipeId) else null
            original = recipe
            _state.value = if (recipe == null) {
                RecipeEditUiState(
                    isLoading = false,
                    ingredients = listOf(IngredientDraft(keys.getAndIncrement())),
                    steps = listOf(StepDraft(keys.getAndIncrement())),
                )
            } else {
                RecipeEditUiState(
                    isLoading = false,
                    isNew = false,
                    title = recipe.title,
                    photoFileName = recipe.photoFileName,
                    baseServings = recipe.baseServings.toString(),
                    cookTimeMinutes = recipe.cookTimeMinutes?.toString().orEmpty(),
                    ingredients = recipe.ingredients.map {
                        IngredientDraft(keys.getAndIncrement(), PortionCalculator.amountToInput(it.amount), it.unit, it.name)
                    },
                    steps = recipe.steps.map {
                        StepDraft(keys.getAndIncrement(), it.text, TimerInput.format(it.timerSeconds))
                    },
                )
            }
        }
    }

    private inline fun edit(crossinline block: (RecipeEditUiState) -> RecipeEditUiState) =
        _state.update { block(it).copy(isDirty = true) }

    fun onTitleChange(v: String) = edit { it.copy(title = v, showTitleError = false) }
    fun onServingsChange(v: String) = edit { it.copy(baseServings = v.filter(Char::isDigit).take(2), showServingsError = false) }
    fun onCookTimeChange(v: String) = edit { it.copy(cookTimeMinutes = v.filter(Char::isDigit).take(4)) }

    // --- Zutaten
    fun addIngredient() = edit { it.copy(ingredients = it.ingredients + IngredientDraft(keys.getAndIncrement())) }
    fun removeIngredient(key: Long) = edit { s -> s.copy(ingredients = s.ingredients.filterNot { it.key == key }) }
    fun updateIngredient(updated: IngredientDraft) =
        edit { s -> s.copy(ingredients = s.ingredients.map { if (it.key == updated.key) updated else it }) }

    // --- Schritte
    fun addStep() = edit { it.copy(steps = it.steps + StepDraft(keys.getAndIncrement())) }
    fun removeStep(key: Long) = edit { s -> s.copy(steps = s.steps.filterNot { it.key == key }) }
    fun updateStep(updated: StepDraft) = edit { s -> s.copy(steps = s.steps.map { if (it.key == updated.key) updated else it }) }
    fun moveStep(key: Long, delta: Int) = edit { s ->
        val index = s.steps.indexOfFirst { it.key == key }
        val target = index + delta
        if (index < 0 || target !in s.steps.indices) return@edit s
        s.copy(steps = s.steps.toMutableList().apply { add(target, removeAt(index)) })
    }

    // --- Foto
    fun onPhotoPicked(uri: Uri) {
        _state.update { it.copy(isImportingPhoto = true, photoError = false) }
        viewModelScope.launch {
            runCatching { photoStorage.importPhoto(uri) }
                .onSuccess { newName ->
                    discardUnsavedPhoto()
                    edit { it.copy(photoFileName = newName, isImportingPhoto = false) }
                }
                .onFailure { _state.update { it.copy(isImportingPhoto = false, photoError = true) } }
            photoStorage.clearCameraCache()
        }
    }

    fun createCameraUri(): Uri = photoStorage.createCameraUri()

    fun removePhoto() {
        discardUnsavedPhoto()
        edit { it.copy(photoFileName = null) }
    }

    /** Löscht ein in dieser Sitzung neu importiertes, noch nicht gespeichertes Foto. */
    private fun discardUnsavedPhoto() {
        val current = _state.value.photoFileName
        if (current != null && current != original?.photoFileName) photoStorage.delete(current)
    }

    fun save() {
        val s = _state.value
        val servings = s.baseServings.toIntOrNull()
        val titleError = s.title.isBlank()
        val servingsError = servings == null || servings !in 1..99
        if (titleError || servingsError) {
            _state.update { it.copy(showTitleError = titleError, showServingsError = servingsError) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            val recipe = Recipe(
                id = original?.id ?: 0,
                uuid = original?.uuid ?: UUID.randomUUID().toString(),
                title = s.title.trim(),
                photoFileName = s.photoFileName,
                baseServings = servings!!,
                cookTimeMinutes = s.cookTimeMinutes.toIntOrNull()?.takeIf { it > 0 },
                ingredients = s.ingredients
                    .filter { it.name.isNotBlank() }
                    .map { Ingredient(PortionCalculator.parseAmount(it.amount), it.unit, it.name.trim()) },
                steps = s.steps
                    .filter { it.text.isNotBlank() }
                    .map { CookingStep(it.text.trim(), TimerInput.parse(it.timer)) },
                notes = emptyList(),
                createdAt = original?.createdAt ?: now,
                updatedAt = now,
            )
            val id = repository.saveRecipe(recipe)
            saved = true
            _events.send(EditEvent.Saved(id, isNew = original == null))
        }
    }

    override fun onCleared() {
        if (!saved) discardUnsavedPhoto()
    }
}
