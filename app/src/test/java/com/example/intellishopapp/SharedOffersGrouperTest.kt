package com.example.intellishopapp

import com.example.intellishopapp.logic.SharedOffersGrouper
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.model.dto.SharedItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedOffersGrouperTest {

    private fun coupon(id: String) = CouponDto(
        discount_id = id, title = id, price = 0.0, discount_type = "fixed_amount",
        description = null, image_link = null, discount_link = null,
        terms_and_conditions = null, club_name = null, category = null,
        valid_until = null, usage_limit = null, coupon_code = null,
        provider_link = null, consumer_statuses = null
    )

    private fun share(sender: String, id: String) =
        SharedItemDto(from_user_id = "u_$sender", from_username = sender, discount_id = id)

    private val catalog = listOf("a", "b", "c").associateWith { coupon(it) } as Map<String?, CouponDto>

    @Test
    fun groupsBySender_preservingOrder() {
        // Newest-first input: alice(a), bob(b), alice(c).
        val sections = SharedOffersGrouper.group(
            listOf(share("alice", "a"), share("bob", "b"), share("alice", "c")), catalog
        )
        // Senders ordered by first (most-recent) appearance.
        assertEquals(listOf("alice", "bob"), sections.map { it.sender })
        // Alice keeps newest-first within her section.
        assertEquals(listOf("a", "c"), sections[0].coupons.map { it.discount_id })
        assertEquals(listOf("b"), sections[1].coupons.map { it.discount_id })
    }

    @Test
    fun skipsCouponsMissingFromCatalog() {
        val sections = SharedOffersGrouper.group(
            listOf(share("alice", "a"), share("alice", "gone")), catalog
        )
        assertEquals(1, sections.size)
        assertEquals(listOf("a"), sections[0].coupons.map { it.discount_id })
    }

    @Test
    fun skipsBlankSender() {
        val sections = SharedOffersGrouper.group(
            listOf(SharedItemDto(from_username = "", discount_id = "a")), catalog
        )
        assertTrue(sections.isEmpty())
    }

    @Test
    fun emptyInput_isEmpty() {
        assertTrue(SharedOffersGrouper.group(emptyList(), catalog).isEmpty())
    }

    @Test
    fun senderWithOnlyMissingCoupons_isDropped() {
        val sections = SharedOffersGrouper.group(
            listOf(share("alice", "a"), share("bob", "gone")), catalog
        )
        assertEquals(listOf("alice"), sections.map { it.sender })
    }
}
