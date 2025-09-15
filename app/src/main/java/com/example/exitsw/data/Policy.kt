package com.example.exitsw.data

data class Policy(
    val name: String = "",
    val description: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 100,
    val region: String = "전국",
    val incomeBracket: Int = 10
)
