package com.example.intellishopapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.intellishopapp.ui.CouponHistoryFragment
import com.example.intellishopapp.ui.FavoritesFragment
import com.example.intellishopapp.ui.HomeFragment
import com.example.intellishopapp.ui.LoginFragment
import com.example.intellishopapp.ui.PreferencesFragment
import com.example.intellishopapp.ui.ProfileFragment
import com.example.intellishopapp.ui.RegisterFragment
import com.example.intellishopapp.ui.SearchFragment
import com.example.intellishopapp.ui.SentOffersFragment
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.example.intellishopapp.utilities.SignalManager
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * The app shell: a top bar with the search bar, a content frame that hosts the
 * Home / Coupons / Profile fragments, and a custom bottom tab bar. Fragments are
 * added once and shown/hidden to preserve their state across tab switches.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var main_LAY_topBar: LinearLayout
    private lateinit var main_ET_search: android.widget.EditText
    private lateinit var main_BTN_ai: com.google.android.material.button.MaterialButton
    private lateinit var main_BTN_search: com.google.android.material.button.MaterialButton
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
    private var restoredTab: Tab? = null
    private val favoriteRepository = FavoriteRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Survive the activity recreate that a Day/Night switch triggers, so the user
        // stays on the tab they were on (e.g. Profile) instead of dropping to Home.
        restoredTab = savedInstanceState?.getInt(STATE_TAB, -1)
            ?.takeIf { it in Tab.entries.indices }
            ?.let { Tab.entries[it] }
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
        main_LAY_topBar = findViewById(R.id.main_LAY_topBar)
        main_ET_search = findViewById(R.id.main_ET_search)
        main_BTN_ai = findViewById(R.id.main_BTN_ai)
        main_BTN_search = findViewById(R.id.main_BTN_search)
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
            activeFragment = homeFragment
            currentTab = Tab.HOME
        } else {
            // Recreated (e.g. a Day/Night switch): restore the tab we were on.
            val start = restoredTab ?: Tab.HOME
            val target = fragmentForTab(start)
            fm.beginTransaction()
                .hide(homeFragment).hide(favoritesFragment).hide(profileFragment)
                .show(target)
                .commit()
            activeFragment = target
            currentTab = start
        }
        paintTabs()

        main_LAY_tabHome.setOnClickListener { selectTab(Tab.HOME) }
        main_LAY_tabCoupons.setOnClickListener { selectTab(Tab.COUPONS) }
        main_LAY_tabProfile.setOnClickListener { selectTab(Tab.PROFILE) }
        // Tapping the search field opens the Search page (showing the filters).
        main_ET_search.setOnClickListener { openSearchWithFilters() }
        main_ET_search.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) openSearchWithFilters() }
        main_ET_search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                triggerSearch(ai = false); true
            } else false
        }
        main_BTN_search.setOnClickListener { triggerSearch(ai = false) }
        main_BTN_ai.setOnClickListener { triggerSearch(ai = true) }

        initBannerSwipe()

        // Keep the top bar and the tab highlight correct whenever an overlay is
        // pushed or popped (e.g. the Login overlay a guest opens from Profile).
        supportFragmentManager.addOnBackStackChangedListener { paintTabs() }
    }

    /** Any overlay layered over the tabs (Login, Register, Detail, Search, Prefs). */
    private fun hasOverlay(): Boolean = supportFragmentManager.backStackEntryCount > 0

    private fun isSearchOpen(): Boolean = supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null

    /** The sign-in / sign-up overlay a guest reaches from the Profile tab. */
    private fun isAuthOpen(): Boolean =
        supportFragmentManager.findFragmentByTag(TAG_LOGIN) != null ||
            supportFragmentManager.findFragmentByTag(TAG_REGISTER) != null

    /** The search top bar shows on Home and on the Search page (static), else hidden. */
    private fun refreshTopBar() {
        main_LAY_topBar.visibility =
            if ((currentTab == Tab.HOME && !hasOverlay()) || isSearchOpen()) View.VISIBLE else View.GONE
    }

    fun searchQueryText(): String = main_ET_search.text?.toString().orEmpty()

    private var pendingSearchAi: Boolean? = null

    fun consumePendingSearch(): Boolean? {
        val v = pendingSearchAi
        pendingSearchAi = null
        return v
    }

    private fun openSearchWithFilters() {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH) as? SearchFragment
        if (fragment != null) fragment.showFilters() else showSearch()
    }

    /** Run a search from the top bar (AI or simple), opening the Search page if needed. */
    private fun triggerSearch(ai: Boolean) {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH) as? SearchFragment
        if (fragment != null) {
            fragment.runShellSearch(ai)
        } else {
            pendingSearchAi = ai
            showSearch()
        }
    }

    /** How far the banner travels when sliding in from / out to the top. */
    private val bannerSlide by lazy { 150f * resources.displayMetrics.density }
    private var bannerDragStartY = 0f

    private val hideBannerRunnable = Runnable { hideBanner() }

    // When true, tapping the current banner opens the Login page instead of just
    // dismissing it (used by the guest sign-in prompts).
    private var bannerOpensLogin = false

    /**
     * Shows the notification in the upper area: it slides down into place, waits
     * (~2s happy flow / ~4s sign-in prompts), then slides back up. The user can
     * also swipe it up to dismiss it early. When [opensLogin] is set, the "Sign-Up"
     * word is emphasised and tapping the banner goes to the Login page.
     */
    fun showBanner(message: String, longDuration: Boolean = false, opensLogin: Boolean = false) {
        // Muted from Profile > Settings > Notifications.
        if (!SessionManager.getInstance().isNotificationsEnabled()) return
        bannerOpensLogin = opensLogin
        main_LBL_banner.removeCallbacks(hideBannerRunnable)
        main_LBL_banner.animate().cancel()
        main_LBL_banner.text = if (opensLogin) emphasizeSignUp(message) else message
        main_LBL_banner.visibility = View.VISIBLE
        main_LBL_banner.alpha = 0f
        main_LBL_banner.translationY = -bannerSlide
        main_LBL_banner.animate().translationY(0f).alpha(1f).setDuration(260).start()
        main_LBL_banner.postDelayed(hideBannerRunnable, if (longDuration) 4000L else 2000L)
    }

    /** Bold + underline the "Sign-Up" word inside a gate message. */
    private fun emphasizeSignUp(message: String): CharSequence {
        val word = getString(R.string.gate_word)
        val start = message.indexOf(word)
        if (start < 0) return message
        val span = android.text.SpannableString(message)
        val end = start + word.length
        span.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, 0)
        span.setSpan(android.text.style.UnderlineSpan(), start, end, 0)
        return span
    }

    /** Slides the notification back up and out. */
    private fun hideBanner() {
        bannerOpensLogin = false
        main_LBL_banner.removeCallbacks(hideBannerRunnable)
        main_LBL_banner.animate().cancel()
        main_LBL_banner.animate()
            .translationY(-bannerSlide)
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                main_LBL_banner.visibility = View.GONE
                main_LBL_banner.translationY = 0f
            }
            .start()
    }

    /** Swipe the notification upward to dismiss it early. */
    private fun initBannerSwipe() {
        main_LBL_banner.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    bannerDragStartY = event.rawY
                    main_LBL_banner.removeCallbacks(hideBannerRunnable)
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - bannerDragStartY
                    if (dy < 0) main_LBL_banner.translationY = dy
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val dy = event.rawY - bannerDragStartY
                    val density = resources.displayMetrics.density
                    when {
                        // Swiped up -> dismiss.
                        dy < -40f * density -> hideBanner()
                        // A plain tap: gate banners go to Login; others just dismiss.
                        kotlin.math.abs(dy) < 8f * density -> {
                            val toLogin = bannerOpensLogin
                            hideBanner()
                            if (toLogin) selectTab(Tab.PROFILE)
                        }
                        else -> {
                            main_LBL_banner.animate().translationY(0f).setDuration(150).start()
                            main_LBL_banner.postDelayed(hideBannerRunnable, 1500L)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Shows the Login screen inside the shell, layered over the current content
     * (its opaque background covers it). Dismissing pops it off cleanly.
     */
    fun showLogin() {
        if (supportFragmentManager.findFragmentByTag(TAG_LOGIN) != null) return
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, LoginFragment(), TAG_LOGIN)
            .addToBackStack(TAG_LOGIN)
            .commit()
    }

    /** Shows Register layered over the content. args carry the Google prefill. */
    fun showRegister(args: Bundle?) {
        if (supportFragmentManager.findFragmentByTag(TAG_REGISTER) != null) return
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, RegisterFragment().apply { arguments = args }, TAG_REGISTER)
            .addToBackStack(TAG_REGISTER)
            .commit()
    }

    /** Opens the Search page body (the static top bar drives it and stays visible). */
    fun showSearch() {
        if (supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null) return
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
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, CouponDetailFragment.newInstance(coupon), TAG_DETAIL)
            .addToBackStack(TAG_DETAIL)
            .commit()
    }

    /** Jump to the Coupons tab (used by the Profile shortcut). */
    fun openCoupons() = selectTab(Tab.COUPONS)

    /**
     * Celebratory fireworks rising from the bottom. Lives in the shell overlay so it
     * keeps playing while the screen underneath changes (register -> login).
     */
    fun playFireworks() {
        val fx = findViewById<android.widget.FrameLayout>(R.id.main_LAY_fx) ?: return
        val colors = listOf(
            0xFFFFC107.toInt(), 0xFFFF5252.toInt(), 0xFF69F0AE.toInt(),
            0xFF40C4FF.toInt(), 0xFFE040FB.toInt(), 0xFFFFFFFF.toInt()
        )
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        repeat(30) { i ->
            val size = (10..24).random()
            val spark = View(this)
            spark.setBackgroundResource(R.drawable.bg_spark)
            spark.backgroundTintList =
                android.content.res.ColorStateList.valueOf(colors.random())
            val params = android.widget.FrameLayout.LayoutParams(size, size)
            params.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            params.leftMargin = (width * 0.08 + Math.random() * width * 0.84).toInt()
            spark.layoutParams = params
            fx.addView(spark)
            spark.animate()
                .translationY(-(height * (0.45 + Math.random() * 0.45)).toFloat())
                .translationXBy((-140..140).random().toFloat())
                .alpha(0f)
                .scaleX(1.9f)
                .scaleY(1.9f)
                .setStartDelay(i * 45L)
                .setDuration((900..1500).random().toLong())
                .withEndAction { fx.removeView(spark) }
                .start()
        }
    }

    /** Put the just-registered email into the Login form so only the password is needed. */
    fun prefillLoginEmail(email: String) {
        (supportFragmentManager.findFragmentByTag(TAG_LOGIN) as? LoginFragment)?.prefillEmail(email)
    }

    /** Opens the Coupon History page over the content (from the Profile). */
    fun showCouponHistory() {
        if (supportFragmentManager.findFragmentByTag(TAG_HISTORY) != null) return
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, CouponHistoryFragment(), TAG_HISTORY)
            .addToBackStack(TAG_HISTORY)
            .commit()
    }

    /** Opens the "Sent Offers by friends" page over the content (from the Profile). */
    fun showSentOffers() {
        if (supportFragmentManager.findFragmentByTag(TAG_SENT) != null) return
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, SentOffersFragment(), TAG_SENT)
            .addToBackStack(TAG_SENT)
            .commit()
    }

    /** Opens the local preferences/categories editor over the content. */
    fun showPreferences(type: String) {
        if (supportFragmentManager.findFragmentByTag(TAG_PREFS) != null) return
        main_LAY_topBar.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_content, PreferencesFragment.newInstance(type), TAG_PREFS)
            .addToBackStack(TAG_PREFS)
            .commit()
    }

    /**
     * Toggle a coupon's favorite (from a card heart or the detail sheet). Guests get
     * a sign-in notification only (no redirect); members write through to the backend,
     * mirror the local set, show the result banner, and report the new state.
     */
    fun toggleFavorite(discountId: String, onResult: (nowFavorite: Boolean) -> Unit = {}) {
        if (discountId.isBlank()) return
        val session = SessionManager.getInstance()
        if (!session.isLoggedIn()) {
            showBanner(getString(R.string.gate_save), longDuration = true, opensLogin = true)
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
        val target = fragmentForTab(tab)
        if (target !== activeFragment) {
            supportFragmentManager.beginTransaction().hide(activeFragment).show(target).commit()
            activeFragment = target
        }
        currentTab = tab
        paintTabs()
    }

    /**
     * Highlights the active tab. While a guest is on the Login / Register overlay the
     * Profile tab stays highlighted (that is where they tapped to get there).
     */
    private fun paintTabs() {
        refreshTopBar()
        val highlight = if (isAuthOpen()) Tab.PROFILE else currentTab
        val selected = ContextCompat.getColor(this, R.color.brand_primary)
        val normal = ContextCompat.getColor(this, R.color.text_secondary)
        applyTab(main_IMG_tabHome, main_LBL_tabHome, if (highlight == Tab.HOME) selected else normal)
        applyTab(main_IMG_tabCoupons, main_LBL_tabCoupons, if (highlight == Tab.COUPONS) selected else normal)
        applyTab(main_IMG_tabProfile, main_LBL_tabProfile, if (highlight == Tab.PROFILE) selected else normal)
    }

    private fun applyTab(icon: ImageView, label: MaterialTextView, color: Int) {
        icon.setColorFilter(color)
        label.setTextColor(color)
    }

    private fun fragmentForTab(tab: Tab): Fragment = when (tab) {
        Tab.HOME -> homeFragment
        Tab.COUPONS -> favoritesFragment
        Tab.PROFILE -> profileFragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, currentTab.ordinal)
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_COUPONS = "coupons"
        private const val TAG_PROFILE = "profile"
        private const val TAG_LOGIN = "login"
        private const val TAG_REGISTER = "register"
        private const val TAG_DETAIL = "detail"
        private const val TAG_SEARCH = "search"
        private const val TAG_PREFS = "prefs"
        private const val TAG_HISTORY = "history"
        private const val TAG_SENT = "sent_offers"
        private const val STATE_TAB = "state_tab"
    }
}
