package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app gives visible, immediate feedback to actions: the in-app notification
 * banner appears with its message. (The fireworks effect is covered on its own by
 * [FireworksTest], which guards that it stays a no-op under instrumented tests.)
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
    fun banner_appearsWithMessageOnAction() {
        SessionManager.getInstance().setNotificationsEnabled(true)
        scenario.scenario.onActivity { it.showBanner("Nice work") }
        onView(withId(R.id.main_LBL_banner)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LBL_banner)).check(matches(withText("Nice work")))
    }
}
