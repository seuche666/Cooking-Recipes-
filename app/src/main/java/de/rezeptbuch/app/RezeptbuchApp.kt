package de.rezeptbuch.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.rezeptbuch.app.data.seed.SeedManager
import de.rezeptbuch.app.di.ApplicationScope
import de.rezeptbuch.app.service.timer.TimerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RezeptbuchApp : Application() {

    @Inject lateinit var seedManager: SeedManager

    @Inject @field:ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.createChannels(this)
        appScope.launch { seedManager.seedIfNeeded() }
    }
}
