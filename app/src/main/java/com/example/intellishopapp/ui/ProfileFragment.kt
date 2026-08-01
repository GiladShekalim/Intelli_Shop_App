package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.repository.ProfileRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * The signed-in user's profile: avatar, name, email, the personal lists (coupon
 * history, preferences, categories) and a Settings card holding night mode,
 * notifications, Change Password (non-Google users) and Sign Out. Reachable only
 * when logged in.
 */
class ProfileFragment : Fragment() {

    private lateinit var profile_LBL_title: MaterialTextView
    private lateinit var profile_LBL_email: MaterialTextView
    private lateinit var profile_LBL_myCoupons: MaterialTextView
    private lateinit var profile_LBL_preferences: MaterialTextView
    private lateinit var profile_LBL_categories: MaterialTextView
    private lateinit var profile_LBL_memberships: MaterialTextView
    private lateinit var profile_LBL_sentOffers: MaterialTextView
    private lateinit var profile_LBL_password: MaterialTextView
    private lateinit var profile_DIV_password: View
    private lateinit var profile_SW_night: SwitchCompat
    private lateinit var profile_SW_notifications: SwitchCompat
    private lateinit var profile_IMG_avatar: AppCompatImageView
    private lateinit var profile_BTN_signOut: MaterialTextView
    private lateinit var profile_BTN_delete: MaterialTextView

    private val profileRepository = ProfileRepository()
    // The avatar currently shown; re-shows of the tab skip re-loading it (which flickered)
    // unless the signed-in identity actually changed. Reset when the view is rebuilt.
    private var lastAvatarKey: String? = null

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
        profile_LBL_memberships = view.findViewById(R.id.profile_LBL_memberships)
        profile_LBL_sentOffers = view.findViewById(R.id.profile_LBL_sentOffers)
        profile_LBL_password = view.findViewById(R.id.profile_LBL_password)
        profile_DIV_password = view.findViewById(R.id.profile_DIV_password)
        profile_SW_night = view.findViewById(R.id.profile_SW_night)
        profile_SW_notifications = view.findViewById(R.id.profile_SW_notifications)
        profile_IMG_avatar = view.findViewById(R.id.profile_IMG_avatar)
        profile_BTN_signOut = view.findViewById(R.id.profile_BTN_signOut)
        profile_BTN_delete = view.findViewById(R.id.profile_BTN_delete)

        val shell = requireActivity() as MainActivity
        profile_LBL_myCoupons.setOnClickListener { shell.showCouponHistory() }
        profile_LBL_preferences.setOnClickListener { shell.showPreferences(PreferencesFragment.TYPE_STATUS) }
        profile_LBL_categories.setOnClickListener { shell.showPreferences(PreferencesFragment.TYPE_CATEGORY) }
        profile_LBL_memberships.setOnClickListener { shell.showPreferences(PreferencesFragment.TYPE_MEMBERSHIP) }
        profile_LBL_sentOffers.setOnClickListener { shell.showSentOffers() }
        profile_LBL_password.setOnClickListener { showChangePasswordDialog() }
        profile_BTN_signOut.setOnClickListener { shell.signOut() }
        profile_BTN_delete.setOnClickListener { confirmDeleteAccount() }
        // Click (not checked-change) so re-binding the switch never fires the listener.
        profile_SW_night.setOnClickListener { setNightMode(profile_SW_night.isChecked) }
        profile_SW_notifications.setOnClickListener {
            SessionManager.getInstance().setNotificationsEnabled(profile_SW_notifications.isChecked)
        }
        // Fresh view: force the avatar to bind once regardless of the cached key.
        lastAvatarKey = null
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
        val hidePassword = session?.isGoogle == true
        profile_LBL_password.visibility = if (hidePassword) View.GONE else View.VISIBLE
        profile_DIV_password.visibility = if (hidePassword) View.GONE else View.VISIBLE
        profile_SW_night.isChecked = SessionManager.getInstance().isNightMode()
        profile_SW_notifications.isChecked = SessionManager.getInstance().isNotificationsEnabled()
        bindAvatar(session)
    }

    /**
     * The avatar must match the signed-in user: only a Google account shows its
     * photo; email/password users (no upload feature) show the default icon. The
     * fragment is reused across sign-ins, so the non-Google branch actively resets
     * the image — otherwise a previous Google user's photo would linger.
     */
    private fun bindAvatar(session: com.example.intellishopapp.model.UserSession?) {
        val photoUrl = session
            ?.takeIf { it.isGoogle }
            ?.let { SessionManager.getInstance().getPhotoUrl(it.email) }
            ?.takeIf { it.isNotBlank() }

        // Skip the reload when the avatar would be identical to what's already shown —
        // re-opening the tab shouldn't blank-and-refill the image.
        val key = photoUrl ?: "default"
        if (key == lastAvatarKey) return
        lastAvatarKey = key

        if (photoUrl == null) {
            Glide.with(this).clear(profile_IMG_avatar)
            val pad = (24 * resources.displayMetrics.density).toInt()
            profile_IMG_avatar.setPadding(pad, pad, pad, pad)
            profile_IMG_avatar.setImageResource(R.drawable.ic_tab_profile)
            profile_IMG_avatar.imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_primary)
            )
            return
        }
        profile_IMG_avatar.setPadding(0, 0, 0, 0)
        profile_IMG_avatar.imageTintList = null
        Glide.with(this)
            .load(photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_tab_profile)
            .into(profile_IMG_avatar)
    }

    private fun setNightMode(night: Boolean) {
        if (SessionManager.getInstance().isNightMode() == night) return
        SessionManager.getInstance().setNightMode(night)
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
                            // Same rule as registration: a new password must be long enough.
                            nw.length < MIN_PASSWORD -> newPw.error = getString(R.string.error_password_short)
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

    /** Confirm, then delete — only treating it as done once the backend confirms it. */
    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_message)
            .setPositiveButton(R.string.delete_account_confirm) { d, _ ->
                d.dismiss()
                deleteAccount()
            }
            .setNegativeButton(R.string.pw_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun deleteAccount() {
        val shell = activity as? MainActivity ?: return
        shell.showBanner(getString(R.string.delete_account_progress))
        viewLifecycleOwner.lifecycleScope.launch {
            when (profileRepository.deleteAccount()) {
                // Confirmed gone on the backend: clear the local session and go home.
                is ApiResult.Success -> shell.onAccountDeleted()
                is ApiResult.Error -> shell.showBanner(getString(R.string.delete_account_failed))
            }
        }
    }

    companion object {
        private const val MIN_PASSWORD = 6
    }
}
