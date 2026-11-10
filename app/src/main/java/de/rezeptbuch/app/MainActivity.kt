package de.rezeptbuch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.rezeptbuch.app.domain.model.ThemeMode
import de.rezeptbuch.app.domain.repository.SettingsRepository
import de.rezeptbuch.app.ui.navigation.AppNavHost
import de.rezeptbuch.app.ui.theme.RezeptbuchTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            RezeptbuchTheme(darkTheme = dark) {
                AppNavHost()
            }
        }
    }
}
