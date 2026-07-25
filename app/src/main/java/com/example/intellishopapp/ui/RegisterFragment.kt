package com.example.intellishopapp.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Registration inside the shell. Statuses and interests are picked from a grid of
 * toggle buttons (each selected one gets a random light colour). New Google users
 * arrive here pre-filled to complete their profile.
 */
class RegisterFragment : Fragment() {

    private lateinit var register_ET_username: EditText
    private lateinit var register_ET_email: EditText
    private lateinit var register_ET_password: EditText
    private lateinit var register_ET_age: EditText
    private lateinit var register_ET_location: EditText
    private lateinit var register_LAY_statusGrid: GridLayout
    private lateinit var register_LAY_interestGrid: GridLayout
    private lateinit var register_BTN_submit: MaterialButton
    private lateinit var register_LBL_error: MaterialTextView
    private lateinit var register_LBL_loginLink: MaterialTextView
    private lateinit var register_LAY_progress: View

    private val authRepository = AuthRepository()

    private val selectedStatuses = mutableSetOf<String>()
    private val selectedInterests = mutableSetOf<String>()

    private var googleMode = false
    private var generatedPassword: String? = null

    private val palette = listOf(
        0xFFFFCDD2.toInt(), 0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFC5CAE9.toInt(),
        0xFFB3E5FC.toInt(), 0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(),
        0xFFFFE0B2.toInt(), 0xFFD1C4E9.toInt()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        initViews()
    }

    private fun findViews(view: View) {
        register_ET_username = view.findViewById(R.id.register_ET_username)
        register_ET_email = view.findViewById(R.id.register_ET_email)
        register_ET_password = view.findViewById(R.id.register_ET_password)
        register_ET_age = view.findViewById(R.id.register_ET_age)
        register_ET_location = view.findViewById(R.id.register_ET_location)
        register_LAY_statusGrid = view.findViewById(R.id.register_LAY_statusGrid)
        register_LAY_interestGrid = view.findViewById(R.id.register_LAY_interestGrid)
        register_BTN_submit = view.findViewById(R.id.register_BTN_submit)
        register_LBL_error = view.findViewById(R.id.register_LBL_error)
        register_LBL_loginLink = view.findViewById(R.id.register_LBL_loginLink)
        register_LAY_progress = view.findViewById(R.id.register_LAY_progress)
    }

    private fun initViews() {
        buildToggleGrid(register_LAY_statusGrid, Constants.ConsumerStatus.ALL, selectedStatuses)
        buildToggleGrid(register_LAY_interestGrid, Constants.Categories.ALL, selectedInterests)
        register_BTN_submit.setOnClickListener { submit() }
        register_LBL_loginLink.setOnClickListener { parentFragmentManager.popBackStack() }

        googleMode = arguments?.getBoolean(EXTRA_GOOGLE, false) ?: false
        arguments?.getString(EXTRA_EMAIL)?.let { register_ET_email.setText(it) }
        if (googleMode) {
            arguments?.getString(EXTRA_NAME)?.let { register_ET_username.setText(it) }
            register_ET_password.visibility = View.GONE
            generatedPassword = UUID.randomUUID().toString()
        }
    }

    private fun buildToggleGrid(grid: GridLayout, values: List<String>, selected: MutableSet<String>) {
        grid.removeAllViews()
        grid.columnCount = 3
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        for (value in values) {
            val button = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            button.text = value
            button.isAllCaps = false
            button.textSize = 11f
            button.setTextColor(brand)
            button.insetTop = 0
            button.insetBottom = 0
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(6, 6, 6, 6)
            button.layoutParams = params
            // Single tap selects; a double tap on a selected category removes it.
            var lastTapTime = 0L
            button.setOnClickListener {
                val now = System.currentTimeMillis()
                val doubleTap = now - lastTapTime < DOUBLE_TAP_MS
                lastTapTime = now
                if (selected.contains(value)) {
                    if (doubleTap) {
                        selected.remove(value)
                        button.backgroundTintList = null
                        button.setTextColor(brand)
                    }
                } else {
                    selected.add(value)
                    button.backgroundTintList = ColorStateList.valueOf(palette.random())
                    button.setTextColor(0xFF212121.toInt())
                }
            }
            grid.addView(button)
        }
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

        val request = RegisterRequest(
            username = username,
            password = password,
            email = email,
            status = selectedStatuses.toList(),
            age = ageText.toIntOrNull() ?: 0,
            location = location,
            hobbies = selectedInterests.toList()
        )

        register_LBL_error.visibility = View.GONE
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> {
                    if (result.data.status == "success") {
                        // Persist the sign-up selections locally so Profile can edit them.
                        SessionManager.getInstance()
                            .savePreferences(email, selectedStatuses.toList(), selectedInterests.toList())
                        if (googleMode) finishGoogleSignUp(email, password, username)
                        else finishRegister()
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

    private fun finishRegister() {
        (requireActivity() as MainActivity).showBanner(getString(R.string.register_success))
        parentFragmentManager.popBackStack()
    }

    private suspend fun finishGoogleSignUp(email: String, password: String, username: String) {
        when (val login = authRepository.login(email, password)) {
            is ApiResult.Success -> {
                SessionManager.getInstance().save(
                    UserSession(userId = login.data.user_id, email = email, username = username, isGoogle = true)
                )
                val shell = requireActivity() as MainActivity
                shell.showBanner(getString(R.string.welcome_back))
                shell.dismissAuth()
            }
            is ApiResult.Error -> {
                setLoading(false)
                showError(getString(R.string.error_register_failed))
            }
        }
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
        private const val DOUBLE_TAP_MS = 300L
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_GOOGLE = "extra_google"
    }
}
