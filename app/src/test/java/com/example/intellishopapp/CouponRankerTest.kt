package com.example.intellishopapp

import com.example.intellishopapp.logic.CouponRanker
import com.example.intellishopapp.model.dto.CouponDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponRankerTest {

    private fun coupon(
        id: String,
        price: Double? = 0.0,
        category: List<String>? = null,
        statuses: List<String>? = null
    ) = CouponDto(
        discount_id = id, title = id, price = price, discount_type = "fixed_amount",
        description = null, image_link = null, discount_link = null,
        terms_and_conditions = null, club_name = null, category = category,
        valid_until = null, usage_limit = null, coupon_code = null,
        provider_link = null, consumer_statuses = statuses
    )

    @Test
    fun favoriteCoupon_isBoostedToTop() {
        val all = listOf(
            coupon("a", price = 999.0),
            coupon("b", price = 1.0),
            coupon("c", price = 500.0)
        )
        val ranked = CouponRanker.personalizedTop(all, setOf("b"), 10)
        assertEquals("b", ranked.first().discount_id)
    }

    @Test
    fun categoryOfFavorite_liftsSimilarCoupons() {
        val all = listOf(
            coupon("fav", category = listOf("electronics")),
            coupon("similar", category = listOf("electronics")),
            coupon("other", category = listOf("books"))
        )
        // "similar" shares the favourite's category -> ranks above "other".
        val ranked = CouponRanker.personalizedTop(all, setOf("fav"), 10)
        assertEquals("fav", ranked[0].discount_id) // favourite boost
        assertEquals("similar", ranked[1].discount_id)
        assertEquals("other", ranked[2].discount_id)
    }

    @Test
    fun statusOfFavorite_addsWeight() {
        val all = listOf(
            coupon("fav", statuses = listOf("Student")),
            coupon("student", statuses = listOf("Student")),
            coupon("none", statuses = listOf("Retiree"))
        )
        val ranked = CouponRanker.personalizedTop(all, setOf("fav"), 10)
        assertEquals("fav", ranked[0].discount_id)
        assertEquals("student", ranked[1].discount_id)
    }

    @Test
    fun noFavorites_fallsBackToHighestPrice() {
        val all = listOf(
            coupon("cheap", price = 10.0),
            coupon("pricey", price = 900.0),
            coupon("mid", price = 300.0)
        )
        val ranked = CouponRanker.personalizedTop(all, emptySet(), 10)
        assertEquals(listOf("pricey", "mid", "cheap"), ranked.map { it.discount_id })
    }

    @Test
    fun respectsLimit() {
        val all = (1..20).map { coupon("c$it", price = it.toDouble()) }
        assertEquals(5, CouponRanker.personalizedTop(all, emptySet(), 5).size)
    }

    @Test
    fun emptyInput_returnsEmpty() {
        assertTrue(CouponRanker.personalizedTop(emptyList(), setOf("x"), 10).isEmpty())
    }
}
