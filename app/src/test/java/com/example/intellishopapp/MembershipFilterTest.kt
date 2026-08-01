package com.example.intellishopapp

import com.example.intellishopapp.logic.MembershipFilter
import com.example.intellishopapp.model.dto.CouponDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The membership filter is the whole point of the feature: with memberships selected,
 * only matching clubs pass; with none selected, everything passes (no filter).
 */
class MembershipFilterTest {

    private fun coupon(id: String, club: String?) = CouponDto(
        discount_id = id, title = id, price = null, discount_type = null, description = null,
        image_link = null, discount_link = null, terms_and_conditions = null,
        club_name = club?.let { listOf(it) }, category = null, valid_until = null,
        usage_limit = null, coupon_code = null, provider_link = null, consumer_statuses = null
    )

    private val catalog = listOf(
        coupon("1", "hot"), coupon("2", "adif"), coupon("3", "hot"),
        coupon("4", "other"), coupon("5", null)
    )

    @Test
    fun noSelection_passesEverythingThrough() {
        assertEquals(catalog, MembershipFilter.apply(catalog, emptyList()))
    }

    @Test
    fun oneMembership_keepsOnlyThatClub() {
        val kept = MembershipFilter.apply(catalog, listOf("hot"))
        assertEquals(listOf("1", "3"), kept.map { it.discount_id })
    }

    @Test
    fun multipleMemberships_keepAnyMatch() {
        val kept = MembershipFilter.apply(catalog, listOf("hot", "adif"))
        assertEquals(listOf("1", "2", "3"), kept.map { it.discount_id })
    }

    @Test
    fun couponsWithNoClub_areExcludedWhenFiltering() {
        val kept = MembershipFilter.apply(catalog, listOf("adif"))
        assertEquals(listOf("2"), kept.map { it.discount_id })
    }
}
