package com.example.intellishopapp.utilities

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * A light vibration for tactile feedback (e.g. the sign-in prompt), matching the
 * course's SignalManager pattern. Visual messages go through the shell banner.
 */
class SignalManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    fun vibrate(millis: Long = 60L) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator?.vibrate(
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        )
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
