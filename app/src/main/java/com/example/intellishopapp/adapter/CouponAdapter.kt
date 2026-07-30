package com.example.intellishopapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.intellishopapp.R
import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.textview.MaterialTextView

/**
 * Renders coupons into a given card layout (the small section card, the wide hero
 * card, or the favorites row). Views are looked up null-safely so the same adapter
 * serves layouts that omit some fields. If [onFavorite] is set, the card's heart
 * reflects the saved state and toggles it on tap.
 */
class CouponAdapter(
    private var items: List<CouponDto>,
    @LayoutRes private val layoutRes: Int,
    private val onFavorite: ((CouponDto) -> Unit)? = null,
    private val onLongClick: ((CouponDto) -> Unit)? = null,
    private val onClick: (CouponDto) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val logo: AppCompatImageView? = view.findViewById(R.id.item_IMG_logo)
        private val title: MaterialTextView? = view.findViewById(R.id.item_LBL_title)
        private val store: MaterialTextView? = view.findViewById(R.id.item_LBL_store)
        private val discount: MaterialTextView? = view.findViewById(R.id.item_LBL_discount)
        private val favorite: ImageView? = view.findViewById(R.id.item_BTN_favorite)

        fun bind(coupon: CouponDto) {
            title?.text = CouponFormatter.title(coupon)
            store?.text = CouponFormatter.storeName(coupon)
            discount?.text = CouponFormatter.priceLabel(coupon)
            logo?.let {
                Glide.with(it)
                    .load(coupon.image_link)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(it)
            }
            favorite?.let { heart ->
                val isFav = SessionManager.getInstance().isFavorite(coupon.discount_id.orEmpty())
                heart.setImageResource(
                    if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                heart.setOnClickListener { onFavorite?.invoke(coupon) }
            }
            itemView.setOnClickListener { onClick(coupon) }
            itemView.setOnLongClickListener {
                onLongClick?.let { it(coupon); true } ?: false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CouponDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
