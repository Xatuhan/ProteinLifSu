package com.proteinlifsu.domain

import androidx.compose.runtime.Immutable

@Immutable
data class Ingredient(val food: Food, val grams: Float)

@Immutable
data class RecipeResult(
    val totalGrams: Float,
    val totalProteinG: Float,
    val totalFiberG: Float,
    /** 100 g pişmiş yemekteki değerler. */
    val per100: Nutrients,
)

object Recipe {

    /**
     * Malzemelerden 100 g başına değer. [cookedWeightG] verilirse (pişince su kaybı/kazancı)
     * toplam ağırlık odur; verilmezse malzeme gramlarının toplamı.
     *
     * Hatalar: malzeme yoksa EMPTY; malzeme sayısı/ağırlık sınır dışıysa TOO_LARGE; pişmiş
     * ağırlık, 100 g'da 100 g'dan fazla besin çıkacak kadar küçükse TOO_SMALL.
     */
    fun calculate(ingredients: List<Ingredient>, cookedWeightG: Float?): Validated<RecipeResult> {
        if (ingredients.isEmpty()) return Validated.Invalid(InputError.EMPTY)
        if (ingredients.size > InputLimits.MAX_INGREDIENTS) return Validated.Invalid(InputError.TOO_LARGE)
        var grams = 0.0
        var protein = 0.0
        var fiber = 0.0
        for (i in ingredients) {
            if (InputValidator.checkFoodGrams(i.grams, i.food.maxGramsPerEntry) !is Validated.Ok) return Validated.Invalid(InputError.NOT_A_NUMBER)
            val n = i.food.nutrientsFor(i.grams)
            grams += i.grams
            protein += n.proteinG
            fiber += n.fiberG
        }
        val weight = cookedWeightG?.toDouble() ?: grams
        if (!weight.isFinite() || weight <= 0.0) return Validated.Invalid(InputError.NOT_POSITIVE)
        if (weight > InputLimits.MAX_RECIPE_GRAMS) return Validated.Invalid(InputError.TOO_LARGE)
        val p100 = protein * 100.0 / weight
        val f100 = fiber * 100.0 / weight
        if (p100 + f100 > 100.0) return Validated.Invalid(InputError.TOO_SMALL)
        return Validated.Ok(
            RecipeResult(
                totalGrams = weight.toFloat(),
                totalProteinG = protein.toFloat(),
                totalFiberG = fiber.toFloat(),
                per100 = Nutrients(p100.toFloat(), f100.toFloat()),
            ),
        )
    }

    fun validateCookedWeight(raw: String): Validated<Float?> {
        if (raw.isBlank()) return Validated.Ok(null)
        val v = InputValidator.parseDecimal(raw) ?: return Validated.Invalid(InputError.NOT_A_NUMBER)
        return when {
            v <= 0f -> Validated.Invalid(InputError.NOT_POSITIVE)
            v > InputLimits.MAX_RECIPE_GRAMS -> Validated.Invalid(InputError.TOO_LARGE)
            else -> Validated.Ok(v)
        }
    }
}
