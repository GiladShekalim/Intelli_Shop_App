package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import com.example.intellishopapp.utilities.SignalManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Guest-browsable home, styled like the Figma: a featured hero row on top and a
 * horizontal "View More" row per category below. Anyone can browse; opening a
 * coupon requires an account (guests are prompted to sign up).
 */
class HomeFragment : Fragment() {

    private lateinit var home_LAY_hero: RecyclerView
    private lateinit var home_LAY_sections: LinearLayout
    private lateinit var home_PRG_loading: ProgressBar
    private lateinit var home_LBL_empty: MaterialTextView

    private val couponRepository = CouponRepository()
    private val heroAdapter = CouponAdapter(emptyList(), R.layout.item_hero_card) { onCouponClicked(it) }

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
        home_LAY_hero = view.findViewById(R.id.home_LAY_hero)
        home_LAY_sections = view.findViewById(R.id.home_LAY_sections)
        home_PRG_loading = view.findViewById(R.id.home_PRG_loading)
        home_LBL_empty = view.findViewById(R.id.home_LBL_empty)
    }

    private fun initViews() {
        home_LAY_hero.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        home_LAY_hero.adapter = heroAdapter
    }

    private fun loadCoupons() {
        home_PRG_loading.visibility = View.VISIBLE
        home_LBL_empty.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = couponRepository.getAllCoupons()) {
                is ApiResult.Success -> {
                    home_PRG_loading.visibility = View.GONE
                    if (result.data.isEmpty()) {
                        home_LBL_empty.visibility = View.VISIBLE
                    } else {
                        showCoupons(result.data)
                    }
                }
                is ApiResult.Error -> {
                    home_PRG_loading.visibility = View.GONE
                    home_LBL_empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCoupons(coupons: List<CouponDto>) {
        // Hero: the highest-value coupons.
        val featured = coupons.sortedByDescending { it.price ?: 0.0 }.take(6)
        heroAdapter.updateItems(featured)

        // A "View More" row per category that has coupons (canonical order).
        home_LAY_sections.removeAllViews()
        for (category in Constants.Categories.ALL) {
            val inCategory = coupons.filter { it.category?.contains(category) == true }
            if (inCategory.isNotEmpty()) {
                addSection(category, inCategory)
            }
        }
    }

    private fun addSection(category: String, coupons: List<CouponDto>) {
        val section = layoutInflater.inflate(R.layout.section_coupon_row, home_LAY_sections, false)
        val title = section.findViewById<MaterialTextView>(R.id.section_LBL_title)
        val viewMore = section.findViewById<MaterialTextView>(R.id.section_LBL_viewMore)
        val row = section.findViewById<RecyclerView>(R.id.section_LAY_row)

        title.text = category
        row.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        row.adapter = CouponAdapter(coupons, R.layout.item_coupon_card) { onCouponClicked(it) }
        viewMore.setOnClickListener {
            (requireActivity() as MainActivity).showBanner(getString(R.string.search_soon))
        }
        home_LAY_sections.addView(section)
    }

    private fun onCouponClicked(coupon: CouponDto) {
        val shell = requireActivity() as MainActivity
        if (SessionManager.getInstance().isLoggedIn()) {
            shell.showBanner(getString(R.string.coupon_soon))
        } else {
            shell.showBanner(getString(R.string.gate_sign_up), longDuration = true)
            SignalManager.getInstance().vibrate()
            shell.showLogin()
        }
    }
}
