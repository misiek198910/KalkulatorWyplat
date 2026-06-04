package com.example.kalkulatorwyplat.data

data class SalaryResult(
    val type: String = "",
    val gross: Float = 0f,
    val netto: Float = 0f,
    val sEmery: Float = 0f,
    val sRent: Float = 0f,
    val sChor: Float = 0f,
    val uSpol: Float = 0f,
    val uZdro: Float = 0f,
    val kUzyPrz: Float = 0f,
    val poDoch: Float = 0f,
    val zalPoddoch: Float = 0f
)