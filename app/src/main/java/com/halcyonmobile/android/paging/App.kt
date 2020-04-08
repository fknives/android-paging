package com.halcyonmobile.android.paging

import android.app.Application
import com.halcyonmobile.android.core.createCoreModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidContext(this@App)
            modules(appModules())
        }
    }
}