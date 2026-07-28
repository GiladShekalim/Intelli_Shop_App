package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto

/**
 * Personalized ranking, replicating the backend's `index_home` weighting: the pool is
 * narrowed to the user's statuses/interests, then a coupon scores by how often its
 * categories and consumer-statuses appear across the user's favourites, plus a large
 * boost if it is itself a favourite. Ties break by price so a user with no favourites
 * still gets the highest-value coupons first (matches the old guest hero). Pure logic —
 * no Android imports, JVM-unit-testable.
 */
object CouponRanker {

    private const val FAVORITE_BOOST = 1000

    fun personalizedTop(
        all: List<CouponDto>,
        favoriteIds: Set<String>,
        limit: Int = 10,
        statuses: List<String> = emptyList(),
        hobbies: List<String> = emptyList()
    ): List<CouponDto> {
        val favoriteCoupons = all.filter { it.discount_id != null && favoriteIds.contains(it.discount_id) }

        // Backend step 1+4: narrow to the user's profile, then merge favourites back
        // in so a favourite outside the profile is never dropped.
        val pool = if (statuses.isEmpty() && hobbies.isEmpty()) {
            all
        } else {
            val matching = all.filter { coupon ->
                statuses.any { coupon.consumer_statuses?.contains(it) == true } ||
                    hobbies.any { coupon.category?.contains(it) == true }
            }
            (matching + favoriteCoupons).distinctBy { it.discount_id ?: it }
        }

        val categoryCount = HashMap<String, Int>()
        val statusCount = HashMap<String, Int>()
        for (fav in favoriteCoupons) {
            fav.category?.forEach { categoryCount[it] = (categoryCount[it] ?: 0) + 1 }
            fav.consumer_statuses?.forEach { statusCount[it] = (statusCount[it] ?: 0) + 1 }
        }

        return pool.sortedWith(
            compareByDescending<CouponDto> { weight(it, categoryCount, statusCount, favoriteIds) }
                .thenByDescending { it.price ?: 0.0 }
        ).take(limit)
    }

    /**
     * Last Minute Offers: coupons whose valid-until date is nearest, soonest first.
     * Already-expired coupons and coupons with no parseable date are dropped, so the
     * row only ever shows offers you can still use.
     */
    fun lastMinute(
        all: List<CouponDto>,
        limit: Int = 10,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ): List<CouponDto> =
        all.mapNotNull { coupon ->
            CouponFormatter.parseDate(coupon.valid_until)
                ?.takeIf { !it.isBefore(today) }
                ?.let { coupon to it }
        }
            .sortedBy { it.second }
            .map { it.first }
            .take(limit)

    private fun weight(
        coupon: CouponDto,
        categoryCount: Map<String, Int>,
        statusCount: Map<String, Int>,
        favoriteIds: Set<String>
    ): Int {
        var weight = 0
        coupon.category?.forEach { weight += categoryCount[it] ?: 0 }
        coupon.consumer_statuses?.forEach { weight += statusCount[it] ?: 0 }
        if (coupon.discount_id != null && favoriteIds.contains(coupon.discount_id)) {
            weight += FAVORITE_BOOST
        }
        return weight
    }
}
