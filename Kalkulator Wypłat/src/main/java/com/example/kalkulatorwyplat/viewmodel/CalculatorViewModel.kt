package com.example.kalkulatorwyplat.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.data.TaxRates
import com.example.kalkulatorwyplat.logic.SalaryCalculator
import com.example.kalkulatorwyplat.data.YearlySalaryResult

class CalculatorViewModel : ViewModel() {
    private val calculator = SalaryCalculator()
    var amount by mutableStateOf("")
    var isCalculatingFromNetto by mutableStateOf(false)
    var isPitExempt by mutableStateOf(false)
    var isSickLeaveEnabled by mutableStateOf(true)
    var selectedOption by mutableStateOf("Podstawowe (250 zł)")
    var isTaxFreeAmountEnabled by mutableStateOf(true)
    var isPpkEnabled by mutableStateOf(false)
    var currentTaxRates by mutableStateOf(TaxRates())
    var calculationResults by mutableStateOf<List<SalaryResult>>(emptyList())
        private set

    fun calculate() {
        val amountFloat = amount.replace(",", ".").toFloatOrNull() ?: 0f
        val stawka = when (selectedOption) {
            "Podstawowe (250 zł)" -> 0.02f
            "Podwyższone (300 zł)" -> 0.05f
            else -> 0f
        }
        val isInputBrutto = !isCalculatingFromNetto

        calculationResults = calculator.calculateAll(
            amount = amountFloat,
            isBrutto = isInputBrutto,
            stawkaKosztow = stawka,
            isPitExempt = isPitExempt,
            isSickLeaveEnabled = isSickLeaveEnabled,
            isTaxFreeAmountEnabled = isTaxFreeAmountEnabled,
            isPpkEnabled = isPpkEnabled,
            rates = currentTaxRates
        )
    }

    fun updateRate(rateType: String, newValue: String) {
        val value = newValue.replace(",", ".").toFloatOrNull() ?: return

        // Tworzymy nową kopię stawek z zaktualizowaną wartością
        currentTaxRates = when(rateType) {
            "emerytalna" -> currentTaxRates.copy(emerytalna = value)
            "rentowa" -> currentTaxRates.copy(rentowa = value)
            "chorobowa" -> currentTaxRates.copy(chorobowa = value)
            "zdrowotna" -> currentTaxRates.copy(zdrowotna = value)
            "pit" -> currentTaxRates.copy(pit = value)
            else -> currentTaxRates
        }
        calculate()
    }
    fun resetTaxRates() {
        currentTaxRates = TaxRates()
        calculate()
    }

    var yearlyCalculationResult by mutableStateOf<YearlySalaryResult?>(null)
        private set


    fun calculateYearly() {

        val amountFloat = amount.replace(",", ".").toFloatOrNull() ?: 0f

        val stawka = when (selectedOption) {
            "Podstawowe (250 zł)" -> 250f
            "Podwyższone (300 zł)" -> 300f
            else -> 0f
        }

        yearlyCalculationResult = calculator.calculateYearlyProjection(
            monthlyGrossAmount = amountFloat,
            stawkaKosztow = stawka,
            isPitExempt = isPitExempt,
            isTaxFreeAmountEnabled = isTaxFreeAmountEnabled,
            isPpkEnabled = isPpkEnabled,
            rates = currentTaxRates
        )
    }
}