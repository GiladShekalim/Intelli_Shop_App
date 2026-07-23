package com.example.intellishopapp

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher

/** Clicks a specific child view inside a RecyclerView item (e.g. a card's heart). */
fun clickChildViewWithId(id: Int): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()
    override fun getDescription(): String = "click child view with id $id"
    override fun perform(uiController: UiController, view: View) {
        view.findViewById<View>(id)?.performClick()
        uiController.loopMainThreadUntilIdle()
    }
}
