package com.example.intellishopapp.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
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
 * Search page body. The static top bar (owned by MainActivity) drives it: tapping
 * the field shows the filters here; the AI Filter / Search buttons call
 * [runShellSearch]. Applied filters show as removable labels above the results.
 */
class SearchFragment : Fragment() {

    private lateinit var search_RCV_results: RecyclerView
    private lateinit var search_LBL_empty: MaterialTextView
    private lateinit var search_PRG_loading: ProgressBar
    private lateinit var search_LAY_filters: View
    private lateinit var search_LAY_interestGrid: GridLayout
    private lateinit var search_LAY_statusGrid: GridLayout
    private lateinit var search_LAY_percentGrid: GridLayout
    private lateinit var search_ET_price: EditText
    private lateinit var search_BTN_clear: MaterialButton
    private lateinit var search_BTN_doSearch: MaterialButton
    private lateinit var search_LAY_labelsScroll: View
    private lateinit var search_LAY_labels: LinearLayout

    private val searchRepository = SearchRepository()

    private val selectedInterests = mutableSetOf<String>()
    private val selectedStatuses = mutableSetOf<String>()
    private var selectedBucket: String? = null
    private var currentPrice: Double? = null
    private var lastText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        search_RCV_results.layoutManager = LinearLayoutManager(requireContext())
        search_BTN_clear.setOnClickListener { clearFilters() }
        search_BTN_doSearch.setOnClickListener { runShellSearch(ai = false) }
        buildMultiGrid(search_LAY_interestGrid, Constants.Categories.ALL, selectedInterests)
        buildMultiGrid(search_LAY_statusGrid, Constants.ConsumerStatus.ALL, selectedStatuses)
        buildBucketGrid()

        // If opened by an AI/Search tap, run that; otherwise show the filters.
        when (val pending = (requireActivity() as MainActivity).consumePendingSearch()) {
            null -> showFilters()
            else -> runShellSearch(pending)
        }
    }

    private fun findViews(view: View) {
        search_RCV_results = view.findViewById(R.id.search_RCV_results)
        search_LBL_empty = view.findViewById(R.id.search_LBL_empty)
        search_PRG_loading = view.findViewById(R.id.search_PRG_loading)
        search_LAY_filters = view.findViewById(R.id.search_LAY_filters)
        search_LAY_interestGrid = view.findViewById(R.id.search_LAY_interestGrid)
        search_LAY_statusGrid = view.findViewById(R.id.search_LAY_statusGrid)
        search_LAY_percentGrid = view.findViewById(R.id.search_LAY_percentGrid)
        search_ET_price = view.findViewById(R.id.search_ET_price)
        search_BTN_clear = view.findViewById(R.id.search_BTN_clear)
        search_BTN_doSearch = view.findViewById(R.id.search_BTN_doSearch)
        search_LAY_labelsScroll = view.findViewById(R.id.search_LAY_labelsScroll)
        search_LAY_labels = view.findViewById(R.id.search_LAY_labels)
    }

    /** Show the filter panel (called when the top-bar field is tapped). */
    fun showFilters() {
        search_LAY_filters.visibility = View.VISIBLE
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.visibility = View.GONE
        search_PRG_loading.visibility = View.GONE
        search_LAY_labelsScroll.visibility = View.GONE
    }

    /** Run a search from the top bar: AI (parse text into filters) or simple (text + filters). */
    fun runShellSearch(ai: Boolean) {
        if (view == null) return
        val query = (requireActivity() as MainActivity).searchQueryText().trim()
        if (ai) {
            if (query.isEmpty()) {
                banner(R.string.search_ai_empty)
                return
            }
            runAi(query)
        } else {
            if (query.length == 1) {
                banner(R.string.search_min_chars)
                return
            }
            currentPrice = readPrice()
            lastText = query.takeIf { it.length >= 2 }
            doSearch()
        }
    }

    private fun runAi(query: String) {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = searchRepository.aiFilters(query)) {
                is ApiResult.Success -> {
                    val f = result.data
                    val interests = f.interests.orEmpty()
                    val statuses = f.statuses.orEmpty()
                    val bucket = f.percentage_range?.takeIf { it.enabled }?.bucket
                    val price = f.price_range?.takeIf { it.enabled }?.max_value
                    // Old-web parity: if nothing was understood, say so and keep the
                    // filter panel, rather than running an empty (all-results) search.
                    if (interests.isEmpty() && statuses.isEmpty() && bucket == null && price == null) {
                        showFilters()
                        banner(R.string.search_ai_none)
                        return@launch
                    }
                    selectedInterests.clear(); selectedInterests.addAll(interests)
                    selectedStatuses.clear(); selectedStatuses.addAll(statuses)
                    selectedBucket = bucket
                    currentPrice = price
                    lastText = null // AI parsed the text into filters; not a text search
                    applySelectionsToUi()
                    // Clear the text field and confirm, like the old web AI helper did.
                    (requireActivity() as MainActivity).clearSearchQuery()
                    banner(R.string.search_ai_applied)
                    doSearch()
                }
                is ApiResult.Error -> {
                    search_PRG_loading.visibility = View.GONE
                    showEmpty(R.string.search_ai_failed)
                }
            }
        }
    }

    private fun doSearch() {
        val request = FilterRequest(
            text_search = lastText,
            statuses = selectedStatuses.toList().ifEmpty { null },
            interests = selectedInterests.toList().ifEmpty { null },
            price_range = currentPrice?.let { PriceRange(enabled = true, max_value = it) },
            percentage_range = selectedBucket?.let { PercentageRange(enabled = true, bucket = it) }
        )
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = searchRepository.search(request)
            search_PRG_loading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> {
                    renderLabels()
                    render(result.data)
                }
                is ApiResult.Error -> showEmpty(R.string.search_empty)
            }
        }
    }

    /** Re-run the current search after a filter label was removed. */
    private fun reSearch() {
        applySelectionsToUi()
        doSearch()
    }

    private fun render(coupons: List<CouponDto>) {
        if (coupons.isEmpty()) {
            search_RCV_results.visibility = View.GONE
            search_LBL_empty.setText(R.string.search_empty)
            search_LBL_empty.visibility = View.VISIBLE
            return
        }
        search_LBL_empty.visibility = View.GONE
        search_RCV_results.visibility = View.VISIBLE
        search_RCV_results.adapter = CouponAdapter(
            coupons, R.layout.item_favorite_row,
            onFavorite = { onFavoriteClicked(it) }
        ) { onCouponClicked(it) }
    }

    private fun renderLabels() {
        search_LAY_labels.removeAllViews()
        val chips = mutableListOf<Pair<String, () -> Unit>>()
        selectedInterests.toList().forEach { v -> chips.add(v to { selectedInterests.remove(v); reSearch() }) }
        selectedStatuses.toList().forEach { v -> chips.add(v to { selectedStatuses.remove(v); reSearch() }) }
        currentPrice?.let { p ->
            chips.add(getString(R.string.search_label_price, priceText(p)) to {
                currentPrice = null; search_ET_price.text?.clear(); reSearch()
            })
        }
        selectedBucket?.let { b ->
            chips.add(bucketLabel(b) to { selectedBucket = null; reSearch() })
        }
        if (chips.isEmpty()) {
            search_LAY_labelsScroll.visibility = View.GONE
            return
        }
        search_LAY_labelsScroll.visibility = View.VISIBLE
        for ((text, remove) in chips) {
            search_LAY_labels.addView(makeLabelChip(text, remove))
        }
    }

    private fun makeLabelChip(text: String, onRemove: () -> Unit): MaterialTextView {
        val chip = MaterialTextView(requireContext())
        chip.text = getString(R.string.search_label_remove, text)
        chip.setBackgroundResource(R.drawable.bg_label)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        chip.textSize = 12f
        chip.setPadding(24, 12, 24, 12)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 12
        chip.layoutParams = lp
        chip.setOnClickListener { onRemove() }
        return chip
    }

    private fun bucketLabel(key: String): String =
        Constants.PercentageBuckets.ALL.firstOrNull { it.first == key }?.second ?: key

    private fun priceText(p: Double): String =
        if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()

    private fun readPrice(): Double? =
        search_ET_price.text?.toString()?.trim()?.toDoubleOrNull()?.takeIf { it >= 0 }

    private fun showLoading() {
        search_LAY_filters.visibility = View.GONE
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.visibility = View.GONE
        search_LAY_labelsScroll.visibility = View.GONE
        search_PRG_loading.visibility = View.VISIBLE
    }

    private fun showEmpty(messageRes: Int) {
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.setText(messageRes)
        search_LBL_empty.visibility = View.VISIBLE
    }

    private fun banner(res: Int) = (requireActivity() as MainActivity).showBanner(getString(res))

    // --- filter panel ---

    private fun clearFilters() {
        selectedInterests.clear()
        selectedStatuses.clear()
        selectedBucket = null
        currentPrice = null
        search_ET_price.text?.clear()
        applySelectionsToUi()
    }

    private fun applySelectionsToUi() {
        buildMultiGrid(search_LAY_interestGrid, Constants.Categories.ALL, selectedInterests)
        buildMultiGrid(search_LAY_statusGrid, Constants.ConsumerStatus.ALL, selectedStatuses)
        buildBucketGrid()
        search_ET_price.setText(currentPrice?.let { priceText(it) } ?: "")
    }

    private fun buildMultiGrid(grid: GridLayout, values: List<String>, selected: MutableSet<String>) {
        grid.removeAllViews()
        grid.columnCount = 3
        for (value in values) {
            val button = makeToggle(value, selected.contains(value)) { btn ->
                if (selected.contains(value)) {
                    selected.remove(value); styleToggle(btn, false)
                } else {
                    selected.add(value); styleToggle(btn, true)
                }
            }
            grid.addView(button)
        }
    }

    private fun buildBucketGrid() {
        search_LAY_percentGrid.removeAllViews()
        search_LAY_percentGrid.columnCount = 3
        for ((key, label) in Constants.PercentageBuckets.ALL) {
            val button = makeToggle(label, key == selectedBucket) { btn ->
                val nowSelected = selectedBucket != key
                selectedBucket = if (nowSelected) key else null
                for (i in 0 until search_LAY_percentGrid.childCount) {
                    val child = search_LAY_percentGrid.getChildAt(i) as MaterialButton
                    styleToggle(child, child === btn && nowSelected)
                }
            }
            search_LAY_percentGrid.addView(button)
        }
    }

    private fun makeToggle(text: String, selected: Boolean, onClick: (MaterialButton) -> Unit): MaterialButton {
        val button = MaterialButton(
            requireContext(), null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        button.text = text
        button.isAllCaps = false
        button.textSize = 11f
        button.insetTop = 0
        button.insetBottom = 0
        styleToggle(button, selected)
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
            // Keep the outlined look on a light surface (a null tint renders dark).
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.card_background)
            )
            button.setTextColor(brand)
        }
    }

    private fun onCouponClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }

    private fun onFavoriteClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).toggleFavorite(coupon.discount_id.orEmpty()) {
            search_RCV_results.adapter?.notifyDataSetChanged()
        }
    }
}
