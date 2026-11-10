package de.rezeptbuch.app.domain.model

/**
 * Typisierte Mengeneinheiten. [RoundingRule] steuert, wie skalierte Mengen gerundet werden.
 * Gespeichert wird in der Datenbank und im Backup der Enum-Name – nie umbenennen!
 */
enum class IngredientUnit(val label: String, val rounding: RoundingRule) {
    GRAM("g", RoundingRule.WHOLE),
    KILOGRAM("kg", RoundingRule.DECIMAL),
    MILLILITER("ml", RoundingRule.WHOLE),
    LITER("l", RoundingRule.DECIMAL),
    TEASPOON("TL", RoundingRule.DECIMAL),
    TABLESPOON("EL", RoundingRule.DECIMAL),
    CUP("Tasse", RoundingRule.DECIMAL),
    PIECE("Stück", RoundingRule.COUNTABLE),
    PINCH("Prise", RoundingRule.COUNTABLE),
    CLOVE("Zehe", RoundingRule.COUNTABLE),
    BUNCH("Bund", RoundingRule.COUNTABLE),
    CAN("Dose", RoundingRule.COUNTABLE),
    PACKAGE("Pck.", RoundingRule.COUNTABLE),
    SLICE("Scheibe", RoundingRule.COUNTABLE),
    /** Ohne Einheit, z. B. „3 Eier“ – wird wie zählbare Stücke behandelt. */
    NONE("", RoundingRule.COUNTABLE),
    ;

    companion object {
        fun fromName(name: String?): IngredientUnit =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

enum class RoundingRule {
    /** Ganze Zahlen (g, ml); nur Mengen unter 1 mit einer Nachkommastelle (z. B. 0,5 g Safran). */
    WHOLE,

    /** Eine Nachkommastelle bei kleinen Mengen (< 10), sonst ganze Zahlen. */
    DECIMAL,

    /** Zählbare Dinge: ganze Zahlen, Zwischenwerte als Spanne „1–2“. */
    COUNTABLE,
}
