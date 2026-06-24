package com.example.data.model

import java.io.Serializable

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val group: String
) : Serializable
