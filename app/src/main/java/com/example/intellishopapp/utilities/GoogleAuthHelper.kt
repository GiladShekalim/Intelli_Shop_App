package com.example.intellishopapp.utilities

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Wraps Credential Manager to obtain a Google ID token for the configured server
 * (Web) client ID. First tries the one-tap path over existing accounts; if none
 * is offered it falls back to the explicit "Sign in with Google" chooser so the
 * user can pick or add an account. Returns null if the user cancels.
 *
 * Any other failure propagates so the caller can surface the real reason (bad
 * client id, SHA-1/package mismatch, etc.). Needs working network — GMS contacts
 * Google to mint the token, so DNS/connectivity must be up.
 */
object GoogleAuthHelper {

    /** What Google hands back: the token the backend verifies, plus the account photo. */
    data class GoogleAccount(val idToken: String, val photoUrl: String?)

    suspend fun getAccount(context: Context): GoogleAccount? {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(Constants.Api.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            requestAccount(context, credentialManager, googleIdOption)
        } catch (e: NoCredentialException) {
            // No account offered by the one-tap path -> show the explicit chooser.
            val signInOption = GetSignInWithGoogleOption
                .Builder(Constants.Api.GOOGLE_WEB_CLIENT_ID)
                .build()
            requestAccount(context, credentialManager, signInOption)
        }
    }

    private suspend fun requestAccount(
        context: Context,
        credentialManager: CredentialManager,
        option: CredentialOption
    ): GoogleAccount? {
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return extractAccount(credentialManager.getCredential(context, request).credential)
    }

    private fun extractAccount(credential: Credential): GoogleAccount? =
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAccount(googleCredential.idToken, googleCredential.profilePictureUri?.toString())
        } else {
            null
        }
}
