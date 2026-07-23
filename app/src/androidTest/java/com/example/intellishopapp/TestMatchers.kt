package com.example.intellishopapp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/** Matches a TextView whose text is present and not blank. */
fun withNonEmptyText(): Matcher<View> =
    object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with non-empty text")
        }

        override fun matchesSafely(view: TextView): Boolean =
            !view.text.isNullOrBlank()
    }

/** Matches an ImageView that has a drawable set (a loaded image or the placeholder). */
fun hasDrawable(): Matcher<View> =
    object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has a drawable")
        }

        override fun matchesSafely(view: ImageView): Boolean = view.drawable != null
    }
