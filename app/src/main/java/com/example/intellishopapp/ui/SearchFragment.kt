package com.example.intellishopapp.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.adapter.CouponAdapter
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.model.dto.FilterRequest
import com.example.intellishopapp.model.dto.PercentageRange
import com.example.intellishopapp.model.dto.PriceRange
import com.example.intellishopapp.repository.SearchRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Coupon search over the shell. A text query hits /filtered_discounts/ (text-only
 * search for now; filters come next). Results reuse the coupon row with its heart;
 * tapping a result opens the detail sheet. Back / the arrow dismiss it.
 */
class SearchFragment : Fragment() {

    private lateinit var search_ET_query: EditText
    private lateinit var search_BTN_close: ImageButton
    private lateinit var search_BTN_go: MaterialButton
    private lateinit var search_BTN_ai: MaterialButton
    private lateinit var search_RCV_results: RecyclerView
    private lateinit var search_LBL_empty: MaterialTextView
    private lateinit var search_PRG_loading: ProgressBar
    private lateinit var search_BTN_filters: MaterialButton
    private lateinit var search_LAY_filters: View
    private lateinit var search_LAY_interestGrid: GridLayout
    private lateinit var search_LAY_statusGrid: GridLayout
    private lateinit var search_LAY_percentGrid: GridLayout
    private lateinit var search_ET_price: EditText
    private lateinit var search_BTN_apply: MaterialButton
    private lateinit var search_BTN_clear: MaterialButton

    private val searchRepository = SearchRepository()
    private var lastResults: List<CouponDto> = emptyList()

    private val selectedInterests = mutableSetOf<String>()
    private val selectedStatuses = mutableSetOf<String>()
    private var selectedBucket: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        search_RCV_results.layoutManager = LinearLayoutManager(requireContext())
        search_BTN_close.setOnClickListener { dismiss() }
        search_BTN_go.setOnClickListener { runSearch() }
        search_BTN_ai.setOnClickListener { runAiSearch() }
        search_BTN_filters.setOnClickListener { toggleFilters() }
        search_BTN_apply.setOnClickListener { applyFilters() }
        search_BTN_clear.setOnClickListener { clearFilters() }
        buildMultiGrid(search_LAY_interestGrid, Constants.Categories.ALL, selectedInterests)
        buildMultiGrid(search_LAY_statusGrid, Constants.ConsumerStatus.ALL, selectedStatuses)
        buildBucketGrid()
        search_ET_query.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else {
                false
            }
        }
        search_ET_query.requestFocus()
    }

    private fun findViews(view: View) {
        search_ET_query = view.findViewById(R.id.search_ET_query)
        search_BTN_close = view.findViewById(R.id.search_BTN_close)
        search_BTN_go = view.findViewById(R.id.search_BTN_go)
        search_BTN_ai = view.findViewById(R.id.search_BTN_ai)
        search_RCV_results = view.findViewById(R.id.search_RCV_results)
        search_LBL_empty = view.findViewById(R.id.search_LBL_empty)
        search_PRG_loading = view.findViewById(R.id.search_PRG_loading)
        search_BTN_filters = view.findViewById(R.id.search_BTN_filters)
        search_LAY_filters = view.findViewById(R.id.search_LAY_filters)
        search_LAY_interestGrid = view.findViewById(R.id.search_LAY_interestGrid)
        search_LAY_statusGrid = view.findViewById(R.id.search_LAY_statusGrid)
        search_LAY_percentGrid = view.findViewById(R.id.search_LAY_percentGrid)
        search_ET_price = view.findViewById(R.id.search_ET_price)
        search_BTN_apply = view.findViewById(R.id.search_BTN_apply)
        search_BTN_clear = view.findViewById(R.id.search_BTN_clear)
    }

    // --- filters ---

    private fun toggleFilters() {
        val show = search_LAY_filters.visibility != View.VISIBLE
        search_LAY_filters.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            hideKeyboard()
            search_RCV_results.visibility = View.GONE
            search_LBL_empty.visibility = View.GONE
            search_PRG_loading.visibility = View.GONE
        } else {
            search_LBL_empty.visibility = if (lastResults.isEmpty()) View.VISIBLE else View.GONE
            search_RCV_results.visibility = if (lastResults.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun applyFilters() {
        val text = queryText().takeIf { it.length >= 2 }
        val price = search_ET_price.text?.toString()?.trim()?.toDoubleOrNull()
            ?.takeIf { it >= 0 }?.let { PriceRange(enabled = true, max_value = it) }
        val percent = selectedBucket?.let { PercentageRange(enabled = true, bucket = it) }
        val request = FilterRequest(
            text_search = text,
            statuses = selectedStatuses.toList().ifEmpty { null },
            interests = selectedInterests.toList().ifEmpty { null },
            price_range = price,
            percentage_range = percent
        )
        search_LAY_filters.visibility = View.GONE
        launchSearch(R.string.search_empty) { searchRepository.search(request) }
    }

    private fun clearFilters() {
        selectedInterests.clear()
        selectedStatuses.clear()
        selectedBucket = null
        search_ET_price.text?.clear()
        buildMultiGrid(search_LAY_interestGrid, Constants.Categories.ALL, selectedInterests)
        buildMultiGrid(search_LAY_statusGrid, Constants.ConsumerStatus.ALL, selectedStatuses)
        buildBucketGrid()
    }

    /** Multi-select toggle grid (a tap selects/deselects the value). */
    private fun buildMultiGrid(grid: GridLayout, values: List<String>, selected: MutableSet<String>) {
        grid.removeAllViews()
        grid.columnCount = 3
        for (value in values) {
            val button = makeToggle(value) { btn ->
                if (selected.contains(value)) {
                    selected.remove(value); styleToggle(btn, false)
                } else {
                    selected.add(value); styleToggle(btn, true)
                }
            }
            grid.addView(button)
        }
    }

    /** Single-select grid for percentage buckets. */
    private fun buildBucketGrid() {
        search_LAY_percentGrid.removeAllViews()
        search_LAY_percentGrid.columnCount = 3
        for ((key, label) in Constants.PercentageBuckets.ALL) {
            val button = makeToggle(label) { btn ->
                val nowSelected = selectedBucket != key
                selectedBucket = if (nowSelected) key else null
                // repaint the whole row so only one stays active
                for (i in 0 until search_LAY_percentGrid.childCount) {
                    val child = search_LAY_percentGrid.getChildAt(i) as MaterialButton
                    styleToggle(child, child === btn && nowSelected)
                }
            }
            search_LAY_percentGrid.addView(button)
        }
    }

    private fun makeToggle(text: String, onClick: (MaterialButton) -> Unit): MaterialButton {
        val button = MaterialButton(
            requireContext(), null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        button.text = text
        button.isAllCaps = false
        button.textSize = 11f
        button.insetTop = 0
        button.insetBottom = 0
        styleToggle(button, false)
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(6, 6, 6, 6)
        button.layoutParams = params
        button.setOnClickListener { onClick(button) }
        return button
    }

    private fun styleToggle(button: MaterialButton, selected: Boolean) {
        val brand = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(brand)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.backgroundTintList = null
            button.setTextColor(brand)
        }
    }

    /** Plain text search across title/description/store/keywords (backend fields). */
    private fun runSearch() {
        val query = queryText()
        if (query.length < 2) {
            banner(R.string.search_min_chars)
            return
        }
        launchSearch(R.string.search_empty) { searchRepository.search(FilterRequest(text_search = query)) }
    }

    /** AI Helper: turn the free text into filters, then search with them. */
    private fun runAiSearch() {
        val query = queryText()
        if (query.isEmpty()) {
            banner(R.string.search_ai_empty)
            return
        }
        launchSearch(R.string.search_ai_failed) { searchRepository.aiSearch(query) }
    }

    private fun queryText(): String = search_ET_query.text?.toString()?.trim().orEmpty()

    private fun banner(res: Int) = (requireActivity() as MainActivity).showBanner(getString(res))

    private fun launchSearch(emptyRes: Int, block: suspend () -> ApiResult<List<CouponDto>>) {
        hideKeyboard()
        search_PRG_loading.visibility = View.VISIBLE
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = block()
            search_PRG_loading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> render(result.data, emptyRes)
                is ApiResult.Error -> {
                    lastResults = emptyList()
                    showEmpty(emptyRes)
                }
            }
        }
    }

    private fun render(coupons: List<CouponDto>, emptyRes: Int) {
        lastResults = coupons
        if (coupons.isEmpty()) {
            showEmpty(emptyRes)
            return
        }
        search_LBL_empty.visibility = View.GONE
        search_RCV_results.visibility = View.VISIBLE
        search_RCV_results.adapter = CouponAdapter(
            coupons, R.layout.item_favorite_row,
            onFavorite = { onFavoriteClicked(it) }
        ) { onCouponClicked(it) }
    }

    private fun showEmpty(messageRes: Int) {
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.setText(messageRes)
        search_LBL_empty.visibility = View.VISIBLE
    }

    private fun onCouponClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }

    private fun onFavoriteClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).toggleFavorite(coupon.discount_id.orEmpty()) {
            search_RCV_results.adapter?.notifyDataSetChanged()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(search_ET_query.windowToken, 0)
    }

    private fun dismiss() {
        hideKeyboard()
        parentFragmentManager.popBackStack()
    }
}
