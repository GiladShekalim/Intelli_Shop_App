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
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

/**
 * Read-only view of the user's saved statuses ("My Preferences") or categories
 * ("My Categories"), shown like registration (saved ones first, light colors).
 * Editing is not active yet — tapping an option just shows a notification.
 */
class PreferencesFragment : Fragment() {

    private lateinit var pref_LBL_title: MaterialTextView
    private lateinit var pref_BTN_close: ImageButton
    private lateinit var pref_LAY_grid: GridLayout

    private val palette = listOf(
        0xFFFFCDD2.toInt(), 0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFC5CAE9.toInt(),
        0xFFB3E5FC.toInt(), 0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(),
        0xFFFFE0B2.toInt(), 0xFFD1C4E9.toInt()
    )

    private val isStatus by lazy { requireArguments().getString(ARG_TYPE) == TYPE_STATUS }
    private val selected = mutableSetOf<String>()

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
        pref_BTN_close.setOnClickListener { parentFragmentManager.popBackStack() }

        val manager = SessionManager.getInstance()
        val session = manager.get()
        // Prefer the live session; a bare login leaves it empty, so fall back to the
        // per-email store saved at registration so the choices still show selected.
        val fromSession = if (isStatus) session?.status.orEmpty() else session?.hobbies.orEmpty()
        val chosen = fromSession.ifEmpty {
            val stored = manager.loadPreferences(session?.email.orEmpty())
            if (isStatus) stored?.first.orEmpty() else stored?.second.orEmpty()
        }
        selected.addAll(chosen)
        pref_LBL_title.setText(
            if (isStatus) R.string.profile_my_preferences else R.string.profile_my_categories
        )
        buildGrid()
    }

    private fun buildGrid() {
        val all = if (isStatus) Constants.ConsumerStatus.ALL else Constants.Categories.ALL
        // Saved selections first, then the rest.
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
            button.setOnClickListener { onToggle() }
            pref_LAY_grid.addView(button)
        }
    }

    private fun onToggle() {
        // Editing is not active yet: the saved selection is read-only.
        (requireActivity() as MainActivity).showBanner(getString(R.string.pref_disabled))
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
