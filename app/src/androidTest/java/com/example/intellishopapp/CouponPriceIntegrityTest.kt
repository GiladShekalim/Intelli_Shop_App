package com.example.intellishopapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the price/percentage values the backend actually serves, so a bad enrichment
 * pass can never ship impossible numbers again:
 *  - a percentage coupon's value is a real percentage, strictly within (0, 100];
 *  - a coupon with no verified price (0) renders NO price text at all.
 * Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class CouponPriceIntegrityTest {

    private fun catalog() = runBlocking {
        (CouponRepository().getAllCoupons() as ApiResult.Success).data
    }

    @Test
    fun everyPercentageCoupon_isWithinZeroToHundred() {
        val bad = catalog().filter { c ->
            c.discount_type == "percentage" && (c.price == null || c.price!! <= 0.0 || c.price!! > 100.0)
        }
        assertTrue(
            "percentage coupons outside (0,100]: " + bad.map { it.discount_id to it.price },
            bad.isEmpty()
        )
    }

    @Test
    fun zeroPriceCoupons_renderNoLabel() {
        catalog().filter { (it.price ?: 0.0) == 0.0 }.forEach { c ->
            assertEquals(
                "id=${c.discount_id} with 0 price should show no text",
                "", CouponFormatter.priceLabel(c)
            )
        }
    }

    @Test
    fun fixedAmountCoupons_arePositiveWhenShown() {
        // A fixed-amount coupon either shows a positive price or nothing (never negative).
        catalog().filter { it.discount_type == "fixed_amount" }.forEach { c ->
            val p = c.price ?: 0.0
            assertTrue("id=${c.discount_id} negative price $p", p >= 0.0)
        }
    }
}
