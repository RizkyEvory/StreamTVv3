package com.example.data.model

data class ChannelGroup(
    val name: String,
    val channels: List<Channel>,
    var isSelected: Boolean = false
)
