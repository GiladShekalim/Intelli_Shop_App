package com.example.intellishopapp

import android.app.Application
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application subclass — declared via android:name=".App" in the manifest.
 * App-scoped singletons are initialized here in onCreate() via init(applicationContext).
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            AuthRepository().ensureCsrfPrimed()
        }
    }
}
