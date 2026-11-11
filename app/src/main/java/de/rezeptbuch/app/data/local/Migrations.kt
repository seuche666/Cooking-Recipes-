package de.rezeptbuch.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.rezeptbuch.app.data.seed.DefaultCategories

/**
 * Datenbank-Migrationen. Bestehende Rezepte, Notizen und Fotos bleiben unverändert erhalten;
 * es kommen nur neue Tabellen bzw. Spalten hinzu. Abgesichert durch `MigrationTest`.
 */
object Migrations {

    /** 1 → 2: Kategorien, Rezept-Kategorie-Zuordnung, Spalte `last_cooked_at`. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `recipes` ADD COLUMN `last_cooked_at` INTEGER")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uuid` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, " +
                    "`color` TEXT NOT NULL, `position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_uuid` ON `categories` (`uuid`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recipe_categories` (" +
                    "`recipe_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`recipe_id`, `category_id`), " +
                    "FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_categories_recipe_id` ON `recipe_categories` (`recipe_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_categories_category_id` ON `recipe_categories` (`category_id`)")

            // Standard-Kategorien auch für bestehende Installationen anlegen
            val now = System.currentTimeMillis()
            DefaultCategories.all.forEachIndexed { index, c ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`uuid`, `name`, `icon`, `color`, `position`, `created_at`) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(c.uuid, c.name, c.icon.name, c.color.name, index, now),
                )
            }
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
