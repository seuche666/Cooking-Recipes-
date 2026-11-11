package de.rezeptbuch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.CategoryWithCount
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.repository.CategoryRepository
import de.rezeptbuch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentlyCooked: List<RecipeSummary> = emptyList(),
    val recentlyAdded: List<RecipeSummary> = emptyList(),
    val categories: List<CategoryWithCount> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && recentlyAdded.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        recipeRepository.observeRecentlyCooked(LIMIT),
        recipeRepository.observeRecentlyAdded(LIMIT),
        categoryRepository.observeCategories(),
    ) { cooked, added, categories ->
        HomeUiState(isLoading = false, recentlyCooked = cooked, recentlyAdded = added, categories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private companion object {
        const val LIMIT = 10
    }
}
