package com.example.intellishopapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * The Coupons tab = the user's saved coupons ("My Coupons"). Favorites are the
 * local-first id set (see [SessionManager]); this filters the fetched catalog by
 * that set, so it re-renders whenever the user saves/removes elsewhere.
 */
class FavoritesFragment : Fragment() {

    private lateinit var favorites_RCV_list: RecyclerView
    private lateinit var favorites_LBL_empty: MaterialTextView
    private lateinit var favorites_PRG_loading: View

    private val couponRepository = CouponRepository()
    private val favoriteRepository = com.example.intellishopapp.repository.FavoriteRepository()
    private var catalog: List<CouponDto>? = null
    // The saved-id set currently drawn on screen. Re-opening the tab skips the rebuild
    // (and its image reloads) when this is unchanged, so switching pages doesn't flicker.
    private var renderedFavs: Set<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_favorites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favorites_RCV_list = view.findViewById(R.id.favorites_RCV_list)
        favorites_LBL_empty = view.findViewById(R.id.favorites_LBL_empty)
        favorites_PRG_loading = view.findViewById(R.id.favorites_PRG_loading)
        favorites_RCV_list.layoutManager = LinearLayoutManager(requireContext())
        // Load lazily: the fragment is created hidden at launch, so only fetch the
        // catalog the first time the Coupons tab is actually shown (or if visible now).
        if (!isHidden) loadThenRender()
    }

    /** Load on first show; refresh from the backend on later shows too. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) loadThenRender()
    }

    private fun loadThenRender() {
        // Only the very first load blanks the screen for the spinner. Later shows keep
        // whatever is already drawn and refresh quietly underneath — no reload flash.
        val firstLoad = catalog == null
        if (firstLoad) {
            favorites_PRG_loading.visibility = View.VISIBLE
            favorites_LBL_empty.visibility = View.GONE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            if (catalog == null) {
                catalog = when (val result = couponRepository.getAllCoupons()) {
                    is ApiResult.Success -> result.data
                    is ApiResult.Error -> emptyList()
                }
            }
            // Pull the authoritative favorites from the backend (keep local on 401/error).
            if (SessionManager.getInstance().isLoggedIn()) {
                (favoriteRepository.getFavorites() as? ApiResult.Success)?.let {
                    SessionManager.getInstance().setFavorites(it.data)
                }
            }
            if (view == null) return@launch
            favorites_PRG_loading.visibility = View.GONE
            render()
        }
    }

    private fun render() {
        val favs = SessionManager.getInstance().favoriteIds().toSet()
        val items = catalog.orEmpty().filter { favs.contains(it.discount_id) }
        if (items.isEmpty()) {
            favorites_RCV_list.visibility = View.GONE
            favorites_LBL_empty.visibility = View.VISIBLE
            renderedFavs = favs
            // A little cheer each time the empty tab is opened; leaving cancels it (selectTab).
            (activity as? MainActivity)?.playFireworks()
            return
        }
        favorites_LBL_empty.visibility = View.GONE
        favorites_RCV_list.visibility = View.VISIBLE
        // Nothing changed since the last draw: leave the list (and its loaded images)
        // in place instead of rebuilding it, which is what caused the flicker.
        if (favs == renderedFavs && favorites_RCV_list.adapter != null) return
        renderedFavs = favs
        favorites_RCV_list.adapter = CouponAdapter(
            items, R.layout.item_favorite_row,
            onFavorite = { onFavoriteClicked(it) }
        ) { onCouponClicked(it) }
    }

    private fun onCouponClicked(coupon: CouponDto) {
        (requireActivity() as MainActivity).showCouponDetail(coupon)
    }

    private fun onFavoriteClicked(coupon: CouponDto) {
        // On this list the heart is always filled -> tapping removes; re-render so the
        // row drops out. (Guests never reach here with saved items.)
        (requireActivity() as MainActivity).toggleFavorite(coupon.discount_id.orEmpty()) {
            render()
        }
    }
}
