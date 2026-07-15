package com.example.intellishopapp

import android.app.Application

/**
 * Application subclass — declared via android:name=".App" in the manifest.
 * App-scoped singletons are initialized here in onCreate() via init(applicationContext).
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Singleton initialization goes here (theme, session, networking).
    }
}
