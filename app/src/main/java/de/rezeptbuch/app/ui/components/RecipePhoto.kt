package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/** Zeigt ein Rezeptfoto aus dem internen Speicher oder einen Platzhalter. */
@Composable
fun RecipePhoto(
    photoFileName: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 40.dp,
) {
    val context = LocalContext.current
    if (photoFileName != null) {
        AsyncImage(
            model = File(File(context.filesDir, "photos"), photoFileName),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.RestaurantMenu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(placeholderIconSize),
            )
        }
    }
}
