package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.adapter.CouponAdapter
import com.example.intellishopapp.logic.SharedOffersGrouper
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.repository.ShareRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * "Sent Offers by friends": coupons other users shared to the signed-in user, one
 * section per sender (labelled with the sender's username), newest first. Tapping a
 * card opens the shared Coupon Detail sheet. Reachable only from the Profile.
 */
class SentOffersFragment : Fragment() {

    private lateinit var sent_LAY_sections: LinearLayout
    private lateinit var sent_LBL_empty: MaterialTextView
    private lateinit var sent_PRG_loading: ProgressBar
    private lateinit var sent_BTN_close: ImageButton

    private val couponRepository = CouponRepository()
    private val shareRepository = ShareRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sent_offers, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sent_LAY_sections = view.findViewById(R.id.sent_LAY_sections)
        sent_LBL_empty = view.findViewById(R.id.sent_LBL_empty)
        sent_PRG_loading = view.findViewById(R.id.sent_PRG_loading)
        sent_BTN_close = view.findViewById(R.id.sent_BTN_close)
        sent_BTN_close.setOnClickListener { parentFragmentManager.popBackStack() }
        load()
    }

    private fun load() {
        sent_PRG_loading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            // Refresh the mirror from the backend; keep the local copy on error/401.
            (shareRepository.getReceived() as? ApiResult.Success)?.let {
                SessionManager.getInstance().setReceivedShares(it.data)
            }
            val shares = SessionManager.getInstance().getReceivedShares()
            if (shares.isEmpty()) {
                sent_PRG_loading.visibility = View.GONE
                showEmpty()
                return@launch
            }
            when (val result = couponRepository.getAllCoupons()) {
                is ApiResult.Success -> {
                    sent_PRG_loading.visibility = View.GONE
                    val byId = result.data.associateBy { it.discount_id }
                    render(SharedOffersGrouper.group(shares, byId))
                }
                is ApiResult.Error -> {
                    sent_PRG_loading.visibility = View.GONE
                    showEmpty()
                }
            }
        }
    }

    private fun render(sections: List<SharedOffersGrouper.SenderSection>) {
        if (sections.isEmpty()) {
            showEmpty()
            return
        }
        sent_LBL_empty.visibility = View.GONE
        sent_LAY_sections.visibility = View.VISIBLE
        sent_LAY_sections.removeAllViews()
        for (section in sections) {
            val row = layoutInflater.inflate(R.layout.section_coupon_row, sent_LAY_sections, false)
            row.findViewById<MaterialTextView>(R.id.section_LBL_title).text = section.sender
            val rcv = row.findViewById<RecyclerView>(R.id.section_LAY_row)
            rcv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            rcv.adapter = CouponAdapter(
                section.coupons, R.layout.item_coupon_card,
                onLongClick = { confirmRemove(section.sender, it) }
            ) { onCouponClicked(it) }
            sent_LAY_sections.addView(row)
        }
    }

    /** Long-press a shared card to dismiss it (removes it from the backend + reloads). */
    private fun confirmRemove(sender: String, coupon: com.example.intellishopapp.model.dto.CouponDto) {
        val discountId = coupon.discount_id ?: return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.sent_remove_title)
            .setMessage(getString(R.string.sent_remove_message, sender))
            .setPositiveButton(R.string.sent_remove_confirm) { d, _ ->
                d.dismiss()
                removeShare(sender, discountId)
            }
            .setNegativeButton(R.string.pw_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun removeShare(sender: String, discountId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (shareRepository.removeShare(sender, discountId)) {
                is ApiResult.Success -> {
                    (requireActivity() as MainActivity).showBanner(getString(R.string.sent_removed))
                    load()
                }
                is ApiResult.Error ->
                    (requireActivity() as MainActivity).showBanner(getString(R.string.sent_remove_failed))
            }
        }
    }

    private fun showEmpty() {
        sent_LAY_sections.visibility = View.GONE
        sent_LBL_empty.visibility = View.VISIBLE
    }

    private fun onCouponClicked(coupon: com.example.intellishopapp.model.dto.CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }
}
