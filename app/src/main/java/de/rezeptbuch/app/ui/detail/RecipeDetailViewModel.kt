package de.rezeptbuch.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.portion.PortionCalculator
import de.rezeptbuch.app.domain.portion.ScaledIngredient
import de.rezeptbuch.app.domain.repository.RecipeRepository
import de.rezeptbuch.app.ui.navigation.Route
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data object NotFound : RecipeDetailUiState
    data class Content(
        val recipe: Recipe,
        val servings: Int,
        val scaledIngredients: List<ScaledIngredient>,
    ) : RecipeDetailUiState
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<Route.RecipeDetail>().recipeId

    /** 0 = noch nicht gewählt → Grundportionen des Rezepts. Wird nie in der DB gespeichert. */
    private val selectedServings = savedStateHandle.getStateFlow(KEY_SERVINGS, 0)

    private val _events = Channel<Unit>(Channel.BUFFERED)
    /** Feuert, nachdem das Rezept gelöscht wurde. */
    val deleted = _events.receiveAsFlow()

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(repository.observeRecipe(recipeId), selectedServings) { recipe, selected ->
            if (recipe == null) {
                RecipeDetailUiState.NotFound
            } else {
                val servings = if (selected > 0) selected else recipe.baseServings
                RecipeDetailUiState.Content(
                    recipe = recipe,
                    servings = servings,
                    scaledIngredients = PortionCalculator.scale(recipe.ingredients, recipe.baseServings, servings),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState.Loading)

    fun setServings(value: Int) {
        savedStateHandle[KEY_SERVINGS] = value.coerceIn(PortionCalculator.MIN_SERVINGS, PortionCalculator.MAX_SERVINGS)
    }

    fun deleteRecipe() = viewModelScope.launch {
        repository.deleteRecipe(recipeId)
        _events.send(Unit)
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.addNote(recipeId, text) }
    }

    fun updateNote(noteId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.updateNote(noteId, text) }
    }

    fun deleteNote(noteId: Long) = viewModelScope.launch { repository.deleteNote(noteId) }

    private companion object {
        const val KEY_SERVINGS = "servings"
    }
}
