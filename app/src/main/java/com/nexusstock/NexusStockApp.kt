package com.nexusstock

import android.app.Application
import com.nexusstock.di.AppContainer

class NexusStockApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
