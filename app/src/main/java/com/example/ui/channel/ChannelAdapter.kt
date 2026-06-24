package com.example.ui.channel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.R
import com.example.data.model.Channel
import com.example.databinding.ItemChannelBinding

class ChannelAdapter(
    private var channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private var activeChannelId: String? = null

    fun updateData(newChannels: List<Channel>) {
        channels = newChannels
        notifyDataSetChanged()
    }

    fun setActiveChannel(channelId: String?) {
        activeChannelId = channelId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    override fun getItemCount(): Int = channels.size

    inner class ChannelViewHolder(private val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.channelName.text = channel.name
            binding.channelGroup.text = channel.group

            // Load logo using Glide
            if (channel.logoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(channel.logoUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(binding.channelLogo)
            } else {
                binding.channelLogo.setImageResource(R.mipmap.ic_launcher_round)
            }

            // Check active state
            val isActive = channel.id == activeChannelId
            if (isActive) {
                binding.imgActiveIndicator.visibility = View.VISIBLE
                binding.channelName.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_cyan))
                binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary_cyan)
            } else {
                binding.imgActiveIndicator.visibility = View.GONE
                binding.channelName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_white))
                binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.border_color)
            }

            // Set click listener
            binding.root.setOnClickListener {
                onChannelClick(channel)
            }

            // D-Pad focus changes (scale 1.0 -> 1.05) and premium visual highlighting
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.root.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                    binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary_cyan)
                    binding.cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.highlight_purple))
                } else {
                    binding.root.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    if (isActive) {
                        binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary_cyan)
                    } else {
                        binding.cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.border_color)
                    }
                    binding.cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_bg))
                }
            }
        }
    }
}
