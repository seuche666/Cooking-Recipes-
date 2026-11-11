package de.rezeptbuch.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import de.rezeptbuch.app.domain.model.CategoryColor

/** Hintergrund- und Symbolfarbe einer Kategorie – für Hell und Dunkel getrennt abgestimmt. */
data class CategoryTint(val container: Color, val content: Color)

private val light = mapOf(
    CategoryColor.AMBER to CategoryTint(Color(0xFFFFF1C2), Color(0xFFB45309)),
    CategoryColor.BLUE to CategoryTint(Color(0xFFDCE8FF), Color(0xFF2F5BD3)),
    CategoryColor.PINK to CategoryTint(Color(0xFFFDE2EC), Color(0xFFBE185D)),
    CategoryColor.GREEN to CategoryTint(Color(0xFFD9F5E3), Color(0xFF15803D)),
    CategoryColor.PURPLE to CategoryTint(Color(0xFFEDE3FF), Color(0xFF7C3AED)),
    CategoryColor.ORANGE to CategoryTint(Color(0xFFFFE4D2), Color(0xFFC2410C)),
    CategoryColor.TEAL to CategoryTint(Color(0xFFD3F3F1), Color(0xFF0F766E)),
    CategoryColor.RED to CategoryTint(Color(0xFFFDE0E0), Color(0xFFB91C1C)),
)

private val dark = mapOf(
    CategoryColor.AMBER to CategoryTint(Color(0xFF453712), Color(0xFFFCD34D)),
    CategoryColor.BLUE to CategoryTint(Color(0xFF1E2E55), Color(0xFF9DB8FF)),
    CategoryColor.PINK to CategoryTint(Color(0xFF4A1D31), Color(0xFFF9A8C9)),
    CategoryColor.GREEN to CategoryTint(Color(0xFF163B26), Color(0xFF86EFAC)),
    CategoryColor.PURPLE to CategoryTint(Color(0xFF2F2352), Color(0xFFC4B5FD)),
    CategoryColor.ORANGE to CategoryTint(Color(0xFF4A2615), Color(0xFFFDBA74)),
    CategoryColor.TEAL to CategoryTint(Color(0xFF12393A), Color(0xFF5EEAD4)),
    CategoryColor.RED to CategoryTint(Color(0xFF4A1C1C), Color(0xFFFCA5A5)),
)

@Composable
@ReadOnlyComposable
fun CategoryColor.tint(): CategoryTint = (if (LocalDarkTheme.current) dark else light).getValue(this)

/** Farbe für Fotoplatzhalter: stabil pro Rezepttitel, damit die Liste abwechslungsreich wirkt. */
@Composable
@ReadOnlyComposable
fun placeholderTint(seed: String): CategoryTint {
    val colors = CategoryColor.entries
    return colors[(seed.hashCode() and 0x7FFFFFFF) % colors.size].tint()
}
