package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.rezeptbuch.app.ui.theme.placeholderTint
import java.io.File

/** Zeigt ein Rezeptfoto aus dem internen Speicher oder einen Platzhalter. */
@Composable
fun RecipePhoto(
    photoFileName: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 40.dp,
    /** Bestimmt die Platzhalterfarbe (z. B. der Rezepttitel), damit nicht alles gleich aussieht. */
    placeholderSeed: String = contentDescription.orEmpty(),
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
        val tint = placeholderTint(placeholderSeed)
        Box(modifier.background(tint.container), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.RiceBowl,
                contentDescription = null,
                tint = tint.content,
                modifier = Modifier.size(placeholderIconSize),
            )
        }
    }
}
