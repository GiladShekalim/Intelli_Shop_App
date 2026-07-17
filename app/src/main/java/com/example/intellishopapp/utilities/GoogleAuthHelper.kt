package com.example.intellishopapp.utilities

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Wraps Credential Manager to obtain a Google ID token for the configured
 * server (Web) client ID. Returns null if the user cancels or no credential
 * is available. The token is sent to the backend's /google_login/ endpoint.
 */
object GoogleAuthHelper {

    suspend fun getIdToken(context: Context): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(Constants.Api.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
