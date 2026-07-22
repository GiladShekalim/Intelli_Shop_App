package com.example.intellishopapp.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.intellishopapp.MainActivity
import com.example.intellishopapp.R
import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.repository.FavoriteRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import com.example.intellishopapp.utilities.SignalManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Coupon Details as a slide-up sheet over the content: raises from the bottom on
 * open, slide it back down (or Back) to dismiss. Anyone can open it; the actions
 * (Save / Copy code / Go To Site / Go To Offer) are gated — a guest gets the
 * sign-in banner and is sent to Login.
 */
class CouponDetailFragment : Fragment() {

    private lateinit var detail_LAY_root: View
    private lateinit var detail_LAY_header: LinearLayout
    private lateinit var detail_VIEW_handle: View
    private lateinit var detail_IMG_hero: View
    private lateinit var detail_LAY_savedPill: View
    private lateinit var detail_BTN_close: ImageButton
    private lateinit var detail_BTN_favorite: ImageButton
    private lateinit var detail_LBL_store: MaterialTextView
    private lateinit var detail_LBL_discount: MaterialTextView
    private lateinit var detail_LBL_couponTitle: MaterialTextView
    private lateinit var detail_LBL_description: MaterialTextView
    private lateinit var detail_LBL_code: MaterialTextView
    private lateinit var detail_LBL_terms: MaterialTextView
    private lateinit var detail_BTN_save: MaterialButton
    private lateinit var detail_BTN_copy: MaterialButton
    private lateinit var detail_BTN_site: MaterialButton
    private lateinit var detail_BTN_offer: MaterialButton

    private val favoriteRepository = FavoriteRepository()
    private val couponId: String get() = requireArguments().getString(ARG_ID).orEmpty()

    private var dragStartY = 0f
    private var dismissing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_coupon_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        bindContent()
        setHeart(SessionManager.getInstance().isFavorite(couponId))
        initActions()
        animateUpOnEnter()
        // Back should slide the sheet down, not fall through to the shell.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = animateDownAndDismiss()
            }
        )
    }

    private fun findViews(view: View) {
        detail_LAY_root = view.findViewById(R.id.detail_LAY_root)
        detail_LAY_header = view.findViewById(R.id.detail_LAY_header)
        detail_VIEW_handle = view.findViewById(R.id.detail_VIEW_handle)
        detail_IMG_hero = view.findViewById(R.id.detail_IMG_hero)
        detail_LAY_savedPill = view.findViewById(R.id.detail_LAY_savedPill)
        detail_BTN_close = view.findViewById(R.id.detail_BTN_close)
        detail_BTN_favorite = view.findViewById(R.id.detail_BTN_favorite)
        detail_LBL_store = view.findViewById(R.id.detail_LBL_store)
        detail_LBL_discount = view.findViewById(R.id.detail_LBL_discount)
        detail_LBL_couponTitle = view.findViewById(R.id.detail_LBL_couponTitle)
        detail_LBL_description = view.findViewById(R.id.detail_LBL_description)
        detail_LBL_code = view.findViewById(R.id.detail_LBL_code)
        detail_LBL_terms = view.findViewById(R.id.detail_LBL_terms)
        detail_BTN_save = view.findViewById(R.id.detail_BTN_save)
        detail_BTN_copy = view.findViewById(R.id.detail_BTN_copy)
        detail_BTN_site = view.findViewById(R.id.detail_BTN_site)
        detail_BTN_offer = view.findViewById(R.id.detail_BTN_offer)
    }

    private fun bindContent() {
        val args = requireArguments()
        detail_LBL_store.text = args.getString(ARG_STORE).orEmpty()
        detail_LBL_discount.text = args.getString(ARG_DISCOUNT).orEmpty()
        detail_LBL_couponTitle.text = args.getString(ARG_TITLE).orEmpty()

        detail_LBL_description.text =
            args.getString(ARG_DESC)?.takeIf { it.isNotBlank() }
                ?: getString(R.string.detail_no_description)
        detail_LBL_code.text =
            args.getString(ARG_CODE)?.takeIf { it.isNotBlank() }
                ?: getString(R.string.detail_no_code)
        detail_LBL_terms.text = termsText(args)

        Glide.with(this)
            .load(args.getString(ARG_IMAGE))
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(detail_IMG_hero as android.widget.ImageView)
    }

    /** Terms body: conditions plus a "Valid until" line; a fallback if neither. */
    private fun termsText(args: Bundle): String {
        val terms = args.getString(ARG_TERMS)?.takeIf { it.isNotBlank() }
        val valid = args.getString(ARG_VALID)?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.detail_valid_until, it) }
        return listOfNotNull(terms, valid).joinToString("\n\n")
            .ifBlank { getString(R.string.detail_no_terms) }
    }

    private fun initActions() {
        detail_BTN_close.setOnClickListener { animateDownAndDismiss() }

        detail_BTN_save.setOnClickListener { gated(R.string.gate_save) { toggleFavorite() } }
        detail_BTN_favorite.setOnClickListener { gated(R.string.gate_save) { toggleFavorite() } }
        detail_BTN_copy.setOnClickListener { gated(R.string.gate_copy) { copyCode() } }
        detail_BTN_site.setOnClickListener { gated(R.string.gate_site) { openLink(ARG_SITE) } }
        detail_BTN_offer.setOnClickListener { gated(R.string.gate_offer) { openLink(ARG_OFFER) } }

        val dragListener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - dragStartY
                    if (dy > 0) detail_LAY_root.translationY = dy
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dy = event.rawY - dragStartY
                    if (dy > dismissThreshold()) {
                        animateDownAndDismiss()
                    } else {
                        detail_LAY_root.animate().translationY(0f).setDuration(160).start()
                    }
                    true
                }
                else -> false
            }
        }
        detail_VIEW_handle.setOnTouchListener(dragListener)
        detail_LAY_header.setOnTouchListener(dragListener)
    }

    private fun dismissThreshold(): Float = 160f * resources.displayMetrics.density

    /** Guests get the sign-in banner + a nudge to Login; members run the action. */
    private fun gated(gateRes: Int, action: () -> Unit) {
        val shell = requireActivity() as MainActivity
        if (SessionManager.getInstance().isLoggedIn()) {
            action()
        } else {
            shell.showBanner(getString(gateRes), longDuration = true)
            SignalManager.getInstance().vibrate()
            shell.showLogin()
        }
    }

    /** Toggle save/unsave: write through to the backend, then mirror locally. */
    private fun toggleFavorite() {
        val id = couponId
        val session = SessionManager.getInstance()
        val currentlyFav = session.isFavorite(id)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = if (currentlyFav) favoriteRepository.remove(id) else favoriteRepository.add(id)
            val shell = activity as? MainActivity ?: return@launch
            when (result) {
                is ApiResult.Success -> if (currentlyFav) {
                    session.removeFavorite(id)
                    setHeart(false)
                    shell.showBanner(getString(R.string.detail_removed))
                } else {
                    session.addFavorite(id)
                    setHeart(true)
                    showSavedPill()
                    shell.showBanner(getString(R.string.detail_saved))
                }
                is ApiResult.Error -> shell.showBanner(getString(R.string.detail_update_failed))
            }
        }
    }

    private fun setHeart(isFavorite: Boolean) {
        detail_BTN_favorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
    }

    private fun showSavedPill() {
        detail_LAY_savedPill.visibility = View.VISIBLE
        detail_LAY_savedPill.postDelayed({
            if (isAdded) detail_LAY_savedPill.visibility = View.GONE
        }, 1500)
    }

    private fun copyCode() {
        val code = requireArguments().getString(ARG_CODE).orEmpty()
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("coupon_code", code))
        (requireActivity() as MainActivity).showBanner(getString(R.string.detail_code_copied))
    }

    private fun openLink(argKey: String) {
        val shell = requireActivity() as MainActivity
        val url = requireArguments().getString(argKey)?.takeIf { it.isNotBlank() }
        if (url == null) {
            shell.showBanner(getString(R.string.detail_link_missing))
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            shell.showBanner(getString(R.string.detail_link_missing))
        }
    }

    private fun animateUpOnEnter() {
        detail_LAY_root.post {
            detail_LAY_root.translationY = detail_LAY_root.height.toFloat()
            detail_LAY_root.animate().translationY(0f).setDuration(260).start()
        }
    }

    private fun animateDownAndDismiss() {
        if (dismissing) return
        dismissing = true
        detail_LAY_root.animate()
            .translationY(detail_LAY_root.height.toFloat())
            .setDuration(220)
            .withEndAction {
                if (isAdded) parentFragmentManager.popBackStack()
            }
            .start()
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_IMAGE = "image"
        private const val ARG_STORE = "store"
        private const val ARG_DISCOUNT = "discount"
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "description"
        private const val ARG_CODE = "code"
        private const val ARG_TERMS = "terms"
        private const val ARG_VALID = "valid"
        private const val ARG_SITE = "site"
        private const val ARG_OFFER = "offer"

        fun newInstance(coupon: CouponDto): CouponDetailFragment =
            CouponDetailFragment().apply {
                arguments = bundleOf(
                    ARG_ID to coupon.discount_id,
                    ARG_IMAGE to coupon.image_link,
                    ARG_STORE to CouponFormatter.storeName(coupon),
                    ARG_DISCOUNT to CouponFormatter.priceLabel(coupon),
                    ARG_TITLE to CouponFormatter.title(coupon),
                    ARG_DESC to coupon.description,
                    ARG_CODE to coupon.coupon_code,
                    ARG_TERMS to coupon.terms_and_conditions,
                    ARG_VALID to coupon.valid_until,
                    ARG_SITE to coupon.provider_link,
                    ARG_OFFER to coupon.discount_link
                )
            }
    }
}
