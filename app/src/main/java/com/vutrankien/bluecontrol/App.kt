package com.vutrankien.bluecontrol

import android.app.Application
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            modules(appDependencies)
        }
    }

}