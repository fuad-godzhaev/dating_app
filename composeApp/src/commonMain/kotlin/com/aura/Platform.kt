package com.aura

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform