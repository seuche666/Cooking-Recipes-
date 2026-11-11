package de.rezeptbuch.app.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryWithCount
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.repository.CategoryRepository
import de.rezeptbuch.app.domain.repository.RecipeRepository
import de.rezeptbuch.app.ui.navigation.Route
import de.rezeptbuch.app.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val isLoading: Boolean = true,
    val recipes: List<RecipeSummary> = emptyList(),
    val categories: List<CategoryWithCount> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    val focusSearchOnStart: Boolean = savedStateHandle.toRoute<Route.Recipes>().focusSearch

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /** Per Wischen gelöschte Rezepte, die bis zum Ablauf von „Rückgängig“ nur ausgeblendet sind. */
    private val pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<RecipesUiState> = combine(
        query.debounce(150).flatMapLatest { recipeRepository.observeRecipes(it) },
        categoryRepository.observeCategories(),
        pendingDeletes,
    ) { recipes, categories, pending ->
        RecipesUiState(isLoading = false, recipes = recipes.filterNot { it.id in pending }, categories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun hideRecipe(id: Long) = pendingDeletes.update { it + id }

    fun undoDelete(id: Long) = pendingDeletes.update { it - id }

    /** Endgültig löschen – im viewModelScope, damit es auch beim Verlassen des Bildschirms passiert. */
    fun confirmDelete(id: Long) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(id)
            pendingDeletes.update { it - id }
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch { categoryRepository.saveCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { categoryRepository.deleteCategory(id) }
    }

    override fun onCleared() {
        // Noch offene „Rückgängig“-Fenster: Löschen trotzdem ausführen
        val pending = pendingDeletes.value
        if (pending.isNotEmpty()) {
            appScope.launch { pending.forEach { recipeRepository.deleteRecipe(it) } }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}
