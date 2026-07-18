package com.example.intellishopapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.example.intellishopapp.utilities.SignalManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.intellishopapp.ui.FavoritesFragment
import com.example.intellishopapp.ui.HomeFragment
import com.example.intellishopapp.ui.ProfileFragment
import com.google.android.material.textview.MaterialTextView

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

    private lateinit var homeFragment: Fragment
    private lateinit var favoritesFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    private enum class Tab { HOME, COUPONS, PROFILE }

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
        main_LAY_search.setOnClickListener {
            SignalManager.getInstance().toast(getString(R.string.search_soon))
        }
    }

    private fun selectTab(tab: Tab) {
        val target = when (tab) {
            Tab.HOME -> homeFragment
            Tab.COUPONS -> favoritesFragment
            Tab.PROFILE -> profileFragment
        }
        if (target !== activeFragment) {
            supportFragmentManager.beginTransaction().hide(activeFragment).show(target).commit()
            activeFragment = target
        }
        updateTabColors(tab)
    }

    private fun updateTabColors(tab: Tab) {
        // Search bar belongs to Home (per the Figma); hidden on other tabs.
        main_LAY_search.visibility = if (tab == Tab.HOME) View.VISIBLE else View.GONE
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
                    startActivity(Intent(this, LoginActivity::class.java))
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
        private const val MENU_SIGN_IN = 1
    }
}
