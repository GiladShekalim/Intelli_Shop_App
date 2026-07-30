package com.example.intellishopapp.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.repository.ProfileRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Editable "My Preferences" (statuses) or "My Categories" (interests). Opens with
 * the user's current selection read fresh from the backend (synced across devices),
 * a tap toggles an option, and Save writes both dimensions back — optimistically to
 * the session for instant feedback, and through to the backend for cross-device sync.
 * Only the edited dimension changes; the other is preserved.
 */
class PreferencesFragment : Fragment() {

    private lateinit var pref_LBL_title: MaterialTextView
    private lateinit var pref_BTN_close: ImageButton
    private lateinit var pref_LAY_grid: GridLayout
    private lateinit var pref_BTN_save: MaterialButton

    private val profileRepository = ProfileRepository()

    private val palette = listOf(
        0xFFFFCDD2.toInt(), 0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFC5CAE9.toInt(),
        0xFFB3E5FC.toInt(), 0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(),
        0xFFFFE0B2.toInt(), 0xFFD1C4E9.toInt()
    )

    private val isStatus by lazy { requireArguments().getString(ARG_TYPE) == TYPE_STATUS }
    private val selected = mutableSetOf<String>()
    // The dimension NOT being edited here, preserved so Save never clears it.
    private var otherDimension: List<String> = emptyList()
    // Once the user has toggled anything, a late backend load must not clobber it.
    private var userEdited = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_preferences, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pref_LBL_title = view.findViewById(R.id.pref_LBL_title)
        pref_BTN_close = view.findViewById(R.id.pref_BTN_close)
        pref_LAY_grid = view.findViewById(R.id.pref_LAY_grid)
        pref_BTN_save = view.findViewById(R.id.pref_BTN_save)
        pref_BTN_close.setOnClickListener { parentFragmentManager.popBackStack() }
        pref_BTN_save.setOnClickListener { save() }
        pref_LBL_title.setText(
            if (isStatus) R.string.profile_my_preferences else R.string.profile_my_categories
        )

        // Seed from the session immediately (instant render), then reconcile with the
        // authoritative backend copy so edits made on another device show up.
        seedFromSession()
        buildGrid()
        loadFromBackend()
    }

    private fun seedFromSession() {
        // The session is authoritative here: login seeds status/hobbies from the
        // backend, and loadFromBackend reconciles right after. (No stale local store.)
        val session = SessionManager.getInstance().get()
        val statuses = session?.status.orEmpty()
        val hobbies = session?.hobbies.orEmpty()
        selected.clear()
        selected.addAll(if (isStatus) statuses else hobbies)
        otherDimension = if (isStatus) hobbies else statuses
    }

    /**
     * Pull the authoritative selection from the backend; keep the seed on 401/error.
     * Applied only if the fragment is untouched AND the backend profile is for the
     * same user as the session — a stale cookie for a different user must never
     * populate this editor.
     */
    private fun loadFromBackend() {
        val sessionEmail = SessionManager.getInstance().get()?.email.orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = profileRepository.getProfile()
            if (result is ApiResult.Success && view != null && !userEdited &&
                result.data.email.equals(sessionEmail, ignoreCase = true)
            ) {
                val statuses = result.data.statuses.orEmpty()
                val hobbies = result.data.hobbies.orEmpty()
                selected.clear()
                selected.addAll(if (isStatus) statuses else hobbies)
                otherDimension = if (isStatus) hobbies else statuses
                buildGrid()
            }
        }
    }

    private fun buildGrid() {
        val all = if (isStatus) Constants.ConsumerStatus.ALL else Constants.Categories.ALL
        // Selected first, then the rest.
        val ordered = all.filter { selected.contains(it) } + all.filterNot { selected.contains(it) }
        pref_LAY_grid.removeAllViews()
        pref_LAY_grid.columnCount = 3
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        for (value in ordered) {
            val button = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            button.text = value
            button.isAllCaps = false
            button.textSize = 11f
            button.insetTop = 0
            button.insetBottom = 0
            style(button, selected.contains(value), brand)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(6, 6, 6, 6)
            button.layoutParams = params
            button.setOnClickListener { onToggle(value, button, brand) }
            pref_LAY_grid.addView(button)
        }
    }

    /** Single tap toggles the option on/off and restyles it immediately. */
    private fun onToggle(value: String, button: MaterialButton, brand: Int) {
        userEdited = true
        if (selected.contains(value)) selected.remove(value) else selected.add(value)
        style(button, selected.contains(value), brand)
    }

    private fun save() {
        val statuses = if (isStatus) selected.toList() else otherDimension
        val hobbies = if (isStatus) otherDimension else selected.toList()
        // Optimistic: update the session now so Home personalization reflects it, then
        // write through to the backend for cross-device sync.
        SessionManager.getInstance().updatePreferences(statuses, hobbies)
        viewLifecycleOwner.lifecycleScope.launch { profileRepository.updatePreferences(statuses, hobbies) }
        (requireActivity() as MainActivity).showBanner(getString(R.string.pref_saved))
        parentFragmentManager.popBackStack()
    }

    private fun style(button: MaterialButton, on: Boolean, brand: Int) {
        if (on) {
            button.backgroundTintList = ColorStateList.valueOf(palette.random())
            button.setTextColor(0xFF212121.toInt())
        } else {
            // Keep the outlined look on a light surface (a null tint renders dark).
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.card_background)
            )
            button.setTextColor(brand)
        }
    }

    companion object {
        private const val ARG_TYPE = "type"
        const val TYPE_STATUS = "status"
        const val TYPE_CATEGORY = "category"

        fun newInstance(type: String): PreferencesFragment =
            PreferencesFragment().apply { arguments = bundleOf(ARG_TYPE to type) }
    }
}
