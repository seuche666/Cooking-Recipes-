package de.rezeptbuch.app.ui.edit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.IngredientUnit
import de.rezeptbuch.app.ui.components.RecipePhoto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditEvent.Saved -> onSaved(event.id, event.isNew)
            }
        }
    }

    val requestBack: () -> Unit = {
        if (state.isDirty) {
            confirmDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(onBack = requestBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.recipe_new else R.string.recipe_edit)) },
                navigationIcon = {
                    IconButton(onClick = requestBack) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.isSaving && !state.isLoading) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            EditForm(state, viewModel, Modifier.padding(padding).imePadding())
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

@Composable
private fun EditForm(state: RecipeEditUiState, vm: RecipeEditViewModel, modifier: Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PhotoEditor(state, vm) }
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text(stringResource(R.string.field_title)) },
                isError = state.showTitleError,
                supportingText = if (state.showTitleError) {
                    { Text(stringResource(R.string.error_title_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.baseServings,
                    onValueChange = vm::onServingsChange,
                    label = { Text(stringResource(R.string.field_servings)) },
                    isError = state.showServingsError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.cookTimeMinutes,
                    onValueChange = vm::onCookTimeChange,
                    label = { Text(stringResource(R.string.field_cook_time)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle(stringResource(R.string.section_ingredients)) }
        itemsIndexed(state.ingredients, key = { _, it -> "i${it.key}" }) { _, ingredient ->
            IngredientEditorRow(
                draft = ingredient,
                onChange = vm::updateIngredient,
                onRemove = { vm.removeIngredient(ingredient.key) },
            )
        }
        item {
            OutlinedButton(onClick = vm::addIngredient) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.ingredient_add), Modifier.padding(start = 8.dp))
            }
        }

        item { SectionTitle(stringResource(R.string.section_steps)) }
        itemsIndexed(state.steps, key = { _, it -> "s${it.key}" }) { index, step ->
            StepEditorCard(
                index = index,
                count = state.steps.size,
                draft = step,
                onChange = vm::updateStep,
                onMove = { vm.moveStep(step.key, it) },
                onRemove = { vm.removeStep(step.key) },
            )
        }
        item {
            OutlinedButton(onClick = vm::addStep) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.step_add), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PhotoEditor(state: RecipeEditUiState, vm: RecipeEditViewModel) {
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::onPhotoPicked)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) vm.onPhotoPicked(uri)
        pendingCameraUri = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)), Alignment.Center) {
            RecipePhoto(state.photoFileName, stringResource(R.string.photo), Modifier.fillMaxSize(), 56.dp)
            if (state.isImportingPhoto) CircularProgressIndicator()
        }
        if (state.photoError) {
            Text(stringResource(R.string.photo_error), color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Outlined.PhotoLibrary, null)
                Text(stringResource(R.string.photo_gallery), Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = {
                val uri = vm.createCameraUri()
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }) {
                Icon(Icons.Outlined.PhotoCamera, null)
                Text(stringResource(R.string.photo_camera), Modifier.padding(start = 6.dp))
            }
            if (state.photoFileName != null) {
                IconButton(onClick = vm::removePhoto) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.photo_remove))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientEditorRow(
    draft: IngredientDraft,
    onChange: (IngredientDraft) -> Unit,
    onRemove: () -> Unit,
) {
    var unitMenuOpen by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = draft.amount,
            onValueChange = { v -> onChange(draft.copy(amount = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8))) },
            label = { Text(stringResource(R.string.field_amount)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(84.dp),
        )
        ExposedDropdownMenuBox(
            expanded = unitMenuOpen,
            onExpandedChange = { unitMenuOpen = it },
            modifier = Modifier.width(100.dp),
        ) {
            OutlinedTextField(
                value = draft.unit.label.ifEmpty { "–" },
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.field_unit)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuOpen) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                IngredientUnit.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.label.ifEmpty { stringResource(R.string.unit_none) }) },
                        onClick = { onChange(draft.copy(unit = unit)); unitMenuOpen = false },
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text(stringResource(R.string.field_ingredient)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, stringResource(R.string.ingredient_remove)) }
    }
}

@Composable
private fun StepEditorCard(
    index: Int,
    count: Int,
    draft: StepDraft,
    onChange: (StepDraft) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.step_number, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onMove(-1) }, enabled = index > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.step_move_up))
                }
                IconButton(onClick = { onMove(1) }, enabled = index < count - 1) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.step_move_down))
                }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, stringResource(R.string.step_remove)) }
            }
            OutlinedTextField(
                value = draft.text,
                onValueChange = { onChange(draft.copy(text = it)) },
                label = { Text(stringResource(R.string.field_step_text)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.timer,
                onValueChange = { v -> onChange(draft.copy(timer = v.filter { it.isDigit() || it in ",.:" }.take(6))) },
                label = { Text(stringResource(R.string.field_step_timer)) },
                supportingText = { Text(stringResource(R.string.field_step_timer_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
