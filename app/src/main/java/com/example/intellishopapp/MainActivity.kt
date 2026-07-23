package com.example.intellishopapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.repository.FavoriteRepository
import com.example.intellishopapp.ui.CouponDetailFragment
import com.example.intellishopapp.ui.FavoritesFragment
import com.example.intellishopapp.ui.HomeFragment
import com.example.intellishopapp.ui.LoginFragment
import com.example.intellishopapp.ui.ProfileFragment
import com.example.intellishopapp.ui.RegisterFragment
import com.example.intellishopapp.ui.SearchFragment
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.example.intellishopapp.utilities.SignalManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * The app shell: a top bar with the burger menu, a content frame that hosts the
 * Home / Coupons / Profile fragments, and a custom bottom tab bar. Fragments are
 * added once and shown/hidden to preserve their state across tab switches.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var main_BTN_burger: ImageButton
    private lateinit var main_LAY_search: LinearLayout
    private lateinit var main_LAY_tabHome: LinearLayout
    private lateinit var main_LAY_tabCoupons: LinearLayout
    private lateinit var main_LAY_tabProfile: LinearLayout
    private lateinit var main_IMG_tabHome: ImageView
    private lateinit var main_IMG_tabCoupons: ImageView
    private lateinit var main_IMG_tabProfile: ImageView
    private lateinit var main_LBL_tabHome: MaterialTextView
    private lateinit var main_LBL_tabCoupons: MaterialTextView
    private lateinit var main_LBL_tabProfile: MaterialTextView
    private lateinit var main_LBL_banner: MaterialTextView

    private lateinit var homeFragment: Fragment
    private lateinit var favoritesFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    private enum class Tab { HOME, COUPONS, PROFILE }
    private var currentTab = Tab.HOME
    private val favoriteRepository = FavoriteRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_LAY_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViews()
        initViews()
    }

    private fun findViews() {
        main_BTN_burger = findViewById(R.id.main_BTN_burger)
        main_LAY_search = findViewById(R.id.main_LAY_search)
        main_LAY_tabHome = findViewById(R.id.main_LAY_tabHome)
        main_LAY_tabCoupons = findViewById(R.id.main_LAY_tabCoupons)
        main_LAY_tabProfile = findViewById(R.id.main_LAY_tabProfile)
        main_IMG_tabHome = findViewById(R.id.main_IMG_tabHome)
        main_IMG_tabCoupons = findViewById(R.id.main_IMG_tabCoupons)
        main_IMG_tabProfile = findViewById(R.id.main_IMG_tabProfile)
        main_LBL_tabHome = findViewById(R.id.main_LBL_tabHome)
        main_LBL_tabCoupons = findViewById(R.id.main_LBL_tabCoupons)
        main_LBL_tabProfile = findViewById(R.id.main_LBL_tabProfile)
        main_LBL_banner = findViewById(R.id.main_LBL_banner)
    }

    private fun initViews() {
        val fm = supportFragmentManager
        homeFragment = fm.findFragmentByTag(TAG_HOME) ?: HomeFragment()
        favoritesFragment = fm.findFragmentByTag(TAG_COUPONS) ?: FavoritesFragment()
        profileFragment = fm.findFragmentByTag(TAG_PROFILE) ?: ProfileFragment()

        if (fm.findFragmentByTag(TAG_HOME) == null) {
            fm.beginTransaction()
                .add(R.id.main_FRAME_content, profileFragment, TAG_PROFILE).hide(profileFragment)
                .add(R.id.main_FRAME_content, favoritesFragment, TAG_COUPONS).hide(favoritesFragment)
                .add(R.id.main_FRAME_content, homeFragment, TAG_HOME)
                .commit()
        } else {
            // Recreated (e.g. rotation): normalise to Home.
            fm.beginTransaction().hide(favoritesFragment).hide(profileFragment).show(homeFragment).commit()
        }
        activeFragment = homeFragment
        updateTabColors(Tab.HOME)

        main_LAY_tabHome.setOnClickListener { selectTab(Tab.HOME) }
        main_LAY_tabCoupons.setOnClickListener { selectTab(Tab.COUPONS) }
        main_LAY_tabProfile.setOnClickListener { selectTab(Tab.PROFILE) }
        main_BTN_burger.setOnClickListener { showMenu() }
        main_LAY_search.setOnClickListener { showSearch() }

        // Keep the search bar correct whenever an auth overlay is pushed or popped
        // (e.g. system back leaving Login restores it on Home).
        supportFragmentManager.addOnBackStackChangedListener { refreshSearchBar() }
    }

    /** Any overlay layered over the tabs (Login, Register, or a Coupon Detail). */
    private fun hasOverlay(): Boolean = supportFragmentManager.backStackEntryCount > 0

    /** Search bar belongs to Home only, and never over an overlay. */
    private fun refreshSearchBar() {
        main_LAY_search.visibility =
            if (currentTab == Tab.HOME && !hasOverlay()) View.VISIBLE else View.GONE
    }

    private val hideBannerRunnable = Runnable {
        main_LBL_banner.animate().alpha(0f).setDuration(180).withEndAction {
            main_LBL_banner.visibility = View.GONE
        }.start()
    }

    /**
     * Shows the top banner. Short (~2s) for happy-flow messages, long (~4s) for
     * sign-in prompts. Light green, fades in and auto-hides.
     */
    fun showBanner(message: String, longDuration: Boolean = false) {
        main_LBL_banner.text = message
        main_LBL_banner.visibility = View.VISIBLE
        main_LBL_banner.alpha = 0f
        main_LBL_banner.animate().alpha(1f).setDuration(180).start()
        main_LBL_banner.removeCallbacks(hideBannerRunnable)
        main_LBL_banner.postDelayed(hideBannerRunnable, if (longDuration) 4000L else 2000L)
    }

    /**
     * Shows the Login screen inside the shell, layered over the current content
     * (its opaque background covers it). Dismissing pops it off cleanly.
     */
    fun showLogin() {
        if (supportFragmentManager.findFragmentByTag(TAG_LOGIN) != null) return
        main_LAY_search.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, LoginFragment(), TAG_LOGIN)
            .addToBackStack(TAG_LOGIN)
            .commit()
    }

    /** Shows Register layered over the content. args carry the Google prefill. */
    fun showRegister(args: Bundle?) {
        if (supportFragmentManager.findFragmentByTag(TAG_REGISTER) != null) return
        main_LAY_search.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, RegisterFragment().apply { arguments = args }, TAG_REGISTER)
            .addToBackStack(TAG_REGISTER)
            .commit()
    }

    /** Opens the Search screen over the content (its own field; bottom menu stays). */
    fun showSearch() {
        if (supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null) return
        main_LAY_search.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, SearchFragment(), TAG_SEARCH)
            .addToBackStack(TAG_SEARCH)
            .commit()
    }

    /** Clears any auth overlays (login/register) — used after a successful sign-in. */
    fun dismissAuth() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    /**
     * Opens the Coupon Details sheet over the content (the fragment handles its own
     * slide up/down). Openable by everyone; its actions gate guests to Login.
     */
    fun showCouponDetail(coupon: com.example.intellishopapp.model.dto.CouponDto) {
        if (supportFragmentManager.findFragmentByTag(TAG_DETAIL) != null) return
        main_LAY_search.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, CouponDetailFragment.newInstance(coupon), TAG_DETAIL)
            .addToBackStack(TAG_DETAIL)
            .commit()
    }

    /** Jump to the Coupons tab (used by the Profile shortcut). */
    fun openCoupons() = selectTab(Tab.COUPONS)

    /**
     * Toggle a coupon's favorite (from a card heart or the detail sheet). Guests get
     * a sign-in notification only (no redirect); members write through to the backend,
     * mirror the local set, show the result banner, and report the new state.
     */
    fun toggleFavorite(discountId: String, onResult: (nowFavorite: Boolean) -> Unit = {}) {
        if (discountId.isBlank()) return
        val session = SessionManager.getInstance()
        if (!session.isLoggedIn()) {
            showBanner(getString(R.string.gate_save), longDuration = true)
            SignalManager.getInstance().vibrate()
            return
        }
        val currentlyFav = session.isFavorite(discountId)
        lifecycleScope.launch {
            val result =
                if (currentlyFav) favoriteRepository.remove(discountId)
                else favoriteRepository.add(discountId)
            when (result) {
                is ApiResult.Success -> {
                    if (currentlyFav) {
                        session.removeFavorite(discountId)
                        showBanner(getString(R.string.detail_removed))
                    } else {
                        session.addFavorite(discountId)
                        showBanner(getString(R.string.detail_saved))
                    }
                    onResult(!currentlyFav)
                }
                is ApiResult.Error -> showBanner(getString(R.string.detail_update_failed))
            }
        }
    }

    /** Sign out: drop the session + cookies and return to the guest Home state. */
    fun signOut() {
        SessionManager.getInstance().clear()
        com.example.intellishopapp.network.RetrofitClient.getInstance().clearCookies()
        selectTab(Tab.HOME)
        showBanner(getString(R.string.signed_out))
    }

    private fun selectTab(tab: Tab) {
        // A tab press always leaves any overlay (Login/Register/Detail) behind so the
        // chosen page is actually shown instead of staying pinned under it.
        if (hasOverlay()) {
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        // Profile requires an account; a guest is sent to Login instead.
        if (tab == Tab.PROFILE && !SessionManager.getInstance().isLoggedIn()) {
            showLogin()
            return
        }
        val target = when (tab) {
            Tab.HOME -> homeFragment
            Tab.COUPONS -> favoritesFragment
            Tab.PROFILE -> profileFragment
        }
        if (target !== activeFragment) {
            supportFragmentManager.beginTransaction().hide(activeFragment).show(target).commit()
            activeFragment = target
        }
        currentTab = tab
        updateTabColors(tab)
    }

    private fun updateTabColors(tab: Tab) {
        refreshSearchBar()
        val selected = ContextCompat.getColor(this, R.color.brand_primary)
        val normal = ContextCompat.getColor(this, R.color.text_secondary)
        applyTab(main_IMG_tabHome, main_LBL_tabHome, if (tab == Tab.HOME) selected else normal)
        applyTab(main_IMG_tabCoupons, main_LBL_tabCoupons, if (tab == Tab.COUPONS) selected else normal)
        applyTab(main_IMG_tabProfile, main_LBL_tabProfile, if (tab == Tab.PROFILE) selected else normal)
    }

    private fun applyTab(icon: ImageView, label: MaterialTextView, color: Int) {
        icon.setColorFilter(color)
        label.setTextColor(color)
    }

    private fun showMenu() {
        val popup = PopupMenu(this, main_BTN_burger)
        popup.menu.add(0, MENU_SIGN_IN, 0, getString(R.string.menu_sign_in))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_SIGN_IN -> {
                    showLogin()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_COUPONS = "coupons"
        private const val TAG_PROFILE = "profile"
        private const val TAG_LOGIN = "login"
        private const val TAG_REGISTER = "register"
        private const val TAG_DETAIL = "detail"
        private const val TAG_SEARCH = "search"
        private const val MENU_SIGN_IN = 1
    }
}
