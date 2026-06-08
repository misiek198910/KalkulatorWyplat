package com.example.kalkulatorwyplat.data

import java.util.UUID

// Podstawowy wynik dla jednego miesiąca
data class SalaryResult(
    val id: String = UUID.randomUUID().toString(),
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
    val zalPoddoch: Float = 0f,
    val ppkPracownik: Float = 0f,
    val ppkPracodawca: Float = 0f
)

// Szczegółowy wynik dla konkretnego miesiąca (np. Styczeń, Luty) w projekcji rocznej
data class MonthlyDetailedResult(
    val monthNumber: Int,
    val monthName: String, // Np. klucz do strings.xml
    val isTaxThresholdCrossed: Boolean,
    val taxRateApplied: Float,
    val accumulatedIncome: Float,
    val monthlyResult: SalaryResult
)

// Całkowite podsumowanie roczne
data class YearlySalaryResult(
    val contractType: String,
    val totalGross: Float,
    val totalNetto: Float,
    val averageNetto: Float,
    val totalZus: Float,
    val totalHealthInsurance: Float,
    val totalIncomeTax: Float,
    val crossedTaxThresholdMonth: Int?,
    val monthlyDetails: List<MonthlyDetailedResult>
)