package de.rezeptbuch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Umschalter als abgerundete Pille. Der weiße Reiter folgt dem Pager stufenlos,
 * auch während man seitlich wischt.
 */
@Composable
fun PillTabs(
    titles: List<String>,
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, trackShape)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / titles.size
        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
        Box(
            Modifier
                .offset(x = tabWidth * position)
                .width(tabWidth)
                .fillMaxHeight()
                .softShadow(trackShape, 4.dp)
                .background(MaterialTheme.colorScheme.surface, trackShape),
        )
        Row(Modifier.fillMaxSize()) {
            titles.forEachIndexed { index, title ->
                val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(index) }
                        .semantics {
                            role = Role.Tab
                            selected = pagerState.currentPage == index
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onSurface, closeness),
                    )
                }
            }
        }
    }
}
