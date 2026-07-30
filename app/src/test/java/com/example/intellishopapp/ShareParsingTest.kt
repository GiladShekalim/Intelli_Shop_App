package com.example.intellishopapp

import com.example.intellishopapp.model.dto.ReceivedSharesResponse
import com.example.intellishopapp.model.dto.ShareResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareParsingTest {

    private val gson = Gson()

    @Test
    fun receivedShares_parsesSenderAndCoupon() {
        val json = """
            {"received_shares":[
              {"from_user_id":"u1","from_username":"alice","discount_id":"1"},
              {"from_user_id":"u2","from_username":"bob","discount_id":"2"}
            ]}
        """.trimIndent()
        val body = gson.fromJson(json, ReceivedSharesResponse::class.java)
        assertEquals(2, body.received_shares!!.size)
        assertEquals("alice", body.received_shares!![0].from_username)
        assertEquals("2", body.received_shares!![1].discount_id)
    }

    @Test
    fun receivedShares_emptyArray() {
        val body = gson.fromJson("""{"received_shares":[]}""", ReceivedSharesResponse::class.java)
        assertTrue(body.received_shares!!.isEmpty())
    }

    @Test
    fun receivedShares_missingField_isNull() {
        val body = gson.fromJson("""{}""", ReceivedSharesResponse::class.java)
        assertNull(body.received_shares)
    }

    @Test
    fun shareResponse_carriesSelfStatus() {
        val body = gson.fromJson("""{"error":"...","status":"self"}""", ShareResponse::class.java)
        assertEquals("self", body.status)
    }

    @Test
    fun shareResponse_success() {
        assertEquals("success", gson.fromJson("""{"status":"success"}""", ShareResponse::class.java).status)
    }
}
