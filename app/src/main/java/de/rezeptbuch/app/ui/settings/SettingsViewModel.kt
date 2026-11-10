package de.rezeptbuch.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rezeptbuch.app.data.backup.BackupException
import de.rezeptbuch.app.domain.model.ExportResult
import de.rezeptbuch.app.domain.model.ImportMode
import de.rezeptbuch.app.domain.model.ImportResult
import de.rezeptbuch.app.domain.model.ThemeMode
import de.rezeptbuch.app.domain.repository.BackupRepository
import de.rezeptbuch.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface BackupState {
    data object Idle : BackupState
    data class Running(val isExport: Boolean, val progress: Float) : BackupState
    data class ExportDone(val result: ExportResult) : BackupState
    data class ImportDone(val result: ImportResult) : BackupState

    /** [message] ist eine deutschsprachige Fehlermeldung oder `null` (→ generische Meldung). */
    data class Failed(val isExport: Boolean, val message: String?) : BackupState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    /** Vorgeschlagener Dateiname, z. B. `rezepte-backup-2026-11-10.zip`. */
    fun suggestedBackupFileName(): String = "rezepte-backup-${LocalDate.now()}.zip"

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }

    fun export(target: Uri) = runBackup(isExport = true) {
        BackupState.ExportDone(backupRepository.export(target) { p -> progress(true, p) })
    }

    fun import(source: Uri, mode: ImportMode) = runBackup(isExport = false) {
        BackupState.ImportDone(backupRepository.import(source, mode) { p -> progress(false, p) })
    }

    fun dismissResult() {
        if (_backupState.value !is BackupState.Running) _backupState.value = BackupState.Idle
    }

    private fun progress(isExport: Boolean, value: Float) {
        _backupState.value = BackupState.Running(isExport, value.coerceIn(0f, 1f))
    }

    private fun runBackup(isExport: Boolean, block: suspend () -> BackupState) {
        if (_backupState.value is BackupState.Running) return
        _backupState.value = BackupState.Running(isExport, 0f)
        viewModelScope.launch {
            _backupState.value = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                BackupState.Failed(isExport, e.message)
            } catch (e: Exception) {
                BackupState.Failed(isExport, null)
            }
        }
    }
}
