package de.rezeptbuch.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import de.rezeptbuch.app.ui.components.CardShape
import de.rezeptbuch.app.ui.components.softShadow
import de.rezeptbuch.app.BuildConfig
import de.rezeptbuch.app.R
import de.rezeptbuch.app.data.backup.BackupFormat
import de.rezeptbuch.app.domain.model.ImportMode
import de.rezeptbuch.app.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Storage Access Framework: Nutzer wählt Speicherort (Downloads, Google Drive, …)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE),
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    val running = backupState is BackupState.Running

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)

        // --- Design: Hell / Dunkel / Wie System (Standard); wirkt sofort ohne Neustart
        SettingsHeader(stringResource(R.string.settings_design))
        SettingsCard {
            Column(Modifier.selectableGroup()) {
                listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                    val (label, icon) = when (mode) {
                        ThemeMode.LIGHT -> R.string.theme_light to Icons.Outlined.LightMode
                        ThemeMode.DARK -> R.string.theme_dark to Icons.Outlined.DarkMode
                        ThemeMode.SYSTEM -> R.string.theme_system to Icons.Outlined.PhoneAndroid
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) }, role = Role.RadioButton)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(label),
                            Modifier.weight(1f).padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        RadioButton(selected = themeMode == mode, onClick = null)
                    }
                }
            }
        }

        SettingsHeader(stringResource(R.string.settings_backup))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_export), style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text(stringResource(R.string.backup_export_desc)) },
                leadingContent = { Icon(Icons.Outlined.Upload, null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(enabled = !running, onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) }),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_import), style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text(stringResource(R.string.backup_import_desc)) },
                leadingContent = { Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(
                    enabled = !running,
                    onClick = { importLauncher.launch(arrayOf(BackupFormat.MIME_TYPE, "application/x-zip-compressed", "application/octet-stream")) },
                ),
            )
        }

        SettingsHeader(stringResource(R.string.settings_about))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall) },
                supportingContent = {
                    Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BackupFormat.CURRENT_VERSION))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }

    // --- Ersetzen oder Zusammenführen?
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_mode_title)) },
            text = { Text(stringResource(R.string.import_mode_message)) },
            confirmButton = {
                TextButton(onClick = { pendingImport = null; viewModel.import(uri, ImportMode.MERGE) }) {
                    Text(stringResource(R.string.import_merge))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { pendingImport = null; viewModel.import(uri, ImportMode.REPLACE) }) {
                        Text(stringResource(R.string.import_replace), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    BackupStateDialog(backupState, onDismiss = viewModel::dismissResult)
}

/** Weiße, abgerundete Fläche für eine Einstellungsgruppe. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(CardShape, 6.dp)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
    )
}

@Composable
private fun BackupStateDialog(state: BackupState, onDismiss: () -> Unit) {
    when (state) {
        BackupState.Idle -> Unit
        is BackupState.Running -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(if (state.isExport) R.string.backup_exporting else R.string.backup_importing)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(state.progress * 100).toInt()} %")
                }
            },
            confirmButton = {},
        )
        is BackupState.ExportDone -> ResultDialog(
            success = true,
            title = stringResource(R.string.backup_export_success),
            message = pluralStringResource(R.plurals.backup_export_result, state.result.recipeCount, state.result.recipeCount) +
                " " + pluralStringResource(R.plurals.backup_photos, state.result.photoCount, state.result.photoCount),
            onDismiss = onDismiss,
        )
        is BackupState.ImportDone -> ResultDialog(
            success = true,
            title = stringResource(R.string.backup_import_success),
            message = stringResource(
                R.string.backup_import_result,
                state.result.imported,
                state.result.updated,
                state.result.skipped,
            ),
            onDismiss = onDismiss,
        )
        is BackupState.Failed -> ResultDialog(
            success = false,
            title = stringResource(if (state.isExport) R.string.backup_export_failed else R.string.backup_import_failed),
            message = state.message ?: stringResource(R.string.backup_unknown_error),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ResultDialog(success: Boolean, title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                null,
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
