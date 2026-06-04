package com.example.kalkulatorwyplat.logic

import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.data.TaxRates
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

class SalaryCalculator {

    private val ppkPracownikRate = 0.02f
    private val ppkPracodawcaRate = 0.015f

    fun calculateAll(
        amount: Float,
        isBrutto: Boolean,
        stawkaKosztow: Float,
        isPitExempt: Boolean,
        isSickLeaveEnabled: Boolean,
        isTaxFreeAmountEnabled: Boolean,
        isPpkEnabled: Boolean,
        rates: TaxRates
    ): List<SalaryResult> {

        // Zabezpieczenie: Jeśli z ViewModelu wciąż przychodzi "0.02", wymuszamy 250 zł dla etatu.
        // Jeśli przychodzi poprawnie 250 lub 300, używamy tej wartości.
        val bezpieczneKupPraca = if (stawkaKosztow < 10f) 250f else stawkaKosztow

        return if (isBrutto) {
            listOf(
                umPracaBruttoNaNetto(amount, bezpieczneKupPraca, isPitExempt, isTaxFreeAmountEnabled, isPpkEnabled, rates),
                umZlecenieBruttoNaNetto(amount, isPitExempt, isSickLeaveEnabled, isTaxFreeAmountEnabled, isPpkEnabled, rates),
                umDzieloBruttoNaNetto(amount, isPitExempt, rates)
            )
        } else {
            listOf(
                umPracaNettoNaBrutto(amount, bezpieczneKupPraca, isPitExempt, isTaxFreeAmountEnabled, isPpkEnabled, rates),
                umZlecenieNettoNaBrutto(amount, isPitExempt, isSickLeaveEnabled, isTaxFreeAmountEnabled, isPpkEnabled, rates),
                umDzieloNettoNaBrutto(amount, isPitExempt, rates)
            )
        }
    }

    // --- UMOWA O PRACĘ ---
    private fun umPracaBruttoNaNetto(
        kwota: Float, kupPraca: Float, isPitExempt: Boolean, isTaxFree: Boolean, isPpk: Boolean, rates: TaxRates
    ): SalaryResult {
        val sEmery = kwota * (rates.emerytalna / 100)
        val sRent = kwota * (rates.rentowa / 100)
        val sChor = kwota * (rates.chorobowa / 100)
        val uSpol = sEmery + sRent + sChor

        val podstZdr = kwota - uSpol
        val uZdro = podstZdr * (rates.zdrowotna / 100)

        // UMOWA O PRACĘ: Stała kwota (np. 250 zł), a nie procent!
        val kUzyPrz = kupPraca

        val ppkPracodawca = if(isPpk) kwota * ppkPracodawcaRate else 0f
        val poDoch = round(max(0f, podstZdr - kUzyPrz + ppkPracodawca))

        var zalPoddoch = if (isPitExempt) 0f else (poDoch * (rates.pit / 100))

        if (!isPitExempt && isTaxFree) {
            zalPoddoch -= 300f
        }
        zalPoddoch = max(0f, round(zalPoddoch))

        val ppkPracownik = if(isPpk) kwota * ppkPracownikRate else 0f
        val netto = kwota - uSpol - uZdro - zalPoddoch - ppkPracownik

        return SalaryResult(
            type = "Umowa o Pracę", gross = kwota, netto = netto,
            sEmery = sEmery, sRent = sRent, sChor = sChor, uSpol = uSpol, uZdro = uZdro, kUzyPrz = kUzyPrz, poDoch = poDoch, zalPoddoch = zalPoddoch
        )
    }

    private fun umPracaNettoNaBrutto(nettoCel: Float, kupPraca: Float, pitExempt: Boolean, taxFree: Boolean, ppk: Boolean, rates: TaxRates): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 2.5f
        var guess = 0f
        repeat(50) {
            guess = (min + max) / 2
            val res = umPracaBruttoNaNetto(guess, kupPraca, pitExempt, taxFree, ppk, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umPracaBruttoNaNetto(guess, kupPraca, pitExempt, taxFree, ppk, rates)
    }

    // --- UMOWA ZLECENIE ---
    private fun umZlecenieBruttoNaNetto(
        kwota: Float, isPitExempt: Boolean, isChor: Boolean, isTaxFree: Boolean, isPpk: Boolean, rates: TaxRates
    ): SalaryResult {
        val sEmery = kwota * (rates.emerytalna / 100)
        val sRent = kwota * (rates.rentowa / 100)
        val sChorVal = if (isChor) kwota * (rates.chorobowa / 100) else 0f
        val uSpol = sEmery + sRent + sChorVal

        val podstZdr = kwota - uSpol
        val uZdro = podstZdr * (rates.zdrowotna / 100)

        // ZLECENIE: KUP to zawsze 20% od podstawy (Brutto minus ZUS)
        val kUzyPrz = podstZdr * 0.20f

        val ppkPracodawca = if(isPpk) kwota * ppkPracodawcaRate else 0f
        val poDoch = round(max(0f, podstZdr - kUzyPrz + ppkPracodawca))

        var zalPoddoch = if (isPitExempt) 0f else (poDoch * (rates.pit / 100))
        if (!isPitExempt && isTaxFree) zalPoddoch -= 300f
        zalPoddoch = max(0f, round(zalPoddoch))

        val ppkPracownik = if(isPpk) kwota * ppkPracownikRate else 0f
        val netto = kwota - uSpol - uZdro - zalPoddoch - ppkPracownik

        return SalaryResult(
            type = "Umowa Zlecenie", gross = kwota, netto = netto,
            sEmery = sEmery, sRent = sRent, sChor = sChorVal, uSpol = uSpol, uZdro = uZdro, kUzyPrz = kUzyPrz, poDoch = poDoch, zalPoddoch = zalPoddoch
        )
    }

    private fun umZlecenieNettoNaBrutto(nettoCel: Float, pitExempt: Boolean, chor: Boolean, taxFree: Boolean, ppk: Boolean, rates: TaxRates): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 2.5f
        var guess = 0f
        repeat(50) {
            guess = (min + max) / 2
            val res = umZlecenieBruttoNaNetto(guess, pitExempt, chor, taxFree, ppk, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umZlecenieBruttoNaNetto(guess, pitExempt, chor, taxFree, ppk, rates)
    }

    // --- UMOWA O DZIEŁO ---
    private fun umDzieloBruttoNaNetto(kwota: Float, isPitExempt: Boolean, rates: TaxRates): SalaryResult {
        // DZIEŁO: KUP to 20% bezpośrednio od kwoty Brutto
        val kUzyPrz = kwota * 0.20f
        val poDoch = round(max(0f, kwota - kUzyPrz))

        var zalPoddoch = if (isPitExempt) 0f else (poDoch * (rates.pit / 100))
        zalPoddoch = max(0f, round(zalPoddoch))

        return SalaryResult(
            type = "Umowa o Dzieło", gross = kwota, netto = kwota - zalPoddoch,
            kUzyPrz = kUzyPrz, poDoch = poDoch, zalPoddoch = zalPoddoch,
            sEmery = 0f, sRent = 0f, sChor = 0f, uSpol = 0f, uZdro = 0f // Dzieło nie ma ZUS
        )
    }

    private fun umDzieloNettoNaBrutto(nettoCel: Float, pitExempt: Boolean, rates: TaxRates): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 3f
        var guess = 0f
        repeat(50) {
            guess = (min + max) / 2
            val res = umDzieloBruttoNaNetto(guess, pitExempt, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umDzieloBruttoNaNetto(guess, pitExempt, rates)
    }
}