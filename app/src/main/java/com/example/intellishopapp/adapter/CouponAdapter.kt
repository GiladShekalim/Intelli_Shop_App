package com.example.intellishopapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.intellishopapp.R
import com.example.intellishopapp.logic.CouponFormatter
import com.example.intellishopapp.model.dto.CouponDto
import com.google.android.material.textview.MaterialTextView

class CouponAdapter(
    private var items: List<CouponDto>,
    private val onClick: (CouponDto) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val item_IMG_logo: AppCompatImageView = view.findViewById(R.id.item_IMG_logo)
        private val item_LBL_title: MaterialTextView = view.findViewById(R.id.item_LBL_title)
        private val item_LBL_store: MaterialTextView = view.findViewById(R.id.item_LBL_store)
        private val item_LBL_discount: MaterialTextView = view.findViewById(R.id.item_LBL_discount)

        fun bind(coupon: CouponDto) {
            item_LBL_title.text = CouponFormatter.title(coupon)
            item_LBL_store.text = CouponFormatter.storeName(coupon)
            item_LBL_discount.text = CouponFormatter.priceLabel(coupon)
            Glide.with(item_IMG_logo)
                .load(coupon.image_link)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(item_IMG_logo)
            itemView.setOnClickListener { onClick(coupon) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coupon_card, parent, false)
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
