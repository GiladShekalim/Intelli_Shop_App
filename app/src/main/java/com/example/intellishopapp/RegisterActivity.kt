package com.example.intellishopapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import java.util.UUID
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var register_ET_username: TextInputEditText
    private lateinit var register_ET_email: TextInputEditText
    private lateinit var register_ET_password: TextInputEditText
    private lateinit var register_LAY_passwordField: View
    private lateinit var register_ET_age: TextInputEditText
    private lateinit var register_ET_location: TextInputEditText
    private lateinit var register_LAY_statusChips: ChipGroup
    private lateinit var register_LAY_hobbiesChips: ChipGroup
    private lateinit var register_BTN_submit: MaterialButton
    private lateinit var register_LBL_error: MaterialTextView
    private lateinit var register_LBL_loginLink: MaterialTextView
    private lateinit var register_LAY_progress: View

    private val authRepository = AuthRepository()

    private var googleMode = false
    private var generatedPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_LAY_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViews()
        initViews()
    }

    private fun findViews() {
        register_ET_username = findViewById(R.id.register_ET_username)
        register_ET_email = findViewById(R.id.register_ET_email)
        register_ET_password = findViewById(R.id.register_ET_password)
        register_LAY_passwordField = findViewById(R.id.register_LAY_passwordField)
        register_ET_age = findViewById(R.id.register_ET_age)
        register_ET_location = findViewById(R.id.register_ET_location)
        register_LAY_statusChips = findViewById(R.id.register_LAY_statusChips)
        register_LAY_hobbiesChips = findViewById(R.id.register_LAY_hobbiesChips)
        register_BTN_submit = findViewById(R.id.register_BTN_submit)
        register_LBL_error = findViewById(R.id.register_LBL_error)
        register_LBL_loginLink = findViewById(R.id.register_LBL_loginLink)
        register_LAY_progress = findViewById(R.id.register_LAY_progress)
    }

    private fun initViews() {
        addChips(register_LAY_statusChips, Constants.ConsumerStatus.ALL)
        addChips(register_LAY_hobbiesChips, Constants.Categories.ALL)
        register_BTN_submit.setOnClickListener { submit() }
        register_LBL_loginLink.setOnClickListener { goToLogin(null) }

        // Coming from Google: prefill identity, hide the password field (auto-generated).
        googleMode = intent.getBooleanExtra(EXTRA_GOOGLE, false)
        intent.getStringExtra(EXTRA_EMAIL)?.let { register_ET_email.setText(it) }
        if (googleMode) {
            intent.getStringExtra(EXTRA_NAME)?.let { register_ET_username.setText(it) }
            register_LAY_passwordField.visibility = View.GONE
            generatedPassword = UUID.randomUUID().toString()
        }
    }

    private fun addChips(group: ChipGroup, values: List<String>) {
        for (value in values) {
            val chip = Chip(this)
            chip.text = value
            chip.isCheckable = true
            group.addView(chip)
        }
    }

    private fun selectedChips(group: ChipGroup): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.isChecked) result.add(chip.text.toString())
        }
        return result
    }

    private fun submit() {
        val username = register_ET_username.text?.toString()?.trim().orEmpty()
        val email = register_ET_email.text?.toString()?.trim().orEmpty()
        val password = if (googleMode) generatedPassword.orEmpty()
        else register_ET_password.text?.toString().orEmpty()
        val ageText = register_ET_age.text?.toString()?.trim().orEmpty()
        val location = register_ET_location.text?.toString()?.trim().orEmpty()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fill_required))
            return
        }
        val age = ageText.toIntOrNull() ?: 0

        val request = RegisterRequest(
            username = username,
            password = password,
            email = email,
            status = selectedChips(register_LAY_statusChips),
            age = age,
            location = location,
            hobbies = selectedChips(register_LAY_hobbiesChips)
        )

        register_LBL_error.visibility = View.GONE
        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> {
                    if (result.data.status == "success") {
                        if (googleMode) {
                            finishGoogleSignUp(email, password, username)
                        } else {
                            Toast.makeText(this@RegisterActivity, R.string.register_success, Toast.LENGTH_SHORT).show()
                            goToLogin(email)
                        }
                    } else {
                        setLoading(false)
                        showError(result.data.message ?: getString(R.string.error_register_failed))
                    }
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    showError(result.message)
                }
            }
        }
    }

    // New Google user: register created the account, now log in with the generated
    // password and go straight to main (they'll use Google next time).
    private suspend fun finishGoogleSignUp(email: String, password: String, username: String) {
        when (val login = authRepository.login(email, password)) {
            is ApiResult.Success -> {
                SessionManager.getInstance().save(
                    UserSession(userId = login.data.user_id, email = email, username = username)
                )
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            is ApiResult.Error -> {
                setLoading(false)
                showError(getString(R.string.error_register_failed))
            }
        }
    }

    private fun goToLogin(prefillEmail: String?) {
        val intent = Intent(this, LoginActivity::class.java)
        if (prefillEmail != null) intent.putExtra(EXTRA_EMAIL, prefillEmail)
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        register_LAY_progress.visibility = if (loading) View.VISIBLE else View.GONE
        register_BTN_submit.isEnabled = !loading
    }

    private fun showError(message: String) {
        register_LBL_error.text = message
        register_LBL_error.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_GOOGLE = "extra_google"
    }
}
