package de.rezeptbuch.app.data.remote

import de.rezeptbuch.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentrale Stelle für API-Schlüssel. Der Wert stammt aus `local.properties`
 * (Eintrag `RECIPE_API_KEY`) und wird vom Secrets Gradle Plugin zur Build-Zeit
 * in [BuildConfig] geschrieben – er steht nie im Quellcode oder im Repository.
 *
 * Aktuell arbeitet die App rein lokal; der Key ist für künftige Online-Funktionen
 * (z. B. Rezept-Import aus dem Web) vorgesehen.
 */
@Singleton
class ApiKeyProvider @Inject constructor() {

    val recipeApiKey: String get() = BuildConfig.RECIPE_API_KEY

    /** `false`, solange nur der Platzhalter aus `secrets.defaults.properties` gesetzt ist. */
    val isRecipeApiConfigured: Boolean get() = recipeApiKey.isNotBlank() && recipeApiKey != PLACEHOLDER

    private companion object {
        const val PLACEHOLDER = "DEFAULT_API_KEY"
    }
}
