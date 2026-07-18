package com.example.intellishopapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.intellishopapp.R
import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.model.dto.CouponDto
import com.google.android.material.textview.MaterialTextView

/**
 * Renders coupons into a given card layout (the small section card or the wide
 * hero card). Views are looked up null-safely so the same adapter serves layouts
 * that omit some fields (e.g. the hero has no store line).
 */
class CouponAdapter(
    private var items: List<CouponDto>,
    @LayoutRes private val layoutRes: Int,
    private val onClick: (CouponDto) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val logo: AppCompatImageView? = view.findViewById(R.id.item_IMG_logo)
        private val title: MaterialTextView? = view.findViewById(R.id.item_LBL_title)
        private val store: MaterialTextView? = view.findViewById(R.id.item_LBL_store)
        private val discount: MaterialTextView? = view.findViewById(R.id.item_LBL_discount)

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
            itemView.setOnClickListener { onClick(coupon) }
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
