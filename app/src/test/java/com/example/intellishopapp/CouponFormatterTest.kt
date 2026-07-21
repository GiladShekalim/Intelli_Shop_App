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

    // --- edge cases ---

    @Test
    fun percentageWithDecimal_keepsDecimal() {
        assertEquals("29.5%", CouponFormatter.priceLabel(coupon(29.5, "percentage")))
    }

    @Test
    fun fixedWithDecimal_keepsDecimal() {
        assertEquals("\$99.99", CouponFormatter.priceLabel(coupon(99.99, "fixed_amount")))
    }

    @Test
    fun zeroPercentage_showsZeroPercent() {
        assertEquals("0%", CouponFormatter.priceLabel(coupon(0.0, "percentage")))
    }

    @Test
    fun zeroFixed_showsZeroDollar() {
        assertEquals("\$0", CouponFormatter.priceLabel(coupon(0.0, "fixed_amount")))
    }

    @Test
    fun nullType_fallsBackToDollar() {
        assertEquals("\$50", CouponFormatter.priceLabel(coupon(50.0, null)))
    }

    @Test
    fun unknownType_fallsBackToDollar() {
        assertEquals("\$50", CouponFormatter.priceLabel(coupon(50.0, "mystery")))
    }

    @Test
    fun negativePercentage_keepsSign() {
        assertEquals("-10%", CouponFormatter.priceLabel(coupon(-10.0, "percentage")))
    }

    @Test
    fun largeAmount_hasNoThousandsSeparator() {
        assertEquals("\$1000000", CouponFormatter.priceLabel(coupon(1_000_000.0, "fixed_amount")))
    }

    @Test
    fun storeName_emptyListIsEmpty() {
        assertEquals("", CouponFormatter.storeName(coupon(10.0, "percentage", emptyList())))
    }

    @Test
    fun title_nullIsEmpty() {
        assertEquals("", CouponFormatter.title(coupon(10.0, "percentage", title = null)))
    }

    @Test
    fun title_passesThrough() {
        assertEquals("Big Sale", CouponFormatter.title(coupon(10.0, "percentage", title = "Big Sale")))
    }
}
