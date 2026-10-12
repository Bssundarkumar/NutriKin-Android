package com.nutrikin.core

import kotlin.math.roundToInt
import java.util.Locale

/** Deterministic rules mirrored from the iOS ScoringEngine; no AI in allergy decisions. */
class ScoringEngine {
    companion object {
        const val DEFAULT_SODIUM_LIMIT_MG = 1500.0
        fun defaultSugarLimitG(sex: Sex?) = if (sex == Sex.MALE) 36.0 else 25.0
        fun defaultSatFatLimitG(sex: Sex?) = if (sex == Sex.MALE) 16.0 else 13.0
        fun defaultCalorieGoal(sex: Sex?) = if (sex == Sex.MALE) 2500.0 else 2000.0
    }

    fun score(product: Product, member: Member): MemberScore {
        val hits = member.allergies.filter { product.contains(it) }.map { it.displayName } +
            member.customAllergyNames.filter { product.containsKeyword(it) }
        if (hits.isNotEmpty()) return MemberScore(member, 0, Verdict.AVOID,
            listOf("Contains ${hits.joinToString(", ").lowercase()}. Allergy rules block this product regardless of nutrition."), true)
        val alcohol = AlcoholGuidance.isAlcoholic(product)
        if (alcohol && member.isChild) return blocked(member, "Contains alcohol. Not appropriate for children, regardless of its nutrition.")
        if (member.age == 0) {
            if (SafetyGuidance.contains(product.searchText, SafetyGuidance.honeyTerms))
                return blocked(member, "Contains honey. Not safe for a baby under 1 year — risk of infant botulism, regardless of nutrition.")
            if (SafetyGuidance.contains(product.searchText, SafetyGuidance.juiceTerms))
                return blocked(member, "Contains fruit juice. Pediatric guidance (AAP) advises no juice at all before 12 months, regardless of nutrition.")
        }
        var score = 100.0
        val reasons = mutableListOf<String>()
        val n = product.nutrition
        fun deduct(value: Double?, limit: Double, multiplier: Double, maximum: Double, label: String, unit: String, goal: String) {
            if (value == null) return
            val share = value / limit
            val penalty = minOf(maximum, share * multiplier)
            score -= penalty
            if (penalty >= 10) reasons += "${value.fmt()} $unit $label is ${pct(share)} of the ${limit.fmt()} $unit daily $goal."
        }
        if (member.has(Condition.Diabetes)) {
            deduct(n.sugarG, positive(member.goals.dailySugarGrams, defaultSugarLimitG(member.sex)), 100.0, 60.0, "sugar", "g", "sugar goal")
            if ((n.carbsG ?: 0.0) > 30) { score -= 10; reasons += "${n.carbsG!!.fmt()} g carbs is on the high side for one serving." }
        }
        if (member.has(Condition.Hypertension))
            deduct(n.sodiumMg, positive(member.goals.dailySodiumMg, DEFAULT_SODIUM_LIMIT_MG), 150.0, 60.0, "sodium", "mg", "limit")
        if (member.has(Condition.HighCholesterol)) {
            deduct(n.satFatG, positive(member.goals.dailySatFatGrams, defaultSatFatLimitG(member.sex)), 120.0, 50.0, "saturated fat", "g", "limit")
            if ((n.transFatG ?: 0.0) > 0.2) { score -= 20; reasons += "Contains trans fat (${n.transFatG!!.fmt()} g)." }
        }
        n.calories?.let {
            val goal = positive(member.goals.dailyCalories, defaultCalorieGoal(member.sex))
            val share = it / goal
            if (share > .25) score -= minOf(30.0, (share - .25) * 100)
            reasons += "${it.fmt()} kcal is ${pct(share)} of the ${goal.fmt()} kcal daily goal."
        }
        var cap = 100
        val warnings = mutableListOf<String>()
        fun warn(limit: Int, reason: String) { cap = minOf(cap, limit); warnings += reason }
        val allergyNames = member.allergies.map { it.displayName } + member.customAllergyNames
        if (allergyNames.isNotEmpty() && !product.hasIngredientInfo && product.allergenTags.isEmpty())
            warn(60, "This product has no ingredient list, so ${member.name}'s ${allergyNames.joinToString(", ")} allergy can't be ruled out. Check the package.")
        val traces = member.allergies.filter { it.offTag in product.tracesTags }
        if (traces.isNotEmpty()) warn(60, "The label says it may contain traces of ${traces.joinToString(", ") { it.displayName }}. Take care with ${member.name}'s allergy.")
        val missing = buildList {
            if (member.has(Condition.Diabetes) && n.sugarG == null) add("sugar (diabetes)")
            if (member.has(Condition.Hypertension) && n.sodiumMg == null) add("sodium (blood pressure)")
            if (member.has(Condition.HighCholesterol) && n.satFatG == null) add("saturated fat (cholesterol)")
        }
        if (missing.isNotEmpty()) warn(69, "No ${missing.joinToString(", ")} figure on this product, so it can't be fully checked for ${member.name}.")
        if (member.isPregnant) {
            SafetyGuidance.pregnancyMatches(product).forEach { warn(it.cap, "${it.title}: ${it.reason}") }
            if (!product.hasIngredientInfo) warn(65, "No ingredient list is available, so ${member.name}'s pregnancy can't be fully checked. Look at the package.")
        }
        if (member.isChild) {
            SafetyGuidance.childMatches(product, member.age).forEach { warn(it.cap, "${it.title}: ${it.reason}") }
            if (product.isSupplement) {
                val iron = SafetyGuidance.contains(product.searchText, listOf("iron"))
                warn(if (iron) 50 else 65, if (iron)
                    "Contains iron. Adult-dose iron supplements are a leading cause of accidental child poisoning — use only a children's dose and keep the bottle out of reach."
                    else "This is a supplement, not food — check with a pediatrician that the dose is right for a child before giving it.")
            }
            if (!member.has(Condition.Diabetes) && (n.sugarG ?: 0.0) > 0 && SafetyGuidance.contains(product.ingredientsText.orEmpty(), SafetyGuidance.addedSweetenerTerms)) {
                if ((member.age ?: 2) <= 1) warn(50, "Contains ${n.sugarG!!.fmt()} g added sugar. Guidance (WHO/AAP) advises no added sugar at all under age 2.")
                else if (n.sugarG!! / 25 >= .4) warn(65, "${n.sugarG.fmt()} g sugar is ${pct(n.sugarG / 25)} of a child's roughly 25 g daily guidance.")
            }
        }
        if (alcohol && !member.isPregnant) warn(45, "Contains alcohol — this is capped regardless of its other nutrition.")
        val final = minOf(score, cap.toDouble()).coerceIn(0.0, 100.0).roundToInt()
        val verdict = if (final >= 70) Verdict.OKAY else if (final >= 40) Verdict.CAUTION else Verdict.AVOID
        if (reasons.isEmpty() && warnings.isEmpty()) reasons += "No concerns for ${member.name}'s conditions or goals."
        return MemberScore(member, final, verdict, warnings + reasons)
    }
    private fun blocked(member: Member, reason: String) = MemberScore(member, 0, Verdict.AVOID, listOf(reason))
    fun scoreFamily(product: Product, members: List<Member>) = members.map { score(product, it) }.sortedBy { it.score }
}
private fun positive(value: Double?, fallback: Double) = value?.takeIf { it.isFinite() && it > 0 } ?: fallback
private fun Double.fmt() = if (this == toInt().toDouble()) toInt().toString() else String.format(Locale.US, "%.1f", this)
private fun pct(share: Double) = "${(share * 100).roundToInt()}%"
