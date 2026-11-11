package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryColor
import de.rezeptbuch.app.domain.model.CategoryIcon
import de.rezeptbuch.app.ui.theme.tint

/** Runder, farbiger Symbol-Kreis einer Kategorie. */
@Composable
fun CategoryCircle(icon: CategoryIcon, color: CategoryColor, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val tint = color.tint()
    Box(
        modifier.size(size).softShadow(CircleShape, 4.dp).background(tint.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon.vector, null, tint = tint.content, modifier = Modifier.size(size * 0.45f))
    }
}

/** Kreis mit Beschriftung darunter – für die Schnellzugriffe auf der Startseite. */
@Composable
fun CategoryBubble(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .width(84.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryCircle(category.icon, category.color)
        Text(
            category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Kachel im Reiter „Kategorien“: Symbol, Name und Anzahl der Rezepte. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryTile(
    category: Category,
    recipeCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .softShadow(shape, 8.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryCircle(category.icon, category.color, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.recipe_count, recipeCount, recipeCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
