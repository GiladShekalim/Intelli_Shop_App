package com.example.intellishopapp

import com.example.intellishopapp.model.dto.GoogleLoginResponse
import com.example.intellishopapp.model.dto.LoginResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The login responses now carry the user's saved profile (statuses/hobbies) so the
 * home personalization can pre-filter on any device. These verify the wire format
 * parses both with and without those fields (older responses omit them).
 */
class LoginResponseParsingTest {

    private val gson = Gson()

    @Test
    fun login_parsesStatusesAndHobbies() {
        val json = """
            {"status":"success","redirect":"/home/","user_id":"u1",
             "statuses":["Student","Renter"],"hobbies":["Travel and Vacation"]}
        """.trimIndent()
        val body = gson.fromJson(json, LoginResponse::class.java)
        assertEquals("u1", body.user_id)
        assertEquals(listOf("Student", "Renter"), body.statuses)
        assertEquals(listOf("Travel and Vacation"), body.hobbies)
    }

    @Test
    fun login_withoutProfile_leavesThemNull() {
        val json = """{"status":"success","redirect":"/home/","user_id":"u1"}"""
        val body = gson.fromJson(json, LoginResponse::class.java)
        assertNull(body.statuses)
        assertNull(body.hobbies)
    }

    @Test
    fun login_emptyProfileArrays_parseEmpty() {
        val json = """
            {"status":"success","redirect":"/home/","user_id":"u1","statuses":[],"hobbies":[]}
        """.trimIndent()
        val body = gson.fromJson(json, LoginResponse::class.java)
        assertTrue(body.statuses!!.isEmpty())
        assertTrue(body.hobbies!!.isEmpty())
    }

    @Test
    fun googleLogin_existingUser_parsesProfile() {
        val json = """
            {"status":"success","is_new":false,"user_id":"g1","username":"G","email":"g@x.com",
             "statuses":["Young","Tech"],"hobbies":["electronics"]}
        """.trimIndent()
        val body = gson.fromJson(json, GoogleLoginResponse::class.java)
        assertFalse(body.is_new)
        assertEquals(listOf("Young", "Tech"), body.statuses)
        assertEquals(listOf("electronics"), body.hobbies)
    }

    @Test
    fun googleLogin_newUser_hasNoProfile() {
        val json = """{"status":"success","is_new":true,"email":"g@x.com","name":"G"}"""
        val body = gson.fromJson(json, GoogleLoginResponse::class.java)
        assertTrue(body.is_new)
        assertNull(body.statuses)
        assertNull(body.hobbies)
    }
}
