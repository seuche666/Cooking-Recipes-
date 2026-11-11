package de.rezeptbuch.app.data.seed

import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryColor
import de.rezeptbuch.app.domain.model.CategoryIcon

/**
 * Standard-Kategorien. Die festen UUIDs sorgen dafür, dass „Zusammenführen“ beim Backup-Import
 * dieselbe Kategorie auf verschiedenen Geräten wiedererkennt und nicht verdoppelt.
 */
object DefaultCategories {
    val BREAKFAST = Category(uuid = "default-breakfast", name = "Frühstück", icon = CategoryIcon.BREAKFAST, color = CategoryColor.AMBER, position = 0)
    val MAIN = Category(uuid = "default-main", name = "Hauptgericht", icon = CategoryIcon.MAIN, color = CategoryColor.BLUE, position = 1)
    val DESSERT = Category(uuid = "default-dessert", name = "Dessert", icon = CategoryIcon.DESSERT, color = CategoryColor.PINK, position = 2)
    val BAKING = Category(uuid = "default-baking", name = "Backen", icon = CategoryIcon.BAKING, color = CategoryColor.GREEN, position = 3)

    val all = listOf(BREAKFAST, MAIN, DESSERT, BAKING)
}
