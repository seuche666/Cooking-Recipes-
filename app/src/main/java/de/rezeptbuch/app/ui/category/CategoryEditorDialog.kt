package de.rezeptbuch.app.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryColor
import de.rezeptbuch.app.domain.model.CategoryIcon
import de.rezeptbuch.app.ui.components.CategoryCircle
import de.rezeptbuch.app.ui.components.vector
import de.rezeptbuch.app.ui.theme.tint
import java.util.UUID

/** Dialog zum Anlegen oder Bearbeiten einer Kategorie mit Name, Symbol und Farbe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: CategoryIcon.MAIN) }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: CategoryColor.BLUE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { CategoryCircle(icon, color, size = 56.dp) },
        title = { Text(stringResource(if (initial == null) R.string.category_new else R.string.category_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryIcon.entries.forEach { option ->
                        val selected = option == icon
                        val tint = color.tint()
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selected) tint.container else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    BorderStroke(2.dp, if (selected) tint.content else MaterialTheme.colorScheme.surfaceVariant),
                                    CircleShape,
                                )
                                .clickable { icon = option }
                                .semantics { this.selected = selected },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                option.vector,
                                null,
                                tint = if (selected) tint.content else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Text(stringResource(R.string.category_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryColor.entries.forEach { option ->
                        val tint = option.tint()
                        val selected = option == color
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tint.container)
                                .border(BorderStroke(if (selected) 3.dp else 1.dp, tint.content), CircleShape)
                                .clickable { color = option }
                                .semantics {
                                    this.selected = selected
                                    contentDescription = option.name
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Filled.Check, null, tint = tint.content, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (initial ?: Category(uuid = UUID.randomUUID().toString(), name = "", icon = icon, color = color))
                            .copy(name = name.trim(), icon = icon, color = color),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

/** Sicherheitsabfrage vor dem Löschen einer Kategorie. */
@Composable
fun DeleteCategoryDialog(category: Category, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_title)) },
        text = { Text(stringResource(R.string.category_delete_message, category.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
