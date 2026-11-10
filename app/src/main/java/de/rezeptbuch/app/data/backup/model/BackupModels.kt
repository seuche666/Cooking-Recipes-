package de.rezeptbuch.app.data.backup.model

import kotlinx.serialization.Serializable

/**
 * Backup-Format (Datei `backup.json` im ZIP). Bilder liegen unter `images/`.
 *
 * Versionierung: [formatVersion] wird bei JEDER inkompatiblen Änderung erhöht.
 * Ältere Versionen werden beim Import über [de.rezeptbuch.app.data.backup.BackupMigrations]
 * auf das aktuelle Format gebracht. Neue Felder immer mit Default-Wert anlegen,
 * damit alte Backups ohne Migration lesbar bleiben.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val appVersion: String = "",
    val exportedAt: String = "",
    val recipes: List<BackupRecipe> = emptyList(),
)

@Serializable
data class BackupRecipe(
    val uuid: String,
    val title: String,
    /** Pfad innerhalb des ZIP, z. B. `images/abc.jpg`, oder `null`. */
    val photo: String? = null,
    val baseServings: Int = 1,
    val cookTimeMinutes: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
data class BackupIngredient(
    val amount: Double? = null,
    /** Name von [de.rezeptbuch.app.domain.model.IngredientUnit]; unbekannte Werte → NONE. */
    val unit: String = "NONE",
    val name: String,
)

@Serializable
data class BackupStep(
    val text: String,
    val timerSeconds: Int? = null,
)

@Serializable
data class BackupNote(
    val text: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
