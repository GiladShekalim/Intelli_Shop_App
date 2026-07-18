package com.example.intellishopapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intellishopapp.LoginActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.adapter.CouponAdapter
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.example.intellishopapp.utilities.SignalManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Guest-browsable coupon feed. Anyone can view the list; opening a coupon
 * requires an account (guests are prompted to sign up).
 */
class HomeFragment : Fragment() {

    private lateinit var home_LAY_couponList: RecyclerView
    private lateinit var home_PRG_loading: ProgressBar
    private lateinit var home_LBL_empty: MaterialTextView

    private val couponRepository = CouponRepository()
    private val adapter = CouponAdapter(emptyList()) { coupon -> onCouponClicked(coupon) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        initViews()
        loadCoupons()
    }

    private fun findViews(view: View) {
        home_LAY_couponList = view.findViewById(R.id.home_LAY_couponList)
        home_PRG_loading = view.findViewById(R.id.home_PRG_loading)
        home_LBL_empty = view.findViewById(R.id.home_LBL_empty)
    }

    private fun initViews() {
        home_LAY_couponList.layoutManager = LinearLayoutManager(requireContext())
        home_LAY_couponList.adapter = adapter
    }

    private fun loadCoupons() {
        home_PRG_loading.visibility = View.VISIBLE
        home_LBL_empty.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = couponRepository.getAllCoupons()) {
                is ApiResult.Success -> {
                    home_PRG_loading.visibility = View.GONE
                    adapter.updateItems(result.data)
                    home_LBL_empty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Error -> {
                    home_PRG_loading.visibility = View.GONE
                    home_LBL_empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun onCouponClicked(coupon: CouponDto) {
        if (SessionManager.getInstance().isLoggedIn()) {
            // The coupon detail overlay is built in a later step.
            SignalManager.getInstance().toast(getString(R.string.coupon_soon))
        } else {
            SignalManager.getInstance().signal(getString(R.string.gate_sign_up))
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }
}
