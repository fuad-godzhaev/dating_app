package com.aura.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Like (
    @SerialName("\$type")
    val type: String = "com.aura.records.like",
    val subject: String,
    val createdAt: String
    ) {
}