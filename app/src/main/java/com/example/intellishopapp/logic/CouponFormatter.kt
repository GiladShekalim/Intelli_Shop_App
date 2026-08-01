package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure display formatting for coupons — no Android imports, JVM-unit-testable.
 */
object CouponFormatter {

    private val DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Parses the backend's ISO date (yyyy-MM-dd). Returns null if it is blank or in
     * an unexpected shape, so callers can fall back gracefully.
     */
    fun parseDate(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    /** The valid-until date as dd/MM/yyyy; the raw value if it cannot be parsed. */
    fun validUntilDisplay(raw: String?): String {
        val date = parseDate(raw) ?: return raw?.trim().orEmpty()
        return date.format(DISPLAY)
    }

    /** True when the coupon's valid-until date is strictly before today. */
    fun isExpired(raw: String?, today: LocalDate = LocalDate.now()): Boolean {
        val date = parseDate(raw) ?: return false
        return date.isBefore(today)
    }

    /**
     * "29%" for percentage discounts, else the amount with its currency symbol —
     * shekels (₪) by default, or the coupon's own `currency` ("$", "€") for the few
     * foreign-priced offers. Empty when there is no verified price value (null or 0):
     * a zero is treated as "unknown", so the field shows no text rather than "₪0" / "0%".
     */
    fun priceLabel(coupon: CouponDto): String {
        val price = coupon.price ?: return ""
        if (price == 0.0) return ""
        val n = if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()
        if (coupon.discount_type == "percentage") return "$n%"
        return (coupon.currency ?: "₪") + n
    }

    /** First club/store name, or empty. */
    fun storeName(coupon: CouponDto): String = coupon.club_name?.firstOrNull().orEmpty()

    fun title(coupon: CouponDto): String = coupon.title.orEmpty()
}
