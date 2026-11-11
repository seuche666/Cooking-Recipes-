package de.rezeptbuch.app.domain.model

/** Eine vom Nutzer verwaltete Kategorie, z. B. „Frühstück“. */
data class Category(
    val id: Long = 0,
    /** Stabile ID für Backups („Zusammenführen“ erkennt dieselbe Kategorie wieder). */
    val uuid: String,
    val name: String,
    val icon: CategoryIcon,
    val color: CategoryColor,
    val position: Int = 0,
)

data class CategoryWithCount(val category: Category, val recipeCount: Int)

/**
 * Wählbare Symbole. Gespeichert wird der Enum-Name – nie umbenennen!
 * Die Zuordnung zu den Material-Icons passiert in der UI-Schicht.
 */
enum class CategoryIcon {
    BREAKFAST, MAIN, SOUP, DESSERT, BAKING, CAKE, COOKIE, SALAD, VEGGIE, FISH,
    MEAT, PIZZA, PASTA, BURGER, EGG, DRINK, COCKTAIL, SNACK, RESTAURANT,
    ;

    companion object {
        fun fromName(name: String?): CategoryIcon = entries.firstOrNull { it.name == name } ?: RESTAURANT
    }
}

/**
 * Farbfamilien. Die konkreten Töne für Hell- und Dunkelmodus legt das Theme fest,
 * damit Symbol und Hintergrund in beiden Varianten gut lesbar sind. Nie umbenennen!
 */
enum class CategoryColor {
    AMBER, BLUE, PINK, GREEN, PURPLE, ORANGE, TEAL, RED,
    ;

    companion object {
        fun fromName(name: String?): CategoryColor = entries.firstOrNull { it.name == name } ?: BLUE
    }
}
