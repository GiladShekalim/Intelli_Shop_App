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
 * Coupon History: coupons the user acted on (copy / go to site / go to offer),
 * most recent first. Tapping a row opens the shared Coupon Detail sheet over this
 * page (no duplicated detail code — it reuses MainActivity.showCouponDetail).
 */
class CouponHistoryFragment : Fragment() {

    private lateinit var history_RCV_list: RecyclerView
    private lateinit var history_LBL_empty: MaterialTextView
    private lateinit var history_PRG_loading: ProgressBar
    private lateinit var history_BTN_close: ImageButton

    private val couponRepository = CouponRepository()

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
        val ids = SessionManager.getInstance().getHistory()
        if (ids.isEmpty()) {
            showEmpty()
            return
        }
        history_PRG_loading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
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
        history_RCV_list.adapter = CouponAdapter(coupons, R.layout.item_favorite_row) { onCouponClicked(it) }
    }

    private fun showEmpty() {
        history_RCV_list.visibility = View.GONE
        history_LBL_empty.visibility = View.VISIBLE
    }

    private fun onCouponClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }
}
