package de.rezeptbuch.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.KebabDining
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import de.rezeptbuch.app.domain.model.CategoryIcon

/** Material-Symbol zu jedem [CategoryIcon]. */
val CategoryIcon.vector: ImageVector
    get() = when (this) {
        CategoryIcon.BREAKFAST -> Icons.Outlined.WbSunny
        CategoryIcon.MAIN -> Icons.Outlined.RiceBowl
        CategoryIcon.SOUP -> Icons.Outlined.SoupKitchen
        CategoryIcon.DESSERT -> Icons.Outlined.Icecream
        CategoryIcon.BAKING -> Icons.Outlined.BakeryDining
        CategoryIcon.CAKE -> Icons.Outlined.Cake
        CategoryIcon.COOKIE -> Icons.Outlined.Cookie
        CategoryIcon.SALAD -> Icons.Outlined.Spa
        CategoryIcon.VEGGIE -> Icons.Outlined.Eco
        CategoryIcon.FISH -> Icons.Outlined.SetMeal
        CategoryIcon.MEAT -> Icons.Outlined.KebabDining
        CategoryIcon.PIZZA -> Icons.Outlined.LocalPizza
        CategoryIcon.PASTA -> Icons.Outlined.RamenDining
        CategoryIcon.BURGER -> Icons.Outlined.LunchDining
        CategoryIcon.EGG -> Icons.Outlined.Egg
        CategoryIcon.DRINK -> Icons.Outlined.EmojiFoodBeverage
        CategoryIcon.COCKTAIL -> Icons.Outlined.LocalBar
        CategoryIcon.SNACK -> Icons.Outlined.Fastfood
        CategoryIcon.RESTAURANT -> Icons.Outlined.Restaurant
    }
