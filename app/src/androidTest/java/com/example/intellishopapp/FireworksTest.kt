package com.example.intellishopapp

import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The fireworks are driven by a manual frame loop so they play regardless of the
 * emulator's animator-duration scale. To keep the suite fast they are a no-op under
 * instrumented tests: playFireworks must add nothing to the overlay, and stopFireworks
 * must be safe to call. This guards that gate so a stray effect can never stall the run.
 */
@RunWith(AndroidJUnit4::class)
class FireworksTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun playFireworks_isNoOpUnderTest() {
        scenario.scenario.onActivity { activity ->
            activity.playFireworks()
            val fx = activity.findViewById<FrameLayout>(R.id.main_LAY_fx)
            // Under Espresso the effect is skipped, so nothing is added to the overlay.
            assertEquals(0, fx.childCount)
            // And cancelling is always safe.
            activity.stopFireworks()
            assertEquals(0, fx.childCount)
        }
    }
}
