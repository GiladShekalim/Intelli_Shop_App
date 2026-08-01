package com.example.intellishopapp

import com.example.intellishopapp.logic.CatalogFacets
import com.example.intellishopapp.model.dto.CouponDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The label pickers hide any label the catalog has no coupons for, but never hide a
 * label the user has already selected.
 */
class CatalogFacetsTest {

    private fun coupon(cats: List<String>, statuses: List<String> = emptyList(), clubs: List<String> = emptyList()) =
        CouponDto(
            discount_id = "x", title = "x", price = null, discount_type = null, description = null,
            image_link = null, discount_link = null, terms_and_conditions = null, club_name = clubs,
            category = cats, valid_until = null, usage_limit = null, coupon_code = null,
            provider_link = null, consumer_statuses = statuses
        )

    @Test
    fun hidesLabelsWithNoCoupons() {
        CatalogFacets.update(listOf(coupon(cats = listOf("electronics", "books"))))
        val visible = CatalogFacets.keepPresent(
            listOf("electronics", "Cars", "books", "Insurance"), CatalogFacets.Facet.CATEGORY
        )
        assertEquals(listOf("electronics", "books"), visible)
    }

    @Test
    fun keepsAnAlreadySelectedLabelEvenIfAbsent() {
        CatalogFacets.update(listOf(coupon(cats = listOf("electronics"))))
        val visible = CatalogFacets.keepPresent(
            listOf("electronics", "Cars"), CatalogFacets.Facet.CATEGORY, selected = setOf("Cars")
        )
        assertEquals(listOf("electronics", "Cars"), visible)
    }

    @Test
    fun statusesAndClubs_filterByTheirOwnFacet() {
        CatalogFacets.update(
            listOf(coupon(cats = listOf("electronics"), statuses = listOf("Student"), clubs = listOf("hot")))
        )
        assertEquals(listOf("Student"), CatalogFacets.keepPresent(listOf("Student", "Pets"), CatalogFacets.Facet.STATUS))
        assertEquals(
            listOf("hot" to "HOT"),
            CatalogFacets.keepPresentPairs(listOf("hot" to "HOT", "adif" to "Adif"), CatalogFacets.Facet.MEMBERSHIP)
        )
    }
}
