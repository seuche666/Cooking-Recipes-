package de.rezeptbuch.app.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: RecipeRepository,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<RecipeListUiState> = query
        .debounce(150)
        .flatMapLatest { repository.observeRecipes(it) }
        .map { RecipeListUiState(recipes = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}
