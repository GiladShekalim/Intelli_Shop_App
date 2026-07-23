package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.adapter.CouponAdapter
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.model.dto.FilterRequest
import com.example.intellishopapp.repository.SearchRepository
import com.example.intellishopapp.utilities.ApiResult
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
    private lateinit var search_BTN_go: ImageButton
    private lateinit var search_RCV_results: RecyclerView
    private lateinit var search_LBL_empty: MaterialTextView
    private lateinit var search_PRG_loading: ProgressBar

    private val searchRepository = SearchRepository()
    private var lastResults: List<CouponDto> = emptyList()

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
        search_RCV_results = view.findViewById(R.id.search_RCV_results)
        search_LBL_empty = view.findViewById(R.id.search_LBL_empty)
        search_PRG_loading = view.findViewById(R.id.search_PRG_loading)
    }

    private fun runSearch() {
        val query = search_ET_query.text?.toString()?.trim().orEmpty()
        val shell = requireActivity() as MainActivity
        if (query.length < 2) {
            shell.showBanner(getString(R.string.search_min_chars))
            return
        }
        hideKeyboard()
        search_PRG_loading.visibility = View.VISIBLE
        search_RCV_results.visibility = View.GONE
        search_LBL_empty.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = searchRepository.search(FilterRequest(text_search = query))
            search_PRG_loading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> render(result.data)
                is ApiResult.Error -> {
                    lastResults = emptyList()
                    showEmpty(R.string.search_empty)
                }
            }
        }
    }

    private fun render(coupons: List<CouponDto>) {
        lastResults = coupons
        if (coupons.isEmpty()) {
            showEmpty(R.string.search_empty)
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
