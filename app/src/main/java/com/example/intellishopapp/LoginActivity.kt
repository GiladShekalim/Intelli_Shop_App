package com.example.intellishopapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.GoogleAuthHelper
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var login_ET_email: TextInputEditText
    private lateinit var login_ET_password: TextInputEditText
    private lateinit var login_BTN_submit: MaterialButton
    private lateinit var login_BTN_google: MaterialButton
    private lateinit var login_LBL_error: MaterialTextView
    private lateinit var login_LBL_registerLink: MaterialTextView
    private lateinit var login_LAY_progress: View

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Already logged in (e.g. after relaunch) → go straight to main.
        if (SessionManager.getInstance().isLoggedIn()) {
            goToMain()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_LAY_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViews()
        initViews()
    }

    private fun findViews() {
        login_ET_email = findViewById(R.id.login_ET_email)
        login_ET_password = findViewById(R.id.login_ET_password)
        login_BTN_submit = findViewById(R.id.login_BTN_submit)
        login_BTN_google = findViewById(R.id.login_BTN_google)
        login_LBL_error = findViewById(R.id.login_LBL_error)
        login_LBL_registerLink = findViewById(R.id.login_LBL_registerLink)
        login_LAY_progress = findViewById(R.id.login_LAY_progress)
    }

    private fun initViews() {
        intent.getStringExtra(RegisterActivity.EXTRA_EMAIL)?.let { login_ET_email.setText(it) }
        login_BTN_submit.setOnClickListener { submit() }
        login_BTN_google.setOnClickListener { signInWithGoogle() }
        login_LBL_registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        login_LBL_error.visibility = View.GONE
        setLoading(true)
        lifecycleScope.launch {
            val idToken = try {
                GoogleAuthHelper.getIdToken(this@LoginActivity)
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
                                username = body.username
                            )
                        )
                        goToMain()
                    } else {
                        // New Google user — complete profile/categories then register.
                        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                        intent.putExtra(RegisterActivity.EXTRA_EMAIL, body.email)
                        intent.putExtra(RegisterActivity.EXTRA_NAME, body.name)
                        intent.putExtra(RegisterActivity.EXTRA_GOOGLE, true)
                        startActivity(intent)
                        setLoading(false)
                    }
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    showError(getString(R.string.error_google_failed))
                }
            }
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
        lifecycleScope.launch {
            authRepository.ensureCsrfPrimed()
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    if (result.data.status == "success") {
                        SessionManager.getInstance().save(
                            UserSession(userId = result.data.user_id, email = email)
                        )
                        goToMain()
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

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        login_LAY_progress.visibility = if (loading) View.VISIBLE else View.GONE
        login_BTN_submit.isEnabled = !loading
    }

    private fun showError(message: String) {
        login_LBL_error.text = message
        login_LBL_error.visibility = View.VISIBLE
    }
}
