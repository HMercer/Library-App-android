package com.sensorpic.demo

import android.app.Application
import com.sensorpic.demo.ui.settings.Settings
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Application singleton
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setUpLogging()
        Settings.setContext(this)
    }

    private fun setUpLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

}
