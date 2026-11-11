package de.rezeptbuch.app.data

import de.rezeptbuch.app.data.backup.BackupException
import de.rezeptbuch.app.data.backup.BackupFormat
import de.rezeptbuch.app.data.backup.BackupMigrations
import de.rezeptbuch.app.data.backup.model.BackupFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun read(text: String): BackupFile =
        json.decodeFromJsonElement(BackupFile.serializer(), BackupMigrations.migrate(json.parseToJsonElement(text).jsonObject))

    @Test
    fun `Sicherung im alten Format 1 wird weiterhin gelesen`() {
        val backup = read(
            """
            {
              "formatVersion": 1,
              "appVersion": "1.0.7",
              "exportedAt": "2026-11-10T11:00:00Z",
              "recipes": [{
                "uuid": "abc", "title": "Pfannkuchen", "photo": "images/p.jpg", "baseServings": 4,
                "createdAt": 1, "updatedAt": 2,
                "ingredients": [{ "amount": 250.0, "unit": "GRAM", "name": "Mehl" }],
                "steps": [{ "text": "Rühren", "timerSeconds": 60 }],
                "notes": [{ "text": "Lecker", "createdAt": 3, "updatedAt": 3 }]
              }]
            }
            """.trimIndent(),
        )
        assertEquals(1, backup.recipes.size)
        val recipe = backup.recipes.single()
        assertEquals("Pfannkuchen", recipe.title)
        assertEquals("images/p.jpg", recipe.photo)
        assertEquals(listOf("Mehl"), recipe.ingredients.map { it.name })
        assertTrue("Format 1 kennt keine Kategorien", backup.categories.isEmpty())
        assertTrue(recipe.categories.isEmpty())
        assertNull(recipe.lastCookedAt)
    }

    @Test
    fun `Format 2 enthaelt Kategorien und Zuordnungen`() {
        val backup = read(
            """
            {
              "formatVersion": 2,
              "categories": [{ "uuid": "default-main", "name": "Hauptgericht", "icon": "MAIN", "color": "BLUE", "position": 1 }],
              "recipes": [{ "uuid": "x", "title": "Suppe", "categories": ["default-main"], "lastCookedAt": 99 }]
            }
            """.trimIndent(),
        )
        assertEquals("Hauptgericht", backup.categories.single().name)
        assertEquals(listOf("default-main"), backup.recipes.single().categories)
        assertEquals(99L, backup.recipes.single().lastCookedAt)
    }

    @Test(expected = BackupException::class)
    fun `Sicherung aus neuerer App-Version wird abgelehnt`() {
        read("""{ "formatVersion": ${BackupFormat.CURRENT_VERSION + 1}, "recipes": [] }""")
    }
}
