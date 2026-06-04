package com.example.kalkulatorwyplat.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.data.TaxRates
import com.example.kalkulatorwyplat.logic.SalaryCalculator

class CalculatorViewModel : ViewModel() {
    private val calculator = SalaryCalculator()

    // Stan formularza
    var amount by mutableStateOf("")
    var isCalculatingFromNetto by mutableStateOf(false)
    var isPitExempt by mutableStateOf(false)
    var isSickLeaveEnabled by mutableStateOf(true)
    var selectedOption by mutableStateOf("Podstawowe (250 zł)")

    // Opcje zaawansowane
    var isTaxFreeAmountEnabled by mutableStateOf(true)
    var isPpkEnabled by mutableStateOf(false)

    // --- NOWOŚĆ: Stan stawek podatkowych ---
    // To jest to, czego brakowało. Przechowujemy tu obiekt z procentami.
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
            rates = currentTaxRates // Przekazujemy aktualne stawki do kalkulatora
        )
    }

    // --- NOWOŚĆ: Funkcja aktualizacji stawek ---
    // To jest funkcja, którą wywołuje ekran ustawień
    fun updateRate(rateType: String, newValue: String) {
        // Zamieniamy przecinek na kropkę i parsujemy na Float
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
        // Przeliczamy wyniki od razu po zmianie stawki
        calculate()
    }
    fun resetTaxRates() {
        currentTaxRates = TaxRates() // Tworzy nowy obiekt z domyślnymi wartościami
        calculate() // Przelicza wyniki na nowo
    }
}