package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.portion.PortionCalculator

@Composable
fun ServingsStepper(
    servings: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onChange(servings - 1) },
            enabled = servings > PortionCalculator.MIN_SERVINGS,
        ) { Icon(Icons.Filled.Remove, stringResource(R.string.servings_decrease)) }
        Text(
            text = pluralStringResource(R.plurals.servings, servings, servings),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 112.dp),
        )
        FilledTonalIconButton(
            onClick = { onChange(servings + 1) },
            enabled = servings < PortionCalculator.MAX_SERVINGS,
        ) { Icon(Icons.Filled.Add, stringResource(R.string.servings_increase)) }
    }
}
