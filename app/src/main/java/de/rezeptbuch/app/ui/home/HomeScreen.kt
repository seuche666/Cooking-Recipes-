package de.rezeptbuch.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.ui.components.CategoryBubble
import de.rezeptbuch.app.ui.components.CircleIconButton
import de.rezeptbuch.app.ui.components.RecipeCardCompact
import de.rezeptbuch.app.ui.components.SearchButton
import de.rezeptbuch.app.ui.components.SectionHeader

/**
 * Startseite = Überblick und Schnellzugriffe: Suche, Kategorien, zuletzt gekochte und
 * neu hinzugefügte Rezepte. Die vollständige Liste liegt unter „Rezepte“.
 */
@Composable
fun HomeScreen(
    onOpenRecipe: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onAddRecipe: () -> Unit,
    onSearch: () -> Unit,
    onShowAllRecipes: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    CircleIconButton(Icons.Filled.Add, stringResource(R.string.recipe_add), onAddRecipe)
                }
                Text(
                    stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                SearchButton(stringResource(R.string.search_hint), onClick = onSearch)
            }
        }

        if (state.isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
            return@LazyColumn
        }

        if (state.categories.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.section_categories), Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.categories, key = { it.category.id }) {
                        CategoryBubble(it.category, onClick = { onOpenCategory(it.category.id) })
                    }
                }
            }
        }

        if (state.recentlyCooked.isNotEmpty()) {
            item {
                RecipeRow(
                    title = stringResource(R.string.section_recently_cooked),
                    recipes = state.recentlyCooked,
                    onOpenRecipe = onOpenRecipe,
                )
            }
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item {
                RecipeRow(
                    title = stringResource(R.string.section_recently_added),
                    recipes = state.recentlyAdded,
                    onOpenRecipe = onOpenRecipe,
                    actionLabel = stringResource(R.string.show_all),
                    onAction = onShowAllRecipes,
                )
            }
        }

        if (state.isEmpty) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onAddRecipe) { Text(stringResource(R.string.recipe_add)) }
                }
            }
        } else if (state.recentlyCooked.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_cook_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun RecipeRow(
    title: String,
    recipes: List<RecipeSummary>,
    onOpenRecipe: (Long) -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column {
        SectionHeader(title, Modifier.padding(horizontal = 20.dp), actionLabel, onAction)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(recipes, key = { it.id }) { RecipeCardCompact(it, onClick = { onOpenRecipe(it.id) }) }
        }
    }
}
