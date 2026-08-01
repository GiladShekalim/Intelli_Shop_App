package com.example.intellishopapp

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher

/**
 * A coupon code that currently exists in the backend catalog. The sync tests search
 * by a code to open a coupon deterministically; the catalog is refreshed from scraping,
 * so a live one is picked at run time instead of hard-coding a value that can disappear.
 */
fun aCouponCodeFromCatalog(): String = kotlinx.coroutines.runBlocking {
    val result = com.example.intellishopapp.repository.CouponRepository().getAllCoupons()
    (result as com.example.intellishopapp.utilities.ApiResult.Success).data
        .firstNotNullOf { c -> c.coupon_code?.takeIf { it.isNotBlank() && it != "N/A" } }
}

/** Clicks a specific child view inside a RecyclerView item (e.g. a card's heart). */
fun clickChildViewWithId(id: Int): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()
    override fun getDescription(): String = "click child view with id $id"
    override fun perform(uiController: UiController, view: View) {
        view.findViewById<View>(id)?.performClick()
        uiController.loopMainThreadUntilIdle()
    }
}
