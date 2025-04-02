package com.example.dbit_almaconnect3

import android.app.Application

class AlmaConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.initialize(this)
    }
}

/**
 * App singleton for global access to application context
 */
object App {
    lateinit var instance: Application
        private set
    
    fun initialize(application: Application) {
        instance = application
    }
} 