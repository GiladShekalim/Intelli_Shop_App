package com.example.intellishopapp

import com.example.intellishopapp.model.dto.RedeemedResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedeemedParsingTest {

    private val gson = Gson()

    @Test
    fun parsesRedeemedIds() {
        val body = gson.fromJson("""{"redeemed":["1","2","3"]}""", RedeemedResponse::class.java)
        assertEquals(listOf("1", "2", "3"), body.redeemed)
    }

    @Test
    fun emptyRedeemed() {
        assertTrue(gson.fromJson("""{"redeemed":[]}""", RedeemedResponse::class.java).redeemed!!.isEmpty())
    }

    @Test
    fun missingField_isNull() {
        assertNull(gson.fromJson("""{}""", RedeemedResponse::class.java).redeemed)
    }
}
