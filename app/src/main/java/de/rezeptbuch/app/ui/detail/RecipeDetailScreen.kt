package de.rezeptbuch.app.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.portion.formatMinutes
import de.rezeptbuch.app.domain.portion.formatTimer
import de.rezeptbuch.app.ui.components.IngredientRows
import de.rezeptbuch.app.ui.components.RecipePhoto
import de.rezeptbuch.app.ui.components.ServingsStepper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStartCooking: (recipeId: Long, servings: Int) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    val content = state as? RecipeDetailUiState.Content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content?.recipe?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = { onEdit(content.recipe.id) }) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.recipe_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.recipe_delete))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (content != null && content.recipe.steps.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onStartCooking(content.recipe.id, content.servings) },
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    text = { Text(stringResource(R.string.cooking_mode_start)) },
                )
            }
        },
    ) { padding ->
        when (val s = state) {
            RecipeDetailUiState.Loading ->
                Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            RecipeDetailUiState.NotFound ->
                Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.recipe_not_found)) }
            is RecipeDetailUiState.Content -> DetailContent(s, viewModel, PaddingValues(bottom = 96.dp), Modifier.padding(padding))
        }
    }

    if (confirmDelete && content != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.recipe_delete_title)) },
            text = { Text(stringResource(R.string.recipe_delete_message, content.recipe.title)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteRecipe() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DetailContent(
    state: RecipeDetailUiState.Content,
    viewModel: RecipeDetailViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val recipe = state.recipe
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            RecipePhoto(
                photoFileName = recipe.photoFileName,
                contentDescription = recipe.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                placeholderIconSize = 64.dp,
            )
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.headlineSmall)
                recipe.cookTimeMinutes?.let {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.cook_time_value, formatMinutes(it)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }

        // --- Zutaten + Portionsrechner
        item { SectionHeader(stringResource(R.string.section_ingredients)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ServingsStepper(servings = state.servings, onChange = viewModel::setServings)
                if (state.servings != recipe.baseServings) {
                    Text(
                        pluralStringResource(R.plurals.servings_original, recipe.baseServings, recipe.baseServings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.scaledIngredients.isEmpty()) {
                    Text(stringResource(R.string.ingredients_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    IngredientRows(state.scaledIngredients)
                }
            }
        }

        // --- Zubereitung
        item { SectionHeader(stringResource(R.string.section_steps)) }
        itemsIndexed(recipe.steps) { index, step ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(step.text, style = MaterialTheme.typography.bodyLarge)
                    step.timerSeconds?.let {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.step_timer_label, formatTimer(it.toLong())),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // --- Notizen
        item { SectionHeader(stringResource(R.string.section_notes)) }
        item {
            NotesSection(
                notes = recipe.notes,
                onAdd = viewModel::addNote,
                onUpdate = viewModel::updateNote,
                onDelete = { viewModel.deleteNote(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
        HorizontalDivider(Modifier.padding(bottom = 16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
