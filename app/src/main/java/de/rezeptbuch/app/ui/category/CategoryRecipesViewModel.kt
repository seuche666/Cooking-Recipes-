package de.rezeptbuch.app.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.repository.CategoryRepository
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

data class CategoryRecipesUiState(
    val isLoading: Boolean = true,
    val category: Category? = null,
    val recipes: List<RecipeSummary> = emptyList(),
)

@HiltViewModel
class CategoryRecipesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categoryId = savedStateHandle.toRoute<Route.CategoryRecipes>().categoryId

    val state: StateFlow<CategoryRecipesUiState> = combine(
        categoryRepository.observeCategory(categoryId),
        recipeRepository.observeRecipesInCategory(categoryId),
    ) { category, recipes ->
        CategoryRecipesUiState(isLoading = false, category = category, recipes = recipes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryRecipesUiState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    fun save(category: Category) {
        viewModelScope.launch { categoryRepository.saveCategory(category) }
    }

    fun delete() {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            _deleted.send(Unit)
        }
    }
}
