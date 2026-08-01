package com.example.intellishopapp.utilities

/**
 * Whether the app is running under an instrumented (Espresso) test. Used to skip
 * perpetual/long UI effects (auto-scroll carousels, fireworks) that would otherwise
 * keep the test thread busy. One check, shared by every caller.
 */
object TestEnv {
    val isInstrumented: Boolean by lazy {
        try {
            Class.forName("androidx.test.espresso.Espresso"); true
        } catch (e: Throwable) {
            false
        }
    }
}
