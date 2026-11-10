package de.rezeptbuch.app.ui.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.domain.model.Recipe
import de.rezeptbuch.app.domain.portion.PortionCalculator
import de.rezeptbuch.app.domain.portion.ScaledIngredient
import de.rezeptbuch.app.domain.repository.RecipeRepository
import de.rezeptbuch.app.service.timer.CookingTimer
import de.rezeptbuch.app.service.timer.CookingTimerManager
import de.rezeptbuch.app.ui.navigation.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CookingUiState(
    val recipe: Recipe? = null,
    val servings: Int = 1,
    val ingredients: List<ScaledIngredient> = emptyList(),
    /** Timer dieses Rezepts, Schlüssel = Schrittindex. */
    val timers: Map<Int, CookingTimer> = emptyMap(),
)

@HiltViewModel
class CookingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: RecipeRepository,
    private val timerManager: CookingTimerManager,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.CookingMode>()

    val state: StateFlow<CookingUiState> =
        combine(repository.observeRecipe(route.recipeId), timerManager.timers) { recipe, timers ->
            CookingUiState(
                recipe = recipe,
                servings = route.servings,
                ingredients = recipe?.let { PortionCalculator.scale(it.ingredients, it.baseServings, route.servings) }
                    .orEmpty(),
                timers = timers.values.filter { it.recipeId == route.recipeId }.associateBy { it.stepIndex },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookingUiState(servings = route.servings))

    fun startTimer(stepIndex: Int) {
        val recipe = state.value.recipe ?: return
        val seconds = recipe.steps.getOrNull(stepIndex)?.timerSeconds ?: return
        timerManager.start(recipe.id, recipe.title, stepIndex, seconds)
    }

    fun cancelTimer(stepIndex: Int) {
        timerManager.cancel(CookingTimer.idFor(route.recipeId, stepIndex))
    }
}
