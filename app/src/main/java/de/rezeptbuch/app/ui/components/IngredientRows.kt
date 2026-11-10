package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.domain.portion.ScaledIngredient

/** Zutatenliste: Menge + Einheit rechtsbündig in einer Spalte, Name daneben. */
@Composable
fun IngredientRows(
    ingredients: List<ScaledIngredient>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ingredients.forEach { ing ->
            Row {
                Text(
                    text = listOf(ing.amountText, ing.unitLabel).filter { it.isNotBlank() }.joinToString(" "),
                    style = textStyle.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(96.dp).padding(end = 12.dp),
                )
                Text(ing.name, style = textStyle, modifier = Modifier.weight(1f))
            }
        }
    }
}
