package com.example.intellishopapp.logic

import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.utilities.SessionManager

/**
 * The hard membership filter for discovery surfaces (Home feed, Search, browse). If the
 * user has selected memberships, only coupons whose club_name matches one of them pass;
 * an empty selection means "no filter" and everything passes.
 *
 * Deliberately NOT applied to the user's own lists (Favorites, Redeemed history, Recently
 * Viewed) or to friend-shared coupons — those keep whatever the user already has.
 */
object MembershipFilter {

    /** Filter by the signed-in user's selected memberships (empty = pass-through). */
    fun apply(coupons: List<CouponDto>): List<CouponDto> =
        apply(coupons, SessionManager.getInstance().memberships())

    fun apply(coupons: List<CouponDto>, memberships: List<String>): List<CouponDto> {
        if (memberships.isEmpty()) return coupons
        val selected = memberships.toSet()
        return coupons.filter { coupon ->
            coupon.club_name?.any { selected.contains(it) } == true
        }
    }
}
