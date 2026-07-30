package com.example.intellishopapp

import android.widget.FrameLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app gives visible, immediate feedback to actions: the celebratory fireworks
 * (shown on account creation) actually populate their overlay, and the in-app
 * notification banner appears with its message. These guard that responsiveness.
 */
@RunWith(AndroidJUnit4::class)
class UxResponsivenessTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
        SessionManager.getInstance().setNotificationsEnabled(true)
    }

    @Test
    fun fireworks_populateTheOverlay() {
        // Measure in the SAME UI call: the sparks are added synchronously, then animate
        // out (and with test animations disabled the removal fires almost immediately),
        // so the only reliable observation is right after playFireworks returns.
        var sparkCount = 0
        scenario.scenario.onActivity {
            it.playFireworks()
            sparkCount = it.findViewById<FrameLayout>(R.id.main_LAY_fx).childCount
        }
        assertTrue("fireworks should add spark views to the overlay", sparkCount > 0)
    }

    @Test
    fun banner_appearsWithMessageOnAction() {
        SessionManager.getInstance().setNotificationsEnabled(true)
        scenario.scenario.onActivity { it.showBanner("Nice work") }
        onView(withId(R.id.main_LBL_banner)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LBL_banner)).check(matches(withText("Nice work")))
    }
}
