package com.lukasdylan.hlsvideo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lukasdylan.hlsvideo.databinding.ItemProductHeaderBinding

class ProductAdapter : RecyclerView.Adapter<ProductHeaderViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductHeaderViewHolder {
        val binding = ItemProductHeaderBinding.inflate(LayoutInflater.from(parent.context))
        return ProductHeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductHeaderViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 10

}

class ProductHeaderViewHolder(private val binding: ItemProductHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind() {
        binding.apply {
            tvTitle.text = "Test Title"
            tvSubtitle.text = "Test Subtitle"
        }
    }
}