package com.example.kalkulatorwyplat.data

data class TaxRates(
    val emerytalna: Float = 9.76f, // w procentach
    val rentowa: Float = 1.50f,
    val chorobowa: Float = 2.45f,
    val zdrowotna: Float = 9.00f,
    val pit: Float = 12.00f
)