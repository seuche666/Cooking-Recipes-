package de.rezeptbuch.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.RecipeNote
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val noteDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMANY).withZone(ZoneId.systemDefault())

/** Notizen-Bereich der Detailseite: neue Notiz anlegen, vorhandene inline bearbeiten oder löschen. */
@Composable
fun NotesSection(
    notes: List<RecipeNote>,
    onAdd: (String) -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newNote by rememberSaveable { mutableStateOf("") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newNote,
            onValueChange = { newNote = it },
            placeholder = { Text(stringResource(R.string.note_hint)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = { onAdd(newNote); newNote = "" },
                    enabled = newNote.isNotBlank(),
                ) { Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.note_add)) }
            },
        )
        if (notes.isEmpty()) {
            Text(
                stringResource(R.string.notes_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notes.forEach { note ->
            NoteCard(note, onUpdate = { onUpdate(note.id, it) }, onDelete = { onDelete(note.id) })
        }
    }
}

@Composable
private fun NoteCard(note: RecipeNote, onUpdate: (String) -> Unit, onDelete: () -> Unit) {
    var editing by rememberSaveable(note.id) { mutableStateOf(false) }
    var draft by rememberSaveable(note.id) { mutableStateOf(note.text) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val edited = note.updatedAt.toEpochMilli() - note.createdAt.toEpochMilli() > 1000
                Text(
                    text = noteDateFormat.format(note.createdAt) +
                        if (edited) " · " + stringResource(R.string.note_edited) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                if (!editing) {
                    IconButton(onClick = { draft = note.text; editing = true }) {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.note_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.note_delete))
                    }
                }
            }
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                )
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        onClick = { onUpdate(draft); editing = false },
                        enabled = draft.isNotBlank(),
                    ) { Text(stringResource(R.string.save)) }
                }
            } else {
                Text(
                    note.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(end = 12.dp, bottom = 4.dp),
                )
            }
        }
    }
}
