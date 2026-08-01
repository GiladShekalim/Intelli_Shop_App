package com.example.intellishopapp.ui

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
import com.example.intellishopapp.utilities.ChipPalette
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
    private lateinit var register_LAY_membershipGrid: GridLayout
    private lateinit var register_BTN_submit: MaterialButton
    private lateinit var register_LBL_error: MaterialTextView
    private lateinit var register_LBL_loginLink: MaterialTextView
    private lateinit var register_LAY_progress: View

    private val authRepository = AuthRepository()

    private val selectedStatuses = mutableSetOf<String>()
    private val selectedInterests = mutableSetOf<String>()
    private val selectedMemberships = mutableSetOf<String>()

    private var googleMode = false
    private var generatedPassword: String? = null

    // Live username availability: debounced so we call at most once per idle window.
    // null = unknown/not checked; true = free; false = taken.
    private var usernameAvailable: Boolean? = null
    private val usernameCheckHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var usernameCheckRunnable: Runnable? = null

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
        register_LAY_membershipGrid = view.findViewById(R.id.register_LAY_membershipGrid)
        register_BTN_submit = view.findViewById(R.id.register_BTN_submit)
        register_LBL_error = view.findViewById(R.id.register_LBL_error)
        register_LBL_loginLink = view.findViewById(R.id.register_LBL_loginLink)
        register_LAY_progress = view.findViewById(R.id.register_LAY_progress)
    }

    private fun initViews() {
        buildToggleGrid(register_LAY_statusGrid, Constants.ConsumerStatus.ALL.map { it to it }, selectedStatuses)
        buildToggleGrid(register_LAY_interestGrid, Constants.Categories.ALL.map { it to it }, selectedInterests)
        buildToggleGrid(register_LAY_membershipGrid, Constants.Memberships.ALL, selectedMemberships)
        register_BTN_submit.setOnClickListener { submit() }
        register_LBL_loginLink.setOnClickListener { parentFragmentManager.popBackStack() }

        googleMode = arguments?.getBoolean(EXTRA_GOOGLE, false) ?: false
        arguments?.getString(EXTRA_EMAIL)?.let { register_ET_email.setText(it) }
        if (googleMode) {
            arguments?.getString(EXTRA_NAME)?.let { register_ET_username.setText(it) }
            register_ET_password.visibility = View.GONE
            generatedPassword = UUID.randomUUID().toString()
        }
        watchUsernameAvailability()
    }

    /**
     * Debounced live check: whenever the username changes, wait for a short idle
     * window (so we call at most once per burst of typing) then ask the backend if
     * it is free. Purely a hint — registration still rejects duplicates on submit.
     */
    private fun watchUsernameAvailability() {
        register_ET_username.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                usernameAvailable = null
                usernameCheckRunnable?.let { usernameCheckHandler.removeCallbacks(it) }
                val name = s?.toString()?.trim().orEmpty()
                if (name.length < MIN_USERNAME_CHECK) return
                val runnable = Runnable { checkUsername(name) }
                usernameCheckRunnable = runnable
                usernameCheckHandler.postDelayed(runnable, USERNAME_DEBOUNCE_MS)
            }
        })
    }

    private fun checkUsername(name: String) {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.checkUsername(name)) {
                is ApiResult.Success -> {
                    // Ignore a stale result if the field has since changed.
                    if (register_ET_username.text?.toString()?.trim() != name) return@launch
                    usernameAvailable = result.data
                    if (!result.data) {
                        register_ET_username.error = getString(R.string.error_username_taken)
                    } else if (register_ET_username.error == getString(R.string.error_username_taken)) {
                        register_ET_username.error = null
                    }
                }
                // Fail open: a failed check never blocks a legitimate sign-up.
                is ApiResult.Error -> usernameAvailable = null
            }
        }
    }

    override fun onDestroyView() {
        usernameCheckRunnable?.let { usernameCheckHandler.removeCallbacks(it) }
        super.onDestroyView()
    }

    /** [options] are (storedKey, shownLabel); for status/category the two are identical. */
    private fun buildToggleGrid(
        grid: GridLayout, options: List<Pair<String, String>>, selected: MutableSet<String>
    ) {
        grid.removeAllViews()
        grid.columnCount = 3
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        for ((key, label) in options) {
            val button = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            button.text = label
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
            // Single tap selects; a double tap on a selected option removes it.
            var lastTapTime = 0L
            button.setOnClickListener {
                val now = System.currentTimeMillis()
                val doubleTap = now - lastTapTime < DOUBLE_TAP_MS
                lastTapTime = now
                if (selected.contains(key)) {
                    if (doubleTap) {
                        selected.remove(key)
                        ChipPalette.styleToggle(button, false, brand)
                    }
                } else {
                    selected.add(key)
                    ChipPalette.styleToggle(button, true, brand)
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

        if (!validate(username, email, password)) return

        val request = RegisterRequest(
            username = username,
            password = password,
            email = email,
            status = selectedStatuses.toList(),
            age = ageText.toIntOrNull() ?: 0,
            location = location,
            hobbies = selectedInterests.toList(),
            memberships = selectedMemberships.toList()
        )

        register_LBL_error.visibility = View.GONE
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> {
                    if (result.data.status == "success") {
                        // Persist the sign-up selections locally so Profile can edit them.
                        SessionManager.getInstance().savePreferences(
                            email, selectedStatuses.toList(), selectedInterests.toList(),
                            selectedMemberships.toList()
                        )
                        if (googleMode) finishGoogleSignUp(email, password, username)
                        else finishRegister(email)
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

    /**
     * Username, email and password are required; the email must look like one and the
     * password must be long enough. Errors show on the offending field. Google users
     * have no password field, so that check is skipped for them.
     */
    private fun validate(username: String, email: String, password: String): Boolean {
        // Collect the fields that fail so a single light-orange notice can name them,
        // while each field also flags inline.
        val bad = mutableListOf<String>()
        if (username.isEmpty()) {
            register_ET_username.error = getString(R.string.error_username_required)
            bad.add(getString(R.string.field_username))
        } else if (usernameAvailable == false) {
            // Known-taken from the live check; the backend would reject it anyway.
            register_ET_username.error = getString(R.string.error_username_taken)
            bad.add(getString(R.string.field_username))
        }
        when {
            email.isEmpty() -> {
                register_ET_email.error = getString(R.string.error_email_required)
                bad.add(getString(R.string.field_email))
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                register_ET_email.error = getString(R.string.error_email_invalid)
                bad.add(getString(R.string.field_email))
            }
        }
        if (!googleMode) {
            when {
                password.isEmpty() -> {
                    register_ET_password.error = getString(R.string.error_password_required)
                    bad.add(getString(R.string.field_password))
                }
                password.length < MIN_PASSWORD -> {
                    register_ET_password.error = getString(R.string.error_password_short)
                    bad.add(getString(R.string.field_password))
                }
            }
        }
        if (bad.isNotEmpty()) {
            showError(getString(R.string.register_fix_fields, bad.joinToString(", ")))
            return false
        }
        return true
    }

    private fun finishRegister(email: String) {
        val shell = requireActivity() as MainActivity
        // Celebrate, hand the email to Login, then drop back to it. The fireworks live
        // in the shell so they keep playing over the Login screen.
        shell.playFireworks()
        shell.prefillLoginEmail(email)
        shell.showBanner(getString(R.string.register_success))
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
        private const val MIN_PASSWORD = 6
        private const val MIN_USERNAME_CHECK = 2
        private const val USERNAME_DEBOUNCE_MS = 2000L
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_GOOGLE = "extra_google"
    }
}
