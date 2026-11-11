package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.RecipeSummary
import de.rezeptbuch.app.domain.portion.formatMinutes

val CardShape = RoundedCornerShape(24.dp)

/** Große weiße Rezeptkarte mit Foto, runden Ecken und weichem Schatten (Rezeptliste). */
@Composable
fun RecipeCard(recipe: RecipeSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .softShadow(CardShape)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        RecipePhoto(
            photoFileName = recipe.photoFileName,
            contentDescription = recipe.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            placeholderIconSize = 48.dp,
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            RecipeMeta(recipe, Modifier.padding(top = 4.dp))
            if (recipe.matchedIngredients.isNotEmpty()) {
                Text(
                    stringResource(R.string.search_contains, recipe.matchedIngredients.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Schmale Karte für waagerechte Listen auf der Startseite. */
@Composable
fun RecipeCardCompact(recipe: RecipeSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .width(220.dp)
            .pressScale(interaction)
            .softShadow(CardShape)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        RecipePhoto(
            photoFileName = recipe.photoFileName,
            contentDescription = recipe.title,
            modifier = Modifier.fillMaxWidth().height(132.dp),
            placeholderIconSize = 40.dp,
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            RecipeMeta(recipe, Modifier.padding(top = 2.dp))
        }
    }
}

/** „25 Min · 2 Portionen“ */
@Composable
fun RecipeMeta(recipe: RecipeSummary, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        recipe.cookTimeMinutes?.let {
            Icon(Icons.Outlined.Schedule, null, Modifier.size(15.dp), tint = color)
            Text(
                formatMinutes(it),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
            )
        }
        Icon(Icons.Outlined.People, null, Modifier.size(15.dp), tint = color)
        Text(
            pluralStringResource(R.plurals.servings, recipe.baseServings, recipe.baseServings),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Abschnittsüberschrift mit optionaler Aktion rechts („Alle“). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
