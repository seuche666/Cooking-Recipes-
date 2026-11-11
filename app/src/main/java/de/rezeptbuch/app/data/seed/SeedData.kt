package de.rezeptbuch.app.data.seed

import de.rezeptbuch.app.domain.model.CookingStep
import de.rezeptbuch.app.domain.model.Ingredient
import de.rezeptbuch.app.domain.model.IngredientUnit.CLOVE
import de.rezeptbuch.app.domain.model.IngredientUnit.GRAM
import de.rezeptbuch.app.domain.model.IngredientUnit.LITER
import de.rezeptbuch.app.domain.model.IngredientUnit.MILLILITER
import de.rezeptbuch.app.domain.model.IngredientUnit.NONE
import de.rezeptbuch.app.domain.model.IngredientUnit.PIECE
import de.rezeptbuch.app.domain.model.IngredientUnit.PINCH
import de.rezeptbuch.app.domain.model.IngredientUnit.TABLESPOON
import de.rezeptbuch.app.domain.model.IngredientUnit.TEASPOON
import de.rezeptbuch.app.domain.model.Recipe
import java.time.Instant

/** Drei Beispielrezepte, die beim allerersten Start angelegt werden. */
object SeedData {

    data class SeedRecipe(
        val recipe: Recipe,
        val notes: List<String>,
        /** UUIDs aus [DefaultCategories]. */
        val categoryUuids: List<String> = emptyList(),
    )

    fun recipes(): List<SeedRecipe> {
        val now = Instant.now()
        fun recipe(
            uuid: String,
            title: String,
            servings: Int,
            minutes: Int,
            ingredients: List<Ingredient>,
            steps: List<CookingStep>,
        ) = Recipe(
            uuid = uuid,
            title = title,
            photoFileName = null,
            baseServings = servings,
            cookTimeMinutes = minutes,
            ingredients = ingredients,
            steps = steps,
            notes = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        return listOf(
            SeedRecipe(
                recipe(
                    uuid = "seed-spaghetti-carbonara",
                    title = "Spaghetti Carbonara",
                    servings = 2,
                    minutes = 25,
                    ingredients = listOf(
                        Ingredient(200.0, GRAM, "Spaghetti"),
                        Ingredient(100.0, GRAM, "Guanciale oder Pancetta"),
                        Ingredient(3.0, NONE, "Eigelb"),
                        Ingredient(50.0, GRAM, "Pecorino Romano, gerieben"),
                        Ingredient(1.0, PINCH, "Salz"),
                        Ingredient(null, NONE, "Schwarzer Pfeffer, frisch gemahlen"),
                    ),
                    steps = listOf(
                        CookingStep("Reichlich Wasser in einem großen Topf aufkochen und kräftig salzen.", null),
                        CookingStep("Spaghetti ins kochende Wasser geben und bissfest garen.", 9 * 60),
                        CookingStep("Guanciale in Streifen schneiden und in einer Pfanne ohne Öl knusprig auslassen.", 5 * 60),
                        CookingStep("Eigelb mit Pecorino und viel Pfeffer zu einer cremigen Masse verrühren.", null),
                        CookingStep(
                            "Nudeln abgießen (etwas Nudelwasser aufheben), zum Guanciale geben, Pfanne vom Herd nehmen. " +
                                "Eimasse unterrühren und mit Nudelwasser cremig ziehen lassen. Sofort servieren.",
                            null,
                        ),
                    ),
                ),
                notes = listOf("Nicht zu heiß werden lassen, sonst stockt das Ei."),
                categoryUuids = listOf(DefaultCategories.MAIN.uuid),
            ),
            SeedRecipe(
                recipe(
                    uuid = "seed-pfannkuchen",
                    title = "Pfannkuchen",
                    servings = 4,
                    minutes = 40,
                    ingredients = listOf(
                        Ingredient(250.0, GRAM, "Weizenmehl"),
                        Ingredient(500.0, MILLILITER, "Milch"),
                        Ingredient(3.0, NONE, "Eier"),
                        Ingredient(1.0, TABLESPOON, "Zucker"),
                        Ingredient(1.0, PINCH, "Salz"),
                        Ingredient(2.0, TABLESPOON, "Butter zum Ausbacken"),
                    ),
                    steps = listOf(
                        CookingStep("Mehl, Zucker und Salz mischen. Milch und Eier zugeben und zu einem glatten Teig verrühren.", null),
                        CookingStep("Teig quellen lassen.", 15 * 60),
                        CookingStep("Etwas Butter in einer Pfanne erhitzen, eine Kelle Teig hineingeben und dünn verteilen.", null),
                        CookingStep("Pfannkuchen goldbraun backen, wenden und die zweite Seite fertig backen.", 2 * 60),
                        CookingStep("Mit Apfelmus, Zimt und Zucker oder herzhaft füllen.", null),
                    ),
                ),
                notes = listOf("Mit Mineralwasser statt halber Milch werden sie luftiger."),
                categoryUuids = listOf(DefaultCategories.BREAKFAST.uuid, DefaultCategories.DESSERT.uuid),
            ),
            SeedRecipe(
                recipe(
                    uuid = "seed-linsensuppe",
                    title = "Linsensuppe",
                    servings = 4,
                    minutes = 50,
                    ingredients = listOf(
                        Ingredient(250.0, GRAM, "Tellerlinsen"),
                        Ingredient(1.0, PIECE, "Zwiebel"),
                        Ingredient(2.0, CLOVE, "Knoblauch"),
                        Ingredient(2.0, NONE, "Karotten"),
                        Ingredient(300.0, GRAM, "Kartoffeln"),
                        Ingredient(1.2, LITER, "Gemüsebrühe"),
                        Ingredient(2.0, TABLESPOON, "Olivenöl"),
                        Ingredient(1.0, TEASPOON, "Kreuzkümmel"),
                        Ingredient(2.0, TABLESPOON, "Essig"),
                    ),
                    steps = listOf(
                        CookingStep("Zwiebel, Knoblauch, Karotten und Kartoffeln schälen und klein würfeln.", null),
                        CookingStep("Öl erhitzen, Zwiebel und Knoblauch glasig dünsten, Kreuzkümmel kurz mitrösten.", 3 * 60),
                        CookingStep("Linsen, Gemüse und Brühe zugeben, aufkochen und zugedeckt köcheln lassen.", 30 * 60),
                        CookingStep("Mit Essig, Salz und Pfeffer abschmecken.", null),
                    ),
                ),
                notes = emptyList(),
                categoryUuids = listOf(DefaultCategories.MAIN.uuid),
            ),
        )
    }
}
