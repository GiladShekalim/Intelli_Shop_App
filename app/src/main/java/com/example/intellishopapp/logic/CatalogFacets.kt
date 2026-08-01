package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto

/**
 * Which category / status / membership labels are actually present in the current
 * catalog. The label pickers (registration, the Preferences/Categories/Memberships
 * editor, the Search filter panel) hide any label that no coupon uses, and show it
 * again automatically once a coupon with that label arrives. Filtering behaviour is
 * unchanged — only which options are *offered* is trimmed.
 *
 * Refreshed whenever the full catalog is fetched (see CouponRepository.getAllCoupons).
 * Until the catalog has loaded the sets are null, and every label is shown (never hide
 * everything). An already-selected label is always kept, so a selection is never trapped.
 */
object CatalogFacets {

    enum class Facet { CATEGORY, STATUS, MEMBERSHIP }

    @Volatile private var categories: Set<String>? = null
    @Volatile private var statuses: Set<String>? = null
    @Volatile private var clubs: Set<String>? = null

    fun update(coupons: List<CouponDto>) {
        if (coupons.isEmpty()) return
        categories = coupons.flatMap { it.category ?: emptyList() }.toSet()
        statuses = coupons.flatMap { it.consumer_statuses ?: emptyList() }.toSet()
        clubs = coupons.flatMap { it.club_name ?: emptyList() }.toSet()
    }

    private fun setFor(facet: Facet): Set<String>? = when (facet) {
        Facet.CATEGORY -> categories
        Facet.STATUS -> statuses
        Facet.MEMBERSHIP -> clubs
    }

    /** Plain label list, trimmed to present-or-selected (all if the catalog is unknown). */
    fun keepPresent(all: List<String>, facet: Facet, selected: Set<String> = emptySet()): List<String> {
        val present = setFor(facet) ?: return all
        return all.filter { present.contains(it) || selected.contains(it) }
    }

    /** (key, label) options, trimmed by their key to present-or-selected. */
    fun keepPresentPairs(
        all: List<Pair<String, String>>, facet: Facet, selected: Set<String> = emptySet()
    ): List<Pair<String, String>> {
        val present = setFor(facet) ?: return all
        return all.filter { present.contains(it.first) || selected.contains(it.first) }
    }
}
