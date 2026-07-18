package com.example.intellishopapp

import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.model.dto.CouponDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CouponFormatterTest {

    private fun coupon(
        price: Double?,
        type: String?,
        club: List<String>? = listOf("hot"),
        title: String? = "Title"
    ) = CouponDto(
        discount_id = "id", title = title, price = price, discount_type = type,
        description = null, image_link = null, discount_link = null,
        terms_and_conditions = null, club_name = club, category = null,
        valid_until = null, usage_limit = null, coupon_code = null,
        provider_link = null, consumer_statuses = null
    )

    @Test
    fun percentageDiscount_showsPercent() {
        assertEquals("29%", CouponFormatter.priceLabel(coupon(29.0, "percentage")))
    }

    @Test
    fun fixedAmountDiscount_showsDollar() {
        assertEquals("\$500", CouponFormatter.priceLabel(coupon(500.0, "fixed_amount")))
    }

    @Test
    fun nullPrice_isEmptyLabel() {
        assertEquals("", CouponFormatter.priceLabel(coupon(null, "percentage")))
    }

    @Test
    fun storeName_takesFirstClub() {
        assertEquals("hot", CouponFormatter.storeName(coupon(10.0, "percentage", listOf("hot", "adif"))))
    }

    @Test
    fun storeName_emptyWhenNoClub() {
        assertEquals("", CouponFormatter.storeName(coupon(10.0, "percentage", null)))
    }
}
