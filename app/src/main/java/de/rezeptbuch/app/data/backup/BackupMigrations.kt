package de.rezeptbuch.app.data.backup

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * Hebt ältere Backup-JSONs schrittweise auf [BackupFormat.CURRENT_VERSION] an.
 *
 * Beispiel für eine künftige Version 2:
 * ```
 * 1 -> migrate1To2(json)   // z. B. Feld umbenennen
 * ```
 */
object BackupMigrations {

    fun readVersion(json: JsonObject): Int =
        json["formatVersion"]?.jsonPrimitive?.int ?: throw BackupException("Keine gültige Backup-Datei (Version fehlt).")

    fun migrate(json: JsonObject): JsonObject {
        var current = json
        var version = readVersion(json)
        if (version > BackupFormat.CURRENT_VERSION) {
            throw BackupException(
                "Dieses Backup stammt aus einer neueren App-Version (Format $version). Bitte die App aktualisieren.",
            )
        }
        if (version < 1) throw BackupException("Unbekanntes Backup-Format ($version).")
        while (version < BackupFormat.CURRENT_VERSION) {
            current = when (version) {
                // 1 -> migrate1To2(current)
                else -> current
            }
            version++
        }
        return current
    }
}

object BackupFormat {
    const val CURRENT_VERSION = 1
    const val JSON_ENTRY = "backup.json"
    const val IMAGE_DIR = "images/"
    const val MIME_TYPE = "application/zip"
}

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
