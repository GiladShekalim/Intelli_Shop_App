package com.example.intellishopapp

import com.example.intellishopapp.model.dto.UsernameCheckResponse
import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The registration screen's live availability check parses {available: bool}. */
class UsernameCheckParsingTest {

    private val gson = Gson()

    @Test
    fun parsesAvailableTrue() {
        assertTrue(gson.fromJson("""{"available":true}""", UsernameCheckResponse::class.java).available)
    }

    @Test
    fun parsesAvailableFalse() {
        assertFalse(gson.fromJson("""{"available":false}""", UsernameCheckResponse::class.java).available)
    }

    @Test
    fun missingField_defaultsToUnavailable() {
        // Defensive: a malformed response must not read as "available".
        assertFalse(gson.fromJson("""{}""", UsernameCheckResponse::class.java).available)
    }
}
