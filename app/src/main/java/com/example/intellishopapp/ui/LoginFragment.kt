package com.example.intellishopapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.GoogleAuthHelper
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Login shown inside the shell. On success it pops back to the previous screen
 * (now signed in). New Google users are sent to the profile/categories setup.
 */
class LoginFragment : Fragment() {

    private lateinit var login_ET_email: EditText
    private lateinit var login_ET_password: EditText
    private lateinit var login_BTN_submit: MaterialButton
    private lateinit var login_BTN_google: MaterialButton
    private lateinit var login_LBL_error: MaterialTextView
    private lateinit var login_LBL_registerLink: MaterialTextView
    private lateinit var login_LAY_progress: View

    private val authRepository = AuthRepository()
    private val favoriteRepository = com.example.intellishopapp.repository.FavoriteRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        initViews()
    }

    private fun findViews(view: View) {
        login_ET_email = view.findViewById(R.id.login_ET_email)
        login_ET_password = view.findViewById(R.id.login_ET_password)
        login_BTN_submit = view.findViewById(R.id.login_BTN_submit)
        login_BTN_google = view.findViewById(R.id.login_BTN_google)
        login_LBL_error = view.findViewById(R.id.login_LBL_error)
        login_LBL_registerLink = view.findViewById(R.id.login_LBL_registerLink)
        login_LAY_progress = view.findViewById(R.id.login_LAY_progress)
    }

    private fun initViews() {
        login_BTN_submit.setOnClickListener { submit() }
        login_BTN_google.setOnClickListener { signInWithGoogle() }
        login_LBL_registerLink.setOnClickListener {
            (requireActivity() as MainActivity).showRegister(null)
        }
    }

    private fun submit() {
        val email = login_ET_email.text?.toString()?.trim().orEmpty()
        val password = login_ET_password.text?.toString().orEmpty()
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fill_login))
            return
        }
        login_LBL_error.visibility = View.GONE
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    if (result.data.status == "success") {
                        // Prefer the profile the backend returns (works on any device);
                        // fall back to what was saved locally at registration.
                        val saved = SessionManager.getInstance().loadPreferences(email)
                        val statuses = result.data.statuses?.takeIf { it.isNotEmpty() }
                            ?: saved?.statuses ?: emptyList()
                        val hobbies = result.data.hobbies?.takeIf { it.isNotEmpty() }
                            ?: saved?.hobbies ?: emptyList()
                        val memberships = result.data.memberships?.takeIf { it.isNotEmpty() }
                            ?: saved?.memberships ?: emptyList()
                        // Keep the local store in step so it stays a valid fallback.
                        SessionManager.getInstance().savePreferences(email, statuses, hobbies, memberships)
                        SessionManager.getInstance().save(
                            UserSession(
                                userId = result.data.user_id,
                                email = email,
                                status = statuses,
                                hobbies = hobbies,
                                memberships = memberships
                            )
                        )
                        // Sync favorites from the backend so they show on any device.
                        (favoriteRepository.getFavorites() as? ApiResult.Success)?.let {
                            SessionManager.getInstance().setFavorites(it.data)
                        }
                        onSignedIn()
                    } else {
                        setLoading(false)
                        showError(getString(R.string.error_login_failed))
                    }
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    showError(getString(R.string.error_login_failed))
                }
            }
        }
    }

    private fun signInWithGoogle() {
        login_LBL_error.visibility = View.GONE
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val account = try {
                GoogleAuthHelper.getAccount(requireContext())
            } catch (e: Exception) {
                Log.e("GoogleAuth", "getAccount failed", e)
                setLoading(false)
                showError("Google: ${e.message}")
                return@launch
            }
            if (account == null) {
                setLoading(false)
                showError(getString(R.string.error_google_failed))
                return@launch
            }
            when (val result = authRepository.googleLogin(account.idToken)) {
                is ApiResult.Success -> {
                    val body = result.data
                    // Keyed by email so the avatar is there whether the user signs in
                    // now or finishes registration first.
                    SessionManager.getInstance()
                        .savePhotoUrl(body.email.orEmpty(), account.photoUrl)
                    if (!body.is_new) {
                        // Prefer the profile the backend returns (works on any device);
                        // fall back to what was saved locally at registration.
                        val emailValue = body.email.orEmpty()
                        val saved = SessionManager.getInstance().loadPreferences(emailValue)
                        val statuses = body.statuses?.takeIf { it.isNotEmpty() }
                            ?: saved?.statuses ?: emptyList()
                        val hobbies = body.hobbies?.takeIf { it.isNotEmpty() }
                            ?: saved?.hobbies ?: emptyList()
                        val memberships = body.memberships?.takeIf { it.isNotEmpty() }
                            ?: saved?.memberships ?: emptyList()
                        SessionManager.getInstance().savePreferences(emailValue, statuses, hobbies, memberships)
                        SessionManager.getInstance().save(
                            UserSession(
                                userId = body.user_id ?: "",
                                email = emailValue,
                                username = body.username,
                                status = statuses,
                                hobbies = hobbies,
                                memberships = memberships,
                                isGoogle = true
                            )
                        )
                        onSignedIn()
                    } else {
                        setLoading(false)
                        (requireActivity() as MainActivity).showRegister(
                            bundleOf(
                                RegisterFragment.EXTRA_EMAIL to body.email,
                                RegisterFragment.EXTRA_NAME to body.name,
                                RegisterFragment.EXTRA_GOOGLE to true
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    showError(getString(R.string.error_google_failed))
                }
            }
        }
    }

    /** Called after a fresh sign-up so the user only has to type their password. */
    fun prefillEmail(email: String) {
        if (view == null) return
        login_ET_email.setText(email)
        login_ET_password.text?.clear()
        login_ET_password.requestFocus()
    }

    private fun onSignedIn() {
        (requireActivity() as MainActivity).showBanner(getString(R.string.welcome_back))
        parentFragmentManager.popBackStack()
    }

    private fun setLoading(loading: Boolean) {
        login_LAY_progress.visibility = if (loading) View.VISIBLE else View.GONE
        login_BTN_submit.isEnabled = !loading
        login_BTN_google.isEnabled = !loading
    }

    private fun showError(message: String) {
        login_LBL_error.text = message
        login_LBL_error.visibility = View.VISIBLE
    }
}
