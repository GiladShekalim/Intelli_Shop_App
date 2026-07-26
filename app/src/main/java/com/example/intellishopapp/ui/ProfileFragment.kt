package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.repository.ProfileRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * The signed-in user's profile (Figma layout): avatar, name, email, a Day/Night
 * toggle, My Coupons, Change Password (non-Google users), and Sign Out. Reachable
 * only when logged in.
 */
class ProfileFragment : Fragment() {

    private lateinit var profile_LBL_title: MaterialTextView
    private lateinit var profile_LBL_email: MaterialTextView
    private lateinit var profile_LBL_myCoupons: MaterialTextView
    private lateinit var profile_LBL_preferences: MaterialTextView
    private lateinit var profile_LBL_categories: MaterialTextView
    private lateinit var profile_LBL_password: MaterialTextView
    private lateinit var profile_LBL_day: MaterialTextView
    private lateinit var profile_LBL_night: MaterialTextView
    private lateinit var profile_BTN_signOut: MaterialTextView

    private val profileRepository = ProfileRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profile_LBL_title = view.findViewById(R.id.profile_LBL_title)
        profile_LBL_email = view.findViewById(R.id.profile_LBL_email)
        profile_LBL_myCoupons = view.findViewById(R.id.profile_LBL_myCoupons)
        profile_LBL_preferences = view.findViewById(R.id.profile_LBL_preferences)
        profile_LBL_categories = view.findViewById(R.id.profile_LBL_categories)
        profile_LBL_password = view.findViewById(R.id.profile_LBL_password)
        profile_LBL_day = view.findViewById(R.id.profile_LBL_day)
        profile_LBL_night = view.findViewById(R.id.profile_LBL_night)
        profile_BTN_signOut = view.findViewById(R.id.profile_BTN_signOut)

        val shell = requireActivity() as MainActivity
        profile_LBL_myCoupons.setOnClickListener { shell.openCoupons() }
        profile_LBL_preferences.setOnClickListener { shell.showPreferences(PreferencesFragment.TYPE_STATUS) }
        profile_LBL_categories.setOnClickListener { shell.showPreferences(PreferencesFragment.TYPE_CATEGORY) }
        profile_LBL_password.setOnClickListener { showChangePasswordDialog() }
        profile_BTN_signOut.setOnClickListener { shell.signOut() }
        profile_LBL_day.setOnClickListener { setNightMode(false) }
        profile_LBL_night.setOnClickListener { setNightMode(true) }
        bind()
    }

    /** Re-read the session whenever the tab is shown (e.g. after logging in). */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) bind()
    }

    private fun bind() {
        val session = SessionManager.getInstance().get()
        profile_LBL_title.text = session?.username?.takeIf { it.isNotBlank() }
            ?: getString(R.string.tab_profile)
        profile_LBL_email.text = session?.email.orEmpty()
        // Google accounts have an auto-generated password; hide the change-password row.
        profile_LBL_password.visibility = if (session?.isGoogle == true) View.GONE else View.VISIBLE
        updateModeLabels()
    }

    private fun updateModeLabels() {
        val night = SessionManager.getInstance().isNightMode()
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        profile_LBL_day.setTextColor(if (night) secondary else brand)
        profile_LBL_night.setTextColor(if (night) brand else secondary)
    }

    private fun setNightMode(night: Boolean) {
        if (SessionManager.getInstance().isNightMode() == night) return
        SessionManager.getInstance().setNightMode(night)
        updateModeLabels()
        // Recreates activities to apply the theme.
        AppCompatDelegate.setDefaultNightMode(
            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val current = dialogView.findViewById<EditText>(R.id.pw_ET_current)
        val newPw = dialogView.findViewById<EditText>(R.id.pw_ET_new)
        val confirm = dialogView.findViewById<EditText>(R.id.pw_ET_confirm)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_change_password)
            .setView(dialogView)
            .setPositiveButton(R.string.pw_change, null)
            .setNegativeButton(R.string.pw_cancel) { d, _ -> d.dismiss() }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    // Custom click so validation errors show in the dialog, not dismiss it.
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val cur = current.text.toString()
                        val nw = newPw.text.toString()
                        val cf = confirm.text.toString()
                        when {
                            cur.isBlank() || nw.isBlank() || cf.isBlank() -> {
                                if (cur.isBlank()) current.error = getString(R.string.pw_fill_all)
                                if (nw.isBlank()) newPw.error = getString(R.string.pw_fill_all)
                                if (cf.isBlank()) confirm.error = getString(R.string.pw_fill_all)
                            }
                            nw != cf -> confirm.error = getString(R.string.pw_mismatch)
                            else -> submitPassword(cur, nw, cf, dialog, current)
                        }
                    }
                }
            }
            .show()
    }

    private fun submitPassword(
        current: String,
        new: String,
        confirm: String,
        dialog: AlertDialog,
        currentField: EditText
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = profileRepository.changePassword(current, new, confirm)) {
                is ApiResult.Success -> {
                    dialog.dismiss()
                    (activity as? MainActivity)?.showBanner(getString(R.string.pw_changed))
                }
                // Backend reason (e.g. wrong current password) shown on the field.
                is ApiResult.Error -> currentField.error = result.message
            }
        }
    }
}
