package de.rezeptbuch.app.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.ui.components.CategoryCircle
import de.rezeptbuch.app.ui.components.RecipeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRecipesScreen(
    onBack: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onAddRecipe: (categoryId: Long) -> Unit,
    viewModel: CategoryRecipesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (state.category != null) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, stringResource(R.string.category_edit)) }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.category_delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddRecipe(viewModel.categoryId) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.recipe_add)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        val category = state.category
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            category == null -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.category_not_found))
            }
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryCircle(category.icon, category.color, size = 64.dp)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(category.name, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                pluralStringResource(R.plurals.recipe_count, state.recipes.size, state.recipes.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.recipes.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.category_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        )
                    }
                }
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe, onClick = { onOpenRecipe(recipe.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    val category = state.category
    if (editing && category != null) {
        CategoryEditorDialog(
            initial = category,
            onDismiss = { editing = false },
            onSave = { viewModel.save(it); editing = false },
        )
    }
    if (confirmDelete && category != null) {
        DeleteCategoryDialog(
            category = category,
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; viewModel.delete() },
        )
    }
}
