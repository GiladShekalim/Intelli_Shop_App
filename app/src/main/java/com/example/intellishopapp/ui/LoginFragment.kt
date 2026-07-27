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
                        // Seed statuses/categories saved locally at registration on this device.
                        val saved = SessionManager.getInstance().loadPreferences(email)
                        SessionManager.getInstance().save(
                            UserSession(
                                userId = result.data.user_id,
                                email = email,
                                status = saved?.first ?: emptyList(),
                                hobbies = saved?.second ?: emptyList()
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
            val idToken = try {
                GoogleAuthHelper.getIdToken(requireContext())
            } catch (e: Exception) {
                Log.e("GoogleAuth", "getIdToken failed", e)
                setLoading(false)
                showError("Google: ${e.message}")
                return@launch
            }
            if (idToken == null) {
                setLoading(false)
                showError(getString(R.string.error_google_failed))
                return@launch
            }
            when (val result = authRepository.googleLogin(idToken)) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (!body.is_new) {
                        SessionManager.getInstance().save(
                            UserSession(
                                userId = body.user_id ?: "",
                                email = body.email ?: "",
                                username = body.username,
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
