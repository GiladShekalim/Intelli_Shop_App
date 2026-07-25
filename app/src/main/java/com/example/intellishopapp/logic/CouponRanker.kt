package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto

/**
 * Personalized ranking, replicating the backend's `index_home` weighting: a coupon
 * scores by how often its categories and consumer-statuses appear across the user's
 * favourites, plus a large boost if it is itself a favourite. Ties break by price so
 * a user with no favourites still gets the highest-value coupons first (matches the
 * old guest hero). Pure logic — no Android imports, JVM-unit-testable.
 */
object CouponRanker {

    private const val FAVORITE_BOOST = 1000

    fun personalizedTop(
        all: List<CouponDto>,
        favoriteIds: Set<String>,
        limit: Int = 10
    ): List<CouponDto> {
        val favoriteCoupons = all.filter { it.discount_id != null && favoriteIds.contains(it.discount_id) }

        val categoryCount = HashMap<String, Int>()
        val statusCount = HashMap<String, Int>()
        for (fav in favoriteCoupons) {
            fav.category?.forEach { categoryCount[it] = (categoryCount[it] ?: 0) + 1 }
            fav.consumer_statuses?.forEach { statusCount[it] = (statusCount[it] ?: 0) + 1 }
        }

        return all.sortedWith(
            compareByDescending<CouponDto> { weight(it, categoryCount, statusCount, favoriteIds) }
                .thenByDescending { it.price ?: 0.0 }
        ).take(limit)
    }

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
