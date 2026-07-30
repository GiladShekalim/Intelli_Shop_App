package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.model.dto.SharedItemDto

/**
 * Turns the flat, newest-first list of received shares into per-sender sections for
 * the "Sent Offers by friends" page. Order is preserved: senders appear by their
 * most-recent share, and each sender's coupons stay newest-first. Shares whose
 * coupon is missing from the catalog (deleted) or whose sender is blank are skipped.
 * Pure logic — no Android imports, JVM-unit-testable.
 */
object SharedOffersGrouper {

    data class SenderSection(val sender: String, val coupons: List<CouponDto>)

    fun group(shares: List<SharedItemDto>, byId: Map<String?, CouponDto>): List<SenderSection> {
        val bySender = LinkedHashMap<String, MutableList<CouponDto>>()
        for (share in shares) {
            val sender = share.from_username?.takeIf { it.isNotBlank() } ?: continue
            val coupon = byId[share.discount_id] ?: continue
            bySender.getOrPut(sender) { mutableListOf() }.add(coupon)
        }
        return bySender.map { SenderSection(it.key, it.value) }
    }
}
