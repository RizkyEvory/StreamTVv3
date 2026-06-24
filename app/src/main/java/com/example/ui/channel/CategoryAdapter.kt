package com.example.ui.channel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemCategoryBinding

class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedCategory: String = "Semua"

    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun setSelectedCategory(category: String) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryName: String) {
            binding.tvCategoryName.text = categoryName

            val isSelected = categoryName == selectedCategory

            if (isSelected) {
                binding.cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary_cyan))
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.bg_dark))
                binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary_cyan)
            } else {
                binding.cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_bg))
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_white))
                binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.border_color)
            }

            binding.root.setOnClickListener {
                onCategoryClick(categoryName)
            }

            // D-Pad focus scaling
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.root.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    if (!isSelected) {
                        binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary_cyan)
                    }
                } else {
                    binding.root.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    if (!isSelected) {
                        binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.border_color)
                    }
                }
            }
        }
    }
}
