package de.rezeptbuch.app.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ImportMode {
    /** Alle vorhandenen Rezepte löschen und durch das Backup ersetzen. */
    REPLACE,

    /** Vorhandene Rezepte behalten; gleiche Rezepte (UUID) werden durch die neuere Version ersetzt. */
    MERGE,
}

data class ImportResult(val imported: Int, val updated: Int, val skipped: Int)

data class ExportResult(val recipeCount: Int, val photoCount: Int)
