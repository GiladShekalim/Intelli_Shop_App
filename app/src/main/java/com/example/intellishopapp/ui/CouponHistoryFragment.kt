package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Redeemed Offers: coupons the user actually redeemed (copy / go to site / go to
 * offer), most recent first. Distinct from Home's "Recently Viewed", which lists
 * coupons merely opened. Tapping a row opens the shared Coupon Detail sheet.
 */
class CouponHistoryFragment : Fragment() {

    private lateinit var history_RCV_list: RecyclerView
    private lateinit var history_LBL_empty: MaterialTextView
    private lateinit var history_PRG_loading: ProgressBar
    private lateinit var history_BTN_close: ImageButton

    private val couponRepository = CouponRepository()
    private val redeemRepository = com.example.intellishopapp.repository.RedeemRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_coupon_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        history_RCV_list = view.findViewById(R.id.history_RCV_list)
        history_LBL_empty = view.findViewById(R.id.history_LBL_empty)
        history_PRG_loading = view.findViewById(R.id.history_PRG_loading)
        history_BTN_close = view.findViewById(R.id.history_BTN_close)
        history_BTN_close.setOnClickListener { parentFragmentManager.popBackStack() }
        history_RCV_list.layoutManager = LinearLayoutManager(requireContext())
        load()
    }

    private fun load() {
        history_PRG_loading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            // Pull the authoritative redemptions from the backend (keep local on 401/error).
            (redeemRepository.get() as? ApiResult.Success)?.let {
                SessionManager.getInstance().setRedeemed(it.data)
            }
            val ids = SessionManager.getInstance().getRedeemed()
            if (ids.isEmpty()) {
                history_PRG_loading.visibility = View.GONE
                showEmpty()
                return@launch
            }
            when (val result = couponRepository.getAllCoupons()) {
                is ApiResult.Success -> {
                    history_PRG_loading.visibility = View.GONE
                    val byId = result.data.associateBy { it.discount_id }
                    render(ids.mapNotNull { byId[it] })
                }
                is ApiResult.Error -> {
                    history_PRG_loading.visibility = View.GONE
                    showEmpty()
                }
            }
        }
    }

    private fun render(coupons: List<CouponDto>) {
        if (coupons.isEmpty()) {
            showEmpty()
            return
        }
        history_LBL_empty.visibility = View.GONE
        history_RCV_list.visibility = View.VISIBLE
        val adapter = CouponAdapter(
            coupons, R.layout.item_favorite_row,
            onFavorite = { onFavoriteClicked(it) }
        ) { onCouponClicked(it) }
        history_RCV_list.adapter = adapter
    }

    private fun onFavoriteClicked(coupon: CouponDto) {
        // The row stays in history either way; just flip the heart to the new state.
        (requireActivity() as MainActivity).toggleFavorite(coupon.discount_id.orEmpty()) {
            history_RCV_list.adapter?.notifyDataSetChanged()
        }
    }

    private fun showEmpty() {
        history_RCV_list.visibility = View.GONE
        history_LBL_empty.visibility = View.VISIBLE
    }

    private fun onCouponClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }
}
