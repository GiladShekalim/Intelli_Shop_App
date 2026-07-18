package com.example.intellishopapp.utilities

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast

/**
 * App-wide user feedback: a short Toast plus a light vibration, matching the
 * course's SignalManager pattern. App-scoped singleton.
 */
class SignalManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    fun toast(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    fun vibrate(millis: Long = 60L) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator?.vibrate(
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** Toast + vibration together — for notable actions. */
    fun signal(message: String) {
        toast(message)
        vibrate()
    }

    companion object {
        @Volatile
        private var instance: SignalManager? = null

        fun init(context: Context): SignalManager =
            instance ?: synchronized(this) {
                instance ?: SignalManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): SignalManager =
            instance ?: throw IllegalStateException(
                "SignalManager not initialized. Call init(context) in App.onCreate()."
            )
    }
}
