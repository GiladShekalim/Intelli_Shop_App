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
        statuses: List<String>? = null,
        validUntil: String? = null
    ) = CouponDto(
        discount_id = id, title = id, price = price, discount_type = "fixed_amount",
        description = null, image_link = null, discount_link = null,
        terms_and_conditions = null, club_name = null, category = category,
        valid_until = validUntil, usage_limit = null, coupon_code = null,
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

    // --- profile pre-filter (backend step 1): narrow to statuses / interests ---

    @Test
    fun profileFilter_bothSet_requiresStatusAndInterest() {
        val all = listOf(
            // Matches BOTH -> kept.
            coupon("both", price = 1.0, category = listOf("Travel and Vacation"), statuses = listOf("Student")),
            // Matches only the interest -> dropped under AND.
            coupon("cat-only", price = 999.0, category = listOf("Travel and Vacation")),
            // Matches only the status -> dropped under AND.
            coupon("status-only", price = 999.0, statuses = listOf("Student")),
            // Matches neither -> dropped.
            coupon("unrelated", price = 999.0, category = listOf("Cars"))
        )
        val ranked = CouponRanker.personalizedTop(
            all, emptySet(), 10,
            statuses = listOf("Student"),
            hobbies = listOf("Travel and Vacation")
        )
        // AND semantics, matching the backend query (status $in AND category $in).
        assertEquals(listOf("both"), ranked.map { it.discount_id })
    }

    @Test
    fun profileFilter_onlyStatusSet_filtersByStatusAlone() {
        val all = listOf(
            coupon("student", price = 1.0, statuses = listOf("Student")),
            coupon("senior", price = 999.0, statuses = listOf("Senior"))
        )
        val ranked = CouponRanker.personalizedTop(
            all, emptySet(), 10, statuses = listOf("Student")
        )
        assertEquals(listOf("student"), ranked.map { it.discount_id })
    }

    @Test
    fun profileFilter_keepsFavoritesEvenWhenOutsideTheProfile() {
        val all = listOf(
            coupon("in-profile", price = 1.0, category = listOf("Cars")),
            coupon("fav-outside", price = 1.0, category = listOf("Insurance"))
        )
        val ranked = CouponRanker.personalizedTop(
            all, setOf("fav-outside"), 10, hobbies = listOf("Cars")
        )
        // The favourite is merged back in and boosted above the profile match.
        assertEquals("fav-outside", ranked.first().discount_id)
        assertEquals(2, ranked.size)
    }

    @Test
    fun emptyProfile_ranksEverything() {
        val all = listOf(
            coupon("a", price = 5.0, category = listOf("Cars")),
            coupon("b", price = 9.0, category = listOf("Insurance"))
        )
        assertEquals(2, CouponRanker.personalizedTop(all, emptySet(), 10).size)
    }

    @Test
    fun profileFilter_noMatches_returnsEmpty() {
        val all = listOf(coupon("a", price = 5.0, category = listOf("Cars")))
        val ranked = CouponRanker.personalizedTop(
            all, emptySet(), 10, hobbies = listOf("Travel and Vacation")
        )
        assertTrue(ranked.isEmpty())
    }

    // --- last minute (closest expiration first) ---

    private val today = java.time.LocalDate.of(2026, 1, 1)

    @Test
    fun lastMinute_soonestExpirationFirst() {
        val all = listOf(
            coupon("far", validUntil = "2026-12-31"),
            coupon("soon", validUntil = "2026-01-10"),
            coupon("mid", validUntil = "2026-06-15")
        )
        assertEquals(
            listOf("soon", "mid", "far"),
            CouponRanker.lastMinute(all, 10, today).map { it.discount_id }
        )
    }

    @Test
    fun lastMinute_dropsExpiredAndUndated() {
        val all = listOf(
            coupon("expired", validUntil = "2025-12-31"),
            coupon("valid", validUntil = "2026-02-01"),
            coupon("undated", validUntil = null)
        )
        val ids = CouponRanker.lastMinute(all, 10, today).map { it.discount_id }
        assertEquals(listOf("valid"), ids)
    }

    @Test
    fun lastMinute_keepsTodayAndRespectsLimit() {
        val all = (1..20).map { coupon("c$it", validUntil = "2026-01-%02d".format(it)) }
        val result = CouponRanker.lastMinute(all, 3, today)
        assertEquals(3, result.size)
        // Jan 1 is today (not expired) and comes first.
        assertEquals("c1", result.first().discount_id)
    }
}
