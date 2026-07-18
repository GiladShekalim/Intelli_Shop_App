package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto

/**
 * Pure display formatting for coupons — no Android imports, JVM-unit-testable.
 */
object CouponFormatter {

    /** "29%" for percentage discounts, "$500" otherwise. Empty if no price. */
    fun priceLabel(coupon: CouponDto): String {
        val price = coupon.price ?: return ""
        val n = if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()
        return if (coupon.discount_type == "percentage") "$n%" else "\$$n"
    }

    /** First club/store name, or empty. */
    fun storeName(coupon: CouponDto): String = coupon.club_name?.firstOrNull().orEmpty()

    fun title(coupon: CouponDto): String = coupon.title.orEmpty()
}
