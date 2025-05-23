package com.example.cryptick.model

data class User(
    val id: String = "",
    val email: String = "",
    val user: String = "",
    val favorites: List<String> = emptyList(),
    val image: String = "",
    val language: String = "EN",
    val theme: String = "light"
) 