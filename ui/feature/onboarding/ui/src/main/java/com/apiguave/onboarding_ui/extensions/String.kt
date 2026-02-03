package com.apiguave.auth_ui.extensions

fun String.isValidUsername(): Boolean {
    // Allow letters, spaces, apostrophes, hyphens, and periods for real names
    // Examples: "John Smith", "O'Brien", "Jean-Paul", "Dr. Martinez"
    return this.length in 2..50 &&
           this.isNotBlank() &&
           this.all { it.isLetter() || it.isWhitespace() || it in listOf('\'', '-', '.') } &&
           this.trim().isNotEmpty()
}