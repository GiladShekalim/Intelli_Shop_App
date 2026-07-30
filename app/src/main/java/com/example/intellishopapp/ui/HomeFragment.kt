package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.adapter.CouponAdapter
import com.example.intellishopapp.logic.CouponRanker
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.repository.HistoryRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.Constants
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Guest-browsable home: the personal suggestions as the hero row, then Recently
 * Viewed, Top Deals, and a horizontal row per category. Anyone can browse and open
 * a coupon; the sign-in gate lives on the actions inside the detail sheet.
 */
class HomeFragment : Fragment() {

    private lateinit var home_LBL_bestMatches: MaterialTextView
    private lateinit var home_LAY_hero: RecyclerView
    private lateinit var home_LAY_hero2: RecyclerView
    private lateinit var home_LAY_sections: LinearLayout
    private lateinit var home_PRG_loading: ProgressBar
    private lateinit var home_LBL_empty: MaterialTextView

    private val couponRepository = CouponRepository()
    private val historyRepository = HistoryRepository()
    private val heroAdapter = CouponAdapter(
        emptyList(), R.layout.item_hero_card,
        onFavorite = { onFavoriteClicked(it) }
    ) { onCouponClicked(it) }
    private val heroAdapter2 = CouponAdapter(
        emptyList(), R.layout.item_hero_card,
        onFavorite = { onFavoriteClicked(it) }
    ) { onCouponClicked(it) }

    // Every card adapter on the page, so a favorite toggle refreshes all hearts.
    private val cardAdapters = mutableListOf(heroAdapter, heroAdapter2)

    private var loadedCoupons: List<CouponDto> = emptyList()
    private var shownHistory: List<String> = emptyList()
    private var shownProfile: Pair<List<String>, List<String>> = emptyList<String>() to emptyList()

    /**
     * Popping an overlay can change what Home shows: Recently Viewed if the history
     * grew, or the personalized hero if the user edited their statuses/interests.
     * Rebuild when either moved. This fires for ANY back-stack change including during
     * teardown, so require a resumed fragment with a live context/view (isAdded can be
     * true while the context is already gone -> requireContext()/getString() crash).
     */
    private val backStackListener = FragmentManager.OnBackStackChangedListener {
        if (isResumed && view != null && context != null && loadedCoupons.isNotEmpty() &&
            (SessionManager.getInstance().getHistory() != shownHistory ||
                currentProfile() != shownProfile)
        ) {
            showCoupons(loadedCoupons)
        }
    }

    private fun currentProfile(): Pair<List<String>, List<String>> {
        val s = SessionManager.getInstance().get()
        return (s?.status.orEmpty()) to (s?.hobbies.orEmpty())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        initViews()
        parentFragmentManager.addOnBackStackChangedListener(backStackListener)
        loadCoupons()
    }

    override fun onDestroyView() {
        parentFragmentManager.removeOnBackStackChangedListener(backStackListener)
        super.onDestroyView()
    }

    private fun findViews(view: View) {
        home_LBL_bestMatches = view.findViewById(R.id.home_LBL_bestMatches)
        home_LAY_hero = view.findViewById(R.id.home_LAY_hero)
        home_LAY_hero2 = view.findViewById(R.id.home_LAY_hero2)
        home_LAY_sections = view.findViewById(R.id.home_LAY_sections)
        home_PRG_loading = view.findViewById(R.id.home_PRG_loading)
        home_LBL_empty = view.findViewById(R.id.home_LBL_empty)
    }

    private fun initViews() {
        home_LAY_hero.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        home_LAY_hero.adapter = heroAdapter
        home_LAY_hero2.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        home_LAY_hero2.adapter = heroAdapter2
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
                        // Refresh the history mirror first so Recently Viewed reflects
                        // what this user did on any device. A guest gets 401 and keeps
                        // whatever is stored locally.
                        val history = historyRepository.get()
                        if (history is ApiResult.Success) {
                            SessionManager.getInstance().setHistory(history.data)
                        }
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
        // A network coroutine can resume just after the view is torn down; showCoupons
        // touches requireContext()/getString(), so bail if we are no longer attached.
        if (!isAdded || view == null || context == null) return
        loadedCoupons = coupons
        shownProfile = currentProfile()
        val session = SessionManager.getInstance()

        // Hero: the personal suggestions, best match first, capped at 10 — the same
        // profile filter + favourites weighting the backend's index_home applies. Shown
        // as a greeting over two big-card rows of five.
        val name = session.get()?.username?.takeIf { it.isNotBlank() }
        home_LBL_bestMatches.text =
            if (name != null) getString(R.string.home_best_matches, name)
            else getString(R.string.home_best_matches_guest)

        val suggestions = CouponRanker.personalizedTop(
            coupons,
            session.favoriteIds(),
            10,
            session.get()?.status.orEmpty(),
            session.get()?.hobbies.orEmpty()
        )
        heroAdapter.updateItems(suggestions.take(5))
        val secondRow = suggestions.drop(5).take(5)
        heroAdapter2.updateItems(secondRow)
        // Nudge the second row half a card to the left so it visibly reads as
        // scrollable. Absolute (scrollToPositionWithOffset) so repeated binds don't
        // accumulate the offset.
        if (secondRow.isNotEmpty()) {
            home_LAY_hero2.post {
                val halfCard = (150 * resources.displayMetrics.density).toInt()
                (home_LAY_hero2.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(0, -halfCard)
            }
        }

        home_LAY_sections.removeAllViews()
        cardAdapters.retainAll(listOf(heroAdapter, heroAdapter2))

        // Recently viewed, newest first. Hidden until there is something to show.
        addRecentlyViewedSection(coupons)

        // Last Minute: offers closest to their expiration date, soonest first.
        addSection(getString(R.string.home_last_minute), CouponRanker.lastMinute(coupons, 10))

        // Then one row per category that has coupons (canonical order).
        for (category in Constants.Categories.ALL) {
            val inCategory = coupons.filter { it.category?.contains(category) == true }
            if (inCategory.isNotEmpty()) {
                addSection(category, inCategory)
            }
        }
    }

    /** Resolves the stored history ids against the loaded coupons, keeping history order. */
    private fun addRecentlyViewedSection(coupons: List<CouponDto>) {
        val byId = coupons.associateBy { it.discount_id }
        shownHistory = SessionManager.getInstance().getHistory()
        val viewed = shownHistory.mapNotNull { byId[it] }
        if (viewed.isNotEmpty()) {
            addSection(getString(R.string.home_recently_viewed), viewed)
        }
    }

    private fun addSection(title: String, coupons: List<CouponDto>) {
        if (coupons.isEmpty()) return
        val section = layoutInflater.inflate(R.layout.section_coupon_row, home_LAY_sections, false)
        val label = section.findViewById<MaterialTextView>(R.id.section_LBL_title)
        val row = section.findViewById<RecyclerView>(R.id.section_LAY_row)

        label.text = title
        row.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val adapter = CouponAdapter(
            coupons, R.layout.item_coupon_card,
            onFavorite = { onFavoriteClicked(it) }
        ) { onCouponClicked(it) }
        row.adapter = adapter
        cardAdapters.add(adapter)
        home_LAY_sections.addView(section)
    }

    private fun onCouponClicked(coupon: CouponDto) {
        // Details open for everyone; the gated actions live inside the sheet.
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }

    private fun onFavoriteClicked(coupon: CouponDto) {
        // Guests get a notification only; members toggle. Refresh every heart after.
        (requireActivity() as MainActivity).toggleFavorite(coupon.discount_id.orEmpty()) {
            cardAdapters.forEach { it.notifyDataSetChanged() }
        }
    }
}
