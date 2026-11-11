package de.rezeptbuch.app.ui.recipes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryWithCount
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.ui.category.CategoryEditorDialog
import de.rezeptbuch.app.ui.category.DeleteCategoryDialog
import de.rezeptbuch.app.ui.components.CardShape
import de.rezeptbuch.app.ui.components.CategoryTile
import de.rezeptbuch.app.ui.components.CircleIconButton
import de.rezeptbuch.app.ui.components.PillTabs
import de.rezeptbuch.app.ui.components.RecipeCard
import de.rezeptbuch.app.ui.components.SearchField
import kotlinx.coroutines.launch

private const val PAGE_ALL = 0
private const val PAGE_CATEGORIES = 1

/** Alle Rezepte: Suche (Name + Zutaten), Umschalter „Alle Rezepte“ / „Kategorien“ mit Wischen. */
@Composable
fun RecipesScreen(
    snackbarHostState: SnackbarHostState,
    onOpenRecipe: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onAddRecipe: () -> Unit,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val deletedMessage = stringResource(R.string.recipe_deleted)
    val undoLabel = stringResource(R.string.undo)

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var creatingCategory by rememberSaveable { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(Unit) {
        if (viewModel.focusSearchOnStart) focusRequester.requestFocus()
    }
    // Beim Tippen in der Suche automatisch zu „Alle Rezepte“ wechseln
    LaunchedEffect(query) {
        if (query.isNotEmpty() && pagerState.currentPage != PAGE_ALL) pagerState.animateScrollToPage(PAGE_ALL)
    }

    val onSwipeDelete: (RecipeSummary) -> Unit = { recipe ->
        viewModel.hideRecipe(recipe.id)
        scope.launch {
            var undone = false
            try {
                undone = snackbarHostState.showSnackbar(
                    message = String.format(deletedMessage, recipe.title),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                ) == SnackbarResult.ActionPerformed
            } finally {
                if (undone) viewModel.undoDelete(recipe.id) else viewModel.confirmDelete(recipe.id)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_recipes), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Filled.Add, stringResource(R.string.recipe_add), onAddRecipe)
            }
            Spacer(Modifier.height(16.dp))
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_hint),
                focusRequester = focusRequester,
            )
            Spacer(Modifier.height(16.dp))
            PillTabs(
                titles = listOf(stringResource(R.string.tab_all_recipes), stringResource(R.string.tab_categories)),
                pagerState = pagerState,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                when (page) {
                    PAGE_ALL -> AllRecipesPage(state.recipes, query, onOpenRecipe, onSwipeDelete)
                    else -> CategoriesPage(
                        categories = state.categories,
                        onOpenCategory = onOpenCategory,
                        onCreate = { creatingCategory = true },
                        onEdit = { editingCategory = it },
                        onDelete = { deletingCategory = it },
                    )
                }
            }
        }
    }

    if (creatingCategory || editingCategory != null) {
        CategoryEditorDialog(
            initial = editingCategory,
            onDismiss = { creatingCategory = false; editingCategory = null },
            onSave = {
                viewModel.saveCategory(it)
                creatingCategory = false
                editingCategory = null
            },
        )
    }
    deletingCategory?.let { category ->
        DeleteCategoryDialog(
            category = category,
            onDismiss = { deletingCategory = null },
            onConfirm = { viewModel.deleteCategory(category.id); deletingCategory = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllRecipesPage(
    recipes: List<RecipeSummary>,
    query: String,
    onOpenRecipe: (Long) -> Unit,
    onSwipeDelete: (RecipeSummary) -> Unit,
) {
    if (recipes.isEmpty()) {
        EmptyHint(stringResource(if (query.isNotBlank()) R.string.list_empty_search else R.string.list_empty))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (query.isBlank()) {
            item {
                Text(
                    stringResource(R.string.swipe_to_delete_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(recipes, key = { it.id }) { recipe ->
            SwipeToDeleteContainer(onDelete = { onSwipeDelete(recipe) }, modifier = Modifier.animateItem()) {
                RecipeCard(recipe, onClick = { onOpenRecipe(recipe.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val active = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val iconPadding by animateDpAsState(if (active) 32.dp else 24.dp, label = "deleteIcon")
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, CardShape)
                    .padding(end = iconPadding),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.recipe_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

@Composable
private fun CategoriesPage(
    categories: List<CategoryWithCount>,
    onOpenCategory: (Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.categories_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(categories, key = { it.category.id }) { item ->
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                CategoryTile(
                    category = item.category,
                    recipeCount = item.recipeCount,
                    onClick = { onOpenCategory(item.category.id) },
                    onLongClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menuOpen = false; onEdit(item.category) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menuOpen = false; onDelete(item.category) },
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.category_new), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
