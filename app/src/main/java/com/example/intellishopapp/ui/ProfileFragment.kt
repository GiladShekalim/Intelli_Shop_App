package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

/**
 * The signed-in user's profile: name, email, a shortcut to My Coupons, and Sign
 * Out. Reachable only when logged in (a guest tapping Profile is sent to Login).
 */
class ProfileFragment : Fragment() {

    private lateinit var profile_LBL_title: MaterialTextView
    private lateinit var profile_LBL_email: MaterialTextView
    private lateinit var profile_LBL_myCoupons: MaterialTextView
    private lateinit var profile_BTN_signOut: MaterialButton

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
        profile_BTN_signOut = view.findViewById(R.id.profile_BTN_signOut)

        profile_LBL_myCoupons.setOnClickListener {
            (requireActivity() as MainActivity).openCoupons()
        }
        profile_BTN_signOut.setOnClickListener {
            (requireActivity() as MainActivity).signOut()
        }
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
    }
}
