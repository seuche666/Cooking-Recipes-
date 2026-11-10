package de.rezeptbuch.app.domain.portion

import de.rezeptbuch.app.domain.model.Ingredient
import de.rezeptbuch.app.domain.model.IngredientUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PortionCalculatorTest {

    // ---------------------------------------------------------------- Proportionale Umrechnung

    @Test
    fun `scaleAmount verdoppelt bei doppelter Portionszahl`() {
        assertEquals(500.0, PortionCalculator.scaleAmount(250.0, 2, 4), 1e-9)
    }

    @Test
    fun `scaleAmount halbiert und drittelt korrekt`() {
        assertEquals(125.0, PortionCalculator.scaleAmount(250.0, 4, 2), 1e-9)
        assertEquals(1.0, PortionCalculator.scaleAmount(3.0, 3, 1), 1e-9)
        assertEquals(100.0 / 3, PortionCalculator.scaleAmount(50.0, 3, 2), 1e-9)
    }

    @Test
    fun `gleiche Portionszahl liefert unveraenderte Menge`() {
        assertEquals(42.0, PortionCalculator.scaleAmount(42.0, 4, 4), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `null Grundportionen sind ungueltig`() {
        PortionCalculator.scaleAmount(1.0, 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `null Zielportionen sind ungueltig`() {
        PortionCalculator.scaleAmount(1.0, 2, 0)
    }

    @Test
    fun `scale laesst gespeicherte Zutat unveraendert`() {
        val original = Ingredient(200.0, IngredientUnit.GRAM, "Mehl")
        val scaled = PortionCalculator.scale(original, baseServings = 2, targetServings = 5)
        assertEquals(500.0, scaled.exactAmount!!, 1e-9)
        assertEquals("500", scaled.amountText)
        assertSame(original, scaled.original)
        assertEquals(200.0, original.amount!!, 0.0) // Originaldaten bleiben unverändert
    }

    @Test
    fun `scale ohne Mengenangabe zeigt nur den Namen`() {
        val scaled = PortionCalculator.scale(Ingredient(null, IngredientUnit.NONE, "Pfeffer"), 2, 6)
        assertNull(scaled.exactAmount)
        assertEquals("", scaled.amountText)
        assertEquals("Pfeffer", scaled.displayText)
    }

    @Test
    fun `scale fuer Liste behaelt Reihenfolge`() {
        val list = listOf(
            Ingredient(100.0, IngredientUnit.GRAM, "Zucker"),
            Ingredient(2.0, IngredientUnit.NONE, "Eier"),
            Ingredient(1.0, IngredientUnit.TEASPOON, "Vanille"),
        )
        val scaled = PortionCalculator.scale(list, baseServings = 4, targetServings = 6)
        assertEquals(listOf("150 g Zucker", "3 Eier", "1,5 TL Vanille"), scaled.map { it.displayText })
    }

    // ---------------------------------------------------------------- Rundung: Gramm/Milliliter

    @Test
    fun `Gramm werden auf ganze Zahlen gerundet`() {
        assertEquals("167", PortionCalculator.formatAmount(166.666, IngredientUnit.GRAM))
        assertEquals("83", PortionCalculator.formatAmount(83.33, IngredientUnit.GRAM))
        assertEquals("8", PortionCalculator.formatAmount(7.5, IngredientUnit.GRAM))
        assertEquals("1250", PortionCalculator.formatAmount(1250.4, IngredientUnit.MILLILITER))
    }

    @Test
    fun `sehr kleine Grammangaben behalten eine Nachkommastelle`() {
        assertEquals("0,5", PortionCalculator.formatAmount(0.5, IngredientUnit.GRAM))
        assertEquals("0,1", PortionCalculator.formatAmount(0.01, IngredientUnit.GRAM)) // nie „0“
    }

    // ---------------------------------------------------------------- Rundung: kleine Mengen

    @Test
    fun `kleine Mengen erhalten eine Nachkommastelle mit Komma`() {
        assertEquals("1,5", PortionCalculator.formatAmount(1.5, IngredientUnit.TABLESPOON))
        assertEquals("0,3", PortionCalculator.formatAmount(1.0 / 3, IngredientUnit.TEASPOON))
        assertEquals("2,7", PortionCalculator.formatAmount(2.666, IngredientUnit.LITER))
    }

    @Test
    fun `ueberfluessiges Komma-Null entfaellt`() {
        assertEquals("2", PortionCalculator.formatAmount(2.0, IngredientUnit.TABLESPOON))
        assertEquals("3", PortionCalculator.formatAmount(2.96, IngredientUnit.KILOGRAM))
    }

    @Test
    fun `groessere Dezimalmengen werden ganzzahlig`() {
        assertEquals("13", PortionCalculator.formatAmount(12.5, IngredientUnit.TABLESPOON))
        assertEquals("10", PortionCalculator.formatAmount(10.2, IngredientUnit.CUP))
    }

    // ---------------------------------------------------------------- Rundung: zählbare Mengen

    @Test
    fun `anderthalb Eier werden als Spanne angezeigt`() {
        assertEquals("1–2", PortionCalculator.formatAmount(1.5, IngredientUnit.NONE))
        assertEquals("1–2", PortionCalculator.formatAmount(1.5, IngredientUnit.PIECE))
    }

    @Test
    fun `beliebige Zwischenwerte werden zur Spanne`() {
        assertEquals("2–3", PortionCalculator.formatAmount(2.25, IngredientUnit.PIECE))
        assertEquals("4–5", PortionCalculator.formatAmount(4.666, IngredientUnit.CLOVE))
    }

    @Test
    fun `fast ganze Stueckzahlen werden zur ganzen Zahl`() {
        assertEquals("3", PortionCalculator.formatAmount(3.0, IngredientUnit.NONE))
        assertEquals("3", PortionCalculator.formatAmount(2.999_999, IngredientUnit.NONE))
        assertEquals("2", PortionCalculator.formatAmount(2.02, IngredientUnit.PIECE))
    }

    @Test
    fun `weniger als ein Stueck ergibt mindestens eins`() {
        assertEquals("1", PortionCalculator.formatAmount(0.5, IngredientUnit.PINCH))
        assertEquals("1", PortionCalculator.formatAmount(0.25, IngredientUnit.NONE))
    }

    @Test
    fun `Beispiel - 3 Eier fuer 2 Portionen auf 1 Portion`() {
        val scaled = PortionCalculator.scale(Ingredient(3.0, IngredientUnit.NONE, "Eier"), 2, 1)
        assertEquals("1–2 Eier", scaled.displayText)
    }

    @Test
    fun `Beispiel - 3 Eier fuer 4 Portionen auf 8 Portionen`() {
        val scaled = PortionCalculator.scale(Ingredient(3.0, IngredientUnit.NONE, "Eier"), 4, 8)
        assertEquals("6 Eier", scaled.displayText)
    }

    // ---------------------------------------------------------------- Ein-/Ausgabe

    @Test
    fun `parseAmount akzeptiert Komma und Punkt`() {
        assertEquals(1.5, PortionCalculator.parseAmount("1,5")!!, 0.0)
        assertEquals(0.25, PortionCalculator.parseAmount(" 0.25 ")!!, 0.0)
        assertNull(PortionCalculator.parseAmount(""))
        assertNull(PortionCalculator.parseAmount("abc"))
        assertNull(PortionCalculator.parseAmount("0"))
    }

    @Test
    fun `amountToInput formatiert fuer Eingabefelder`() {
        assertEquals("2", PortionCalculator.amountToInput(2.0))
        assertEquals("1,5", PortionCalculator.amountToInput(1.5))
        assertEquals("", PortionCalculator.amountToInput(null))
    }

    @Test
    fun `TimerInput parst Minuten und mm-ss`() {
        assertEquals(300, TimerInput.parse("5"))
        assertEquals(90, TimerInput.parse("1,5"))
        assertEquals(90, TimerInput.parse("1:30"))
        assertNull(TimerInput.parse(""))
        assertEquals("1:30", TimerInput.format(90))
        assertEquals("5", TimerInput.format(300))
    }

    @Test
    fun `formatTimer zeigt Minuten und Stunden`() {
        assertEquals("05:00", formatTimer(300))
        assertEquals("1:01:05", formatTimer(3665))
    }
}
