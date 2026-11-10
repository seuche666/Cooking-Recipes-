package de.rezeptbuch.app.domain.portion

import de.rezeptbuch.app.domain.model.Ingredient
import de.rezeptbuch.app.domain.model.IngredientUnit
import de.rezeptbuch.app.domain.model.RoundingRule
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** Eine für die Anzeige umgerechnete Zutat. Die gespeicherte [Ingredient] bleibt unverändert. */
data class ScaledIngredient(
    val original: Ingredient,
    /** Exakter, ungerundeter Wert (oder `null` ohne Mengenangabe). */
    val exactAmount: Double?,
    /** Gerundete, deutsch formatierte Menge, z. B. „1,5“, „250“ oder „1–2“. Leer ohne Menge. */
    val amountText: String,
) {
    val unitLabel: String get() = original.unit.label
    val name: String get() = original.name

    /** „250 g Mehl“, „1–2 Eier“, „Salz“ */
    val displayText: String
        get() = listOf(amountText, unitLabel, name).filter { it.isNotBlank() }.joinToString(" ")
}

/**
 * Rechnet Zutatenmengen proportional auf eine Ziel-Portionszahl um und rundet sinnvoll.
 * Reine Kotlin-Logik ohne Android-Abhängigkeiten – vollständig unit-getestet.
 */
object PortionCalculator {

    const val MIN_SERVINGS = 1
    const val MAX_SERVINGS = 99

    private const val EPSILON = 0.05

    fun scaleAmount(amount: Double, baseServings: Int, targetServings: Int): Double {
        require(baseServings > 0) { "baseServings muss > 0 sein" }
        require(targetServings > 0) { "targetServings muss > 0 sein" }
        return amount * targetServings / baseServings
    }

    fun scale(ingredients: List<Ingredient>, baseServings: Int, targetServings: Int): List<ScaledIngredient> =
        ingredients.map { scale(it, baseServings, targetServings) }

    fun scale(ingredient: Ingredient, baseServings: Int, targetServings: Int): ScaledIngredient {
        val exact = ingredient.amount?.let { scaleAmount(it, baseServings, targetServings) }
        return ScaledIngredient(
            original = ingredient,
            exactAmount = exact,
            amountText = exact?.let { formatAmount(it, ingredient.unit) }.orEmpty(),
        )
    }

    /** Rundet und formatiert eine Menge abhängig von der Einheit. */
    fun formatAmount(value: Double, unit: IngredientUnit): String {
        if (value <= 0.0) return "0"
        return when (unit.rounding) {
            RoundingRule.WHOLE -> if (value < 1) formatDecimal(value) else formatWhole(value)
            RoundingRule.DECIMAL -> if (value < 10) formatDecimal(value) else formatWhole(value)
            RoundingRule.COUNTABLE -> formatCountable(value)
        }
    }

    /** Ganze Zahl, wenn nah dran; sonst Spanne „abgerundet–aufgerundet“; unter 1 mindestens „1“. */
    internal fun formatCountable(value: Double): String {
        val nearest = value.roundToLong()
        return when {
            abs(value - nearest) < EPSILON && nearest >= 1 -> nearest.toString()
            value < 1.0 -> "1"
            else -> "${floor(value).toLong()}–${ceil(value).toLong()}"
        }
    }

    /** Ganze Zahl (kaufmännisch gerundet). */
    internal fun formatWhole(value: Double): String = value.roundToLong().coerceAtLeast(1).toString()

    /** Eine Nachkommastelle mit deutschem Komma, ohne überflüssiges „,0“; nie 0 bei positiver Menge. */
    internal fun formatDecimal(value: Double): String {
        val tenths = (value * 10).roundToLong().coerceAtLeast(1)
        val whole = tenths / 10
        val fraction = tenths % 10
        return if (fraction == 0L) whole.toString() else "$whole,$fraction"
    }

    /** Parst Benutzereingaben wie „1,5“ oder „0.25“. Leere Eingabe → `null`. */
    fun parseAmount(input: String): Double? =
        input.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()?.takeIf { it > 0 }

    /** Formatiert eine gespeicherte Menge für ein Eingabefeld, z. B. 1.5 → „1,5“, 2.0 → „2“. */
    fun amountToInput(amount: Double?): String {
        if (amount == null) return ""
        val rounded = (amount * 1000).roundToLong() / 1000.0
        return if (rounded == floor(rounded)) rounded.toLong().toString()
        else rounded.toString().replace('.', ',')
    }
}
