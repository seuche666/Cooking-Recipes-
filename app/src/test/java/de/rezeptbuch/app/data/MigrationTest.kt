package de.rezeptbuch.app.data

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.Migrations
import de.rezeptbuch.app.data.local.entity.RecipeCategoryCrossRef
import de.rezeptbuch.app.data.seed.DefaultCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Legt eine Datenbank im Format von Version 1 an (so wie sie auf den Geräten liegt), füllt sie mit
 * einem Rezept samt Zutat, Schritt, Notiz und Foto-Verweis und öffnet sie dann mit der aktuellen App.
 * Room führt die Migration aus und prüft das Ergebnis gegen das neue Schema – passt ein Detail
 * nicht, schlägt der Test fehl. Anschließend muss das Rezept unverändert vorhanden sein.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val name = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun `migration 1 auf 2 behaelt Rezepte und legt Kategorien an`() = runBlocking {
        createVersion1Database()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()

        val recipe = db.recipeDao().getRecipeWithDetails(1)
        assertNotNull("Rezept fehlt nach der Migration", recipe)
        recipe!!
        assertEquals("Omas Apfelkuchen", recipe.recipe.title)
        assertEquals("apfelkuchen.jpg", recipe.recipe.photoFileName)
        assertEquals(8, recipe.recipe.baseServings)
        assertNull(recipe.recipe.lastCookedAt)
        assertEquals(listOf("Äpfel"), recipe.ingredients.map { it.name })
        assertEquals(listOf("Äpfel schälen."), recipe.steps.map { it.text })
        assertEquals(listOf("Mehr Zimt!"), recipe.notes.map { it.text })
        assertEquals(emptyList<RecipeCategoryCrossRef>(), recipe.categoryRefs)

        // Standard-Kategorien wurden angelegt
        val categories = db.categoryDao().observeWithCounts().first()
        assertEquals(DefaultCategories.all.map { it.name }, categories.map { it.category.name })

        // Neue Funktionen arbeiten auf der migrierten Datenbank
        val categoryId = categories.first().category.id
        db.recipeDao().insertCategoryRefs(listOf(RecipeCategoryCrossRef(1, categoryId)))
        db.recipeDao().setLastCooked(1, 1234L)
        assertEquals(1, db.categoryDao().observeWithCounts().first().first().recipeCount)
        assertEquals(listOf(1L), db.recipeDao().observeRecentlyCooked(5).first().map { it.id })
        assertEquals(listOf(1L), db.recipeDao().observeRecipes("pfel").first().map { it.id })

        db.close()
    }

    /** Schema exakt wie von Room für Version 1 erzeugt. */
    private fun createVersion1Database() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, `photo_file_name` TEXT, `base_servings` INTEGER NOT NULL, " +
                    "`cook_time_minutes` INTEGER, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recipes_uuid` ON `recipes` (`uuid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_title` ON `recipes` (`title`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recipe_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `amount` REAL, `unit` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_recipe_id` ON `ingredients` (`recipe_id`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recipe_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, `timer_seconds` INTEGER, " +
                    "FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_recipe_id` ON `steps` (`recipe_id`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recipe_id` INTEGER NOT NULL, `text` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_recipe_id` ON `notes` (`recipe_id`)")

            db.insert("recipes", null, ContentValues().apply {
                put("id", 1L); put("uuid", "abc"); put("title", "Omas Apfelkuchen"); put("photo_file_name", "apfelkuchen.jpg")
                put("base_servings", 8); put("cook_time_minutes", 60); put("created_at", 1L); put("updated_at", 2L)
            })
            db.insert("ingredients", null, ContentValues().apply {
                put("recipe_id", 1L); put("position", 0); put("amount", 4.0); put("unit", "PIECE"); put("name", "Äpfel")
            })
            db.insert("steps", null, ContentValues().apply {
                put("recipe_id", 1L); put("position", 0); put("text", "Äpfel schälen.")
            })
            db.insert("notes", null, ContentValues().apply {
                put("recipe_id", 1L); put("text", "Mehr Zimt!"); put("created_at", 3L); put("updated_at", 3L)
            })
            db.version = 1
        }
    }
}
