package com.example.intellishopapp.ui

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
import com.example.intellishopapp.utilities.ChipPalette
import com.example.intellishopapp.logic.CatalogFacets
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

    // One editor for three dimensions: statuses, categories, and memberships. Only the
    // dimension named by ARG_TYPE is edited; the other two are preserved on Save.
    private val type by lazy { requireArguments().getString(ARG_TYPE) ?: TYPE_STATUS }
    private val selected = mutableSetOf<String>() // keys of the edited dimension
    private var curStatuses: List<String> = emptyList()
    private var curHobbies: List<String> = emptyList()
    private var curMemberships: List<String> = emptyList()
    // Once the user has toggled anything, a late backend load must not clobber it.
    private var userEdited = false

    // The (key, label) options for the edited dimension, trimmed to labels the catalog
    // actually has (plus anything already selected). For statuses/categories the key is
    // the label; memberships show "HOT"/"Adif" but store "hot"/"adif".
    private val options: List<Pair<String, String>>
        get() = when (type) {
            TYPE_STATUS -> CatalogFacets.keepPresentPairs(
                Constants.ConsumerStatus.ALL.map { it to it }, CatalogFacets.Facet.STATUS, selected
            )
            TYPE_CATEGORY -> CatalogFacets.keepPresentPairs(
                Constants.Categories.ALL.map { it to it }, CatalogFacets.Facet.CATEGORY, selected
            )
            else -> CatalogFacets.keepPresentPairs(
                Constants.Memberships.ALL, CatalogFacets.Facet.MEMBERSHIP, selected
            )
        }

    private val titleRes: Int
        get() = when (type) {
            TYPE_STATUS -> R.string.profile_my_preferences
            TYPE_CATEGORY -> R.string.profile_my_categories
            else -> R.string.profile_my_memberships
        }

    private fun editedValues(): List<String> = when (type) {
        TYPE_STATUS -> curStatuses
        TYPE_CATEGORY -> curHobbies
        else -> curMemberships
    }

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
        pref_LBL_title.setText(titleRes)

        // Seed from the session immediately (instant render), then reconcile with the
        // authoritative backend copy so edits made on another device show up.
        seedFromSession()
        buildGrid()
        loadFromBackend()
    }

    private fun seedFromSession() {
        // The session is authoritative here: login seeds the three dimensions from the
        // backend, and loadFromBackend reconciles right after. (No stale local store.)
        val session = SessionManager.getInstance().get()
        curStatuses = session?.status.orEmpty()
        curHobbies = session?.hobbies.orEmpty()
        curMemberships = session?.memberships.orEmpty()
        selected.clear()
        selected.addAll(editedValues())
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
                curStatuses = result.data.statuses.orEmpty()
                curHobbies = result.data.hobbies.orEmpty()
                curMemberships = result.data.memberships.orEmpty()
                selected.clear()
                selected.addAll(editedValues())
                buildGrid()
            }
        }
    }

    private fun buildGrid() {
        // Selected first, then the rest. Each option is (storedKey, shownLabel).
        val ordered = options.filter { selected.contains(it.first) } +
            options.filterNot { selected.contains(it.first) }
        pref_LAY_grid.removeAllViews()
        pref_LAY_grid.columnCount = 3
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        for ((key, label) in ordered) {
            val button = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            button.text = label
            button.isAllCaps = false
            button.textSize = 11f
            button.insetTop = 0
            button.insetBottom = 0
            style(button, selected.contains(key), brand)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(6, 6, 6, 6)
            button.layoutParams = params
            button.setOnClickListener { onToggle(key, button, brand) }
            pref_LAY_grid.addView(button)
        }
    }

    /** Single tap toggles the option on/off and restyles it immediately. */
    private fun onToggle(key: String, button: MaterialButton, brand: Int) {
        userEdited = true
        if (selected.contains(key)) selected.remove(key) else selected.add(key)
        style(button, selected.contains(key), brand)
    }

    private fun save() {
        val edited = selected.toList()
        val statuses = if (type == TYPE_STATUS) edited else curStatuses
        val hobbies = if (type == TYPE_CATEGORY) edited else curHobbies
        val memberships = if (type == TYPE_MEMBERSHIP) edited else curMemberships
        // Optimistic: update the session now so Home personalization/filtering reflects it,
        // then write through to the backend for cross-device sync.
        SessionManager.getInstance().updatePreferences(statuses, hobbies, memberships)
        viewLifecycleOwner.lifecycleScope.launch {
            profileRepository.updatePreferences(statuses, hobbies, memberships)
        }
        val shell = requireActivity() as MainActivity
        shell.showBanner(getString(R.string.pref_saved))
        parentFragmentManager.popBackStack()
        // Celebrate the saved selection back on the Profile page.
        shell.playFireworks()
    }

    private fun style(button: MaterialButton, on: Boolean, brand: Int) =
        ChipPalette.styleToggle(button, on, brand)

    companion object {
        private const val ARG_TYPE = "type"
        const val TYPE_STATUS = "status"
        const val TYPE_CATEGORY = "category"
        const val TYPE_MEMBERSHIP = "membership"

        fun newInstance(type: String): PreferencesFragment =
            PreferencesFragment().apply { arguments = bundleOf(ARG_TYPE to type) }
    }
}
