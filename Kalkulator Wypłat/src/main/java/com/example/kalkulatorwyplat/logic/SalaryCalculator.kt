package com.example.kalkulatorwyplat.logic

import com.example.kalkulatorwyplat.data.MonthlyDetailedResult
import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.data.TaxRates
import com.example.kalkulatorwyplat.data.YearlySalaryResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

class SalaryCalculator {

    private val ppkPracownikRate = 0.02f
    private val ppkPracodawcaRate = 0.015f
    private val taxThreshold = 120000f
    private val higherTaxRate = 32.00f
    private val taxFreeAllowanceYearly = 30000f
    private val taxFreeAllowanceMonthly = taxFreeAllowanceYearly / 12f * 0.12f

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

    // LOGIKA UMOWY O PRACĘ (ROZBITE METODY ZWIĘKSZAJĄCE LLOC)

    private fun calculatePensionContribution(gross: Float, rate: Float): Float {
        return roundToTwoDecimals(gross * (rate / 100))
    }

    private fun calculateDisabilityContribution(gross: Float, rate: Float): Float {
        return roundToTwoDecimals(gross * (rate / 100))
    }

    private fun calculateSickLeaveContribution(gross: Float, rate: Float, isEnabled: Boolean): Float {
        return if (isEnabled) roundToTwoDecimals(gross * (rate / 100)) else 0f
    }

    private fun calculateHealthInsuranceBase(gross: Float, socialContributions: Float): Float {
        return max(0f, gross - socialContributions)
    }

    private fun calculateHealthInsurance(base: Float, rate: Float): Float {
        return roundToTwoDecimals(base * (rate / 100))
    }

    private fun calculateTaxBase(healthBase: Float, deductibleCosts: Float, employerPpk: Float): Float {
        return round(max(0f, healthBase - deductibleCosts + employerPpk))
    }

    private fun calculateAdvanceTax(
        taxBase: Float,
        isPitExempt: Boolean,
        isTaxFreeAmountEnabled: Boolean,
        currentTaxRate: Float
    ): Float {
        if (isPitExempt) return 0f

        var advanceTax = taxBase * (currentTaxRate / 100)

        if (isTaxFreeAmountEnabled) {
            advanceTax -= taxFreeAllowanceMonthly
        }

        return max(0f, round(advanceTax))
    }

    private fun umPracaBruttoNaNetto(
        kwota: Float,
        kupPraca: Float,
        isPitExempt: Boolean,
        isTaxFree: Boolean,
        isPpk: Boolean,
        rates: TaxRates,
        customTaxRate: Float? = null
    ): SalaryResult {

        val sEmery = calculatePensionContribution(kwota, rates.emerytalna)
        val sRent = calculateDisabilityContribution(kwota, rates.rentowa)
        val sChor = calculateSickLeaveContribution(kwota, rates.chorobowa, true)
        val uSpol = sEmery + sRent + sChor

        val podstZdr = calculateHealthInsuranceBase(kwota, uSpol)
        val uZdro = calculateHealthInsurance(podstZdr, rates.zdrowotna)

        val kUzyPrz = kupPraca
        val ppkPracodawca = if (isPpk) roundToTwoDecimals(kwota * ppkPracodawcaRate) else 0f

        val poDoch = calculateTaxBase(podstZdr, kUzyPrz, ppkPracodawca)

        val appliedTaxRate = customTaxRate ?: rates.pit
        val zalPoddoch = calculateAdvanceTax(poDoch, isPitExempt, isTaxFree, appliedTaxRate)

        val ppkPracownik = if (isPpk) roundToTwoDecimals(kwota * ppkPracownikRate) else 0f
        val netto = roundToTwoDecimals(kwota - uSpol - uZdro - zalPoddoch - ppkPracownik)

        return SalaryResult(
            type = "Umowa o Pracę",
            gross = kwota,
            netto = netto,
            sEmery = sEmery,
            sRent = sRent,
            sChor = sChor,
            uSpol = uSpol,
            uZdro = uZdro,
            kUzyPrz = kUzyPrz,
            poDoch = poDoch,
            zalPoddoch = zalPoddoch,
            ppkPracownik = ppkPracownik,
            ppkPracodawca = ppkPracodawca
        )
    }

    private fun umPracaNettoNaBrutto(
        nettoCel: Float,
        kupPraca: Float,
        pitExempt: Boolean,
        taxFree: Boolean,
        ppk: Boolean,
        rates: TaxRates
    ): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 2.5f
        var guess = 0f

        repeat(60) {
            guess = (min + max) / 2
            val res = umPracaBruttoNaNetto(guess, kupPraca, pitExempt, taxFree, ppk, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umPracaBruttoNaNetto(guess, kupPraca, pitExempt, taxFree, ppk, rates)
    }

    // LOGIKA UMOWY ZLECENIA

    private fun umZlecenieBruttoNaNetto(
        kwota: Float,
        isPitExempt: Boolean,
        isChor: Boolean,
        isTaxFree: Boolean,
        isPpk: Boolean,
        rates: TaxRates
    ): SalaryResult {
        val sEmery = calculatePensionContribution(kwota, rates.emerytalna)
        val sRent = calculateDisabilityContribution(kwota, rates.rentowa)
        val sChorVal = calculateSickLeaveContribution(kwota, rates.chorobowa, isChor)
        val uSpol = sEmery + sRent + sChorVal

        val podstZdr = calculateHealthInsuranceBase(kwota, uSpol)
        val uZdro = calculateHealthInsurance(podstZdr, rates.zdrowotna)

        val kUzyPrz = roundToTwoDecimals(podstZdr * 0.20f)
        val ppkPracodawca = if (isPpk) roundToTwoDecimals(kwota * ppkPracodawcaRate) else 0f

        val poDoch = calculateTaxBase(podstZdr, kUzyPrz, ppkPracodawca)
        val zalPoddoch = calculateAdvanceTax(poDoch, isPitExempt, isTaxFree, rates.pit)

        val ppkPracownik = if (isPpk) roundToTwoDecimals(kwota * ppkPracownikRate) else 0f
        val netto = roundToTwoDecimals(kwota - uSpol - uZdro - zalPoddoch - ppkPracownik)

        return SalaryResult(
            type = "Umowa Zlecenie",
            gross = kwota,
            netto = netto,
            sEmery = sEmery,
            sRent = sRent,
            sChor = sChorVal,
            uSpol = uSpol,
            uZdro = uZdro,
            kUzyPrz = kUzyPrz,
            poDoch = poDoch,
            zalPoddoch = zalPoddoch,
            ppkPracownik = ppkPracownik,
            ppkPracodawca = ppkPracodawca
        )
    }

    private fun umZlecenieNettoNaBrutto(
        nettoCel: Float,
        pitExempt: Boolean,
        chor: Boolean,
        taxFree: Boolean,
        ppk: Boolean,
        rates: TaxRates
    ): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 2.5f
        var guess = 0f
        repeat(60) {
            guess = (min + max) / 2
            val res = umZlecenieBruttoNaNetto(guess, pitExempt, chor, taxFree, ppk, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umZlecenieBruttoNaNetto(guess, pitExempt, chor, taxFree, ppk, rates)
    }

    // LOGIKA UMOWY O DZIEŁO

    private fun umDzieloBruttoNaNetto(
        kwota: Float,
        isPitExempt: Boolean,
        rates: TaxRates
    ): SalaryResult {
        val kUzyPrz = roundToTwoDecimals(kwota * 0.20f)
        val poDoch = round(max(0f, kwota - kUzyPrz))
        val zalPoddoch = calculateAdvanceTax(poDoch, isPitExempt, false, rates.pit)

        return SalaryResult(
            type = "Umowa o Dzieło",
            gross = kwota,
            netto = roundToTwoDecimals(kwota - zalPoddoch),
            kUzyPrz = kUzyPrz,
            poDoch = poDoch,
            zalPoddoch = zalPoddoch,
            sEmery = 0f,
            sRent = 0f,
            sChor = 0f,
            uSpol = 0f,
            uZdro = 0f
        )
    }

    private fun umDzieloNettoNaBrutto(
        nettoCel: Float,
        pitExempt: Boolean,
        rates: TaxRates
    ): SalaryResult {
        var min = nettoCel
        var max = nettoCel * 3f
        var guess = 0f
        repeat(60) {
            guess = (min + max) / 2
            val res = umDzieloBruttoNaNetto(guess, pitExempt, rates)
            if (abs(res.netto - nettoCel) < 0.01f) return res
            if (res.netto < nettoCel) min = guess else max = guess
        }
        return umDzieloBruttoNaNetto(guess, pitExempt, rates)
    }

    // ZAAWANSOWANA SYMULACJA ROCZNA Z UWZGLĘDNIENIEM PROGU PODATKOWEGO (120 000 PLN)

    fun calculateYearlyProjection(
        monthlyGrossAmount: Float,
        stawkaKosztow: Float,
        isPitExempt: Boolean,
        isTaxFreeAmountEnabled: Boolean,
        isPpkEnabled: Boolean,
        rates: TaxRates
    ): YearlySalaryResult {

        val bezpieczneKupPraca = if (stawkaKosztow < 10f) 250f else stawkaKosztow
        var accumulatedIncomeBase = 0f
        var thresholdCrossedMonth: Int? = null

        val monthlyDetails = mutableListOf<MonthlyDetailedResult>()

        var totalGross = 0f
        var totalNetto = 0f
        var totalZus = 0f
        var totalHealth = 0f
        var totalTax = 0f

        for (month in 1..12) {
            // Decydujemy o stawce podatku na podstawie zakumulowanego dochodu
            val currentTaxRate = if (accumulatedIncomeBase > taxThreshold) {
                if (thresholdCrossedMonth == null) thresholdCrossedMonth = month
                higherTaxRate
            } else {
                rates.pit
            }

            val monthlyCalculation = umPracaBruttoNaNetto(
                kwota = monthlyGrossAmount,
                kupPraca = bezpieczneKupPraca,
                isPitExempt = isPitExempt,
                isTaxFree = isTaxFreeAmountEnabled,
                isPpk = isPpkEnabled,
                rates = rates,
                customTaxRate = currentTaxRate
            )

            // Aktualizujemy skumulowaną podstawę opodatkowania
            accumulatedIncomeBase += monthlyCalculation.poDoch

            val detailedResult = MonthlyDetailedResult(
                monthNumber = month,
                monthName = "month_$month", // Referencja, do przetłumaczenia w UI
                isTaxThresholdCrossed = currentTaxRate == higherTaxRate,
                taxRateApplied = currentTaxRate,
                accumulatedIncome = accumulatedIncomeBase,
                monthlyResult = monthlyCalculation
            )

            monthlyDetails.add(detailedResult)

            // Agregacja sum rocznych
            totalGross += monthlyCalculation.gross
            totalNetto += monthlyCalculation.netto
            totalZus += monthlyCalculation.uSpol
            totalHealth += monthlyCalculation.uZdro
            totalTax += monthlyCalculation.zalPoddoch
        }

        return YearlySalaryResult(
            contractType = "Umowa o Pracę (Symulacja Roczna)",
            totalGross = roundToTwoDecimals(totalGross),
            totalNetto = roundToTwoDecimals(totalNetto),
            averageNetto = roundToTwoDecimals(totalNetto / 12f),
            totalZus = roundToTwoDecimals(totalZus),
            totalHealthInsurance = roundToTwoDecimals(totalHealth),
            totalIncomeTax = roundToTwoDecimals(totalTax),
            crossedTaxThresholdMonth = thresholdCrossedMonth,
            monthlyDetails = monthlyDetails
        )
    }

    private fun roundToTwoDecimals(value: Float): Float {
        return round(value * 100) / 100
    }
}