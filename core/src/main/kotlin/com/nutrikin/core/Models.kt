package com.nutrikin.core

/** Mirrors FamilyFoodScanner/Models/Models.swift's `Allergen`. */
enum class Allergen(val displayName: String, val offTag: String, val keywords: List<String>) {
    PEANUTS("Peanuts", "en:peanuts", listOf("peanut", "groundnut", "cacahuète", "cacahuete", "arachide", "erdnuss")),
    NUTS("Tree nuts", "en:nuts", listOf("almond", "hazelnut", "cashew", "walnut", "pecan", "pistachio", "macadamia")),
    MILK("Milk", "en:milk", listOf("milk", "whey", "casein", "butter", "cream", "lactose", "cheese", "yogurt", "ghee", "paneer")),
    GLUTEN("Gluten", "en:gluten", listOf("wheat", "barley", "rye", "spelt", "gluten", "semolina", "maida", "atta", "couscous")),
    EGGS("Eggs", "en:eggs", listOf("egg", "mayonnaise", "mayo")),
    SOYBEANS("Soy", "en:soybeans", listOf("soy", "soya", "soja", "tofu", "tempeh", "edamame", "miso")),
    FISH("Fish", "en:fish", listOf("fish", "anchov", "salmon", "tuna", "cod", "sardine", "mackerel", "tilapia", "trout")),
    CRUSTACEANS("Shellfish", "en:crustaceans", listOf("shrimp", "prawn", "crab", "lobster")),
    SESAME("Sesame", "en:sesame", listOf("sesame", "tahini")),
}

/** Mirrors `Condition` - a sealed hierarchy instead of Swift's enum-with-payload. */
sealed class Condition {
    data object Diabetes : Condition()
    data object Hypertension : Condition()
    data object HighCholesterol : Condition()
    data class Allergy(val allergen: Allergen) : Condition()
    data class CustomAllergy(val name: String) : Condition()
    data class Custom(val name: String) : Condition()

    val displayName: String
        get() = when (this) {
            Diabetes -> "Diabetes"
            Hypertension -> "High blood pressure"
            HighCholesterol -> "High cholesterol"
            is Allergy -> "${allergen.displayName} allergy"
            is CustomAllergy -> "$name allergy"
            is Custom -> name
        }

    val isPregnancy: Boolean
        get() = this is Custom && name.lowercase().startsWith("pregnan")
}

enum class Sex { MALE, FEMALE }

/** Mirrors `Member` (trimmed to what scoring needs; the full profile has goals/medications too). */
data class Member(
    val id: String,
    val name: String,
    val conditions: List<Condition> = emptyList(),
    val age: Int? = null,
    val sex: Sex? = null,
    val isManagedByParent: Boolean = false,
) {
    val allergies: List<Allergen> get() = conditions.filterIsInstance<Condition.Allergy>().map { it.allergen }
    val customAllergyNames: List<String> get() = conditions.filterIsInstance<Condition.CustomAllergy>().map { it.name }
    val isPregnant: Boolean get() = conditions.any { it.isPregnancy }
    fun has(c: Condition) = conditions.any {
        when (c) {
            Condition.Diabetes -> it is Condition.Diabetes
            Condition.Hypertension -> it is Condition.Hypertension
            Condition.HighCholesterol -> it is Condition.HighCholesterol
            else -> it == c
        }
    }

    /** Mirrors `TodayLayout.isChild`: under 18, or no age on record but flagged as parent-managed. */
    val isChild: Boolean get() = (age != null && age < 18) || (age == null && isManagedByParent)
}

data class Nutrition(
    val calories: Double? = null,
    val sugarG: Double? = null,
    val carbsG: Double? = null,
    val sodiumMg: Double? = null,
    val satFatG: Double? = null,
    val proteinG: Double? = null,
    val basis: String = "per 100 g",
)

/** Mirrors `Product` (trimmed - the full model also carries NutriScore/NOVA/traces/etc.). */
data class Product(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val ingredientsText: String? = null,
    val allergenTags: List<String> = emptyList(),
    val nutrition: Nutrition = Nutrition(),
    val ingredientTags: List<String> = emptyList(),
    val categoryTags: List<String> = emptyList(),
) {
    val hasIngredientInfo: Boolean get() = !ingredientsText.isNullOrBlank() || ingredientTags.isNotEmpty()
    val hasAnyNutrition: Boolean
        get() = listOf(nutrition.calories, nutrition.sugarG, nutrition.carbsG, nutrition.sodiumMg, nutrition.satFatG, nutrition.proteinG)
            .any { it != null }

    /** Same text Swift's `IngredientAnalyzer`/`PregnancyGuidance.searchText` build: name + brand +
     * ingredients + tags, lower-cased, for keyword matching. */
    val searchText: String
        get() = (listOf(name, brand ?: "", ingredientsText ?: "") + categoryTags + ingredientTags)
            .joinToString(" , ").lowercase()

    fun contains(allergen: Allergen): Boolean =
        allergenTags.contains(allergen.offTag) || allergen.keywords.any { searchText.contains(it) }

    fun containsKeyword(word: String): Boolean = searchText.contains(word.trim().lowercase())
}

enum class Verdict { OKAY, CAUTION, AVOID }

data class MemberScore(
    val member: Member,
    val score: Int,
    val verdict: Verdict,
    val reasons: List<String>,
    val blockedByAllergy: Boolean = false,
)
