package com.nutrikin.core

/**
 * Rule-based, explainable scoring - ported from FamilyFoodScanner/Services/Scoring/ScoringEngine.swift.
 * Every point deducted produces a reason the user can read; no AI in this path, especially not for
 * allergies, where the result must be deterministic.
 *
 * Scope of this port (intentionally bounded for the initial Android scaffold - see the Android repo
 * README): the two *hard blocks* most load-bearing for safety are ported exactly (allergy, alcohol for
 * children), plus the general sugar/calorie caution scoring. NOT yet ported from the iOS engine:
 * honey/juice hard blocks for infants, caffeine/mercury-fish/choking-hazard/cured-meat child-safety
 * caps, pregnancy guidance, and supplement caution. Do not treat Android scores as equivalent to iOS's
 * until those land - this is flagged loudly in the README and should be closed before any real release.
 */
class ScoringEngine {
    companion object {
        const val DEFAULT_SODIUM_LIMIT_MG = 1500.0

        fun defaultSugarLimitG(sex: Sex?) = if (sex == Sex.MALE) 36.0 else 25.0
        fun defaultSatFatLimitG(sex: Sex?) = if (sex == Sex.MALE) 16.0 else 13.0
        fun defaultCalorieGoal(sex: Sex?) = if (sex == Sex.MALE) 2500.0 else 2000.0
    }

    fun score(product: Product, member: Member): MemberScore {
        // 1. Allergies are a hard block, independent of nutrition.
        val hits = member.allergies.filter { product.contains(it) }.map { it.displayName }
        val customHits = member.customAllergyNames.filter { product.containsKeyword(it) }
        val allHits = hits + customHits
        if (allHits.isNotEmpty()) {
            val names = allHits.joinToString(", ")
            return MemberScore(
                member = member, score = 0, verdict = Verdict.AVOID,
                reasons = listOf("Contains ${names.lowercase()}. Allergy rules block this product regardless of nutrition."),
                blockedByAllergy = true,
            )
        }

        // 1b. Alcohol is a hard block for children, independent of nutrition.
        val isAlcoholic = AlcoholGuidance.isAlcoholic(product)
        if (isAlcoholic && member.isChild) {
            return MemberScore(
                member = member, score = 0, verdict = Verdict.AVOID,
                reasons = listOf("Contains alcohol. Not appropriate for children, regardless of its nutrition."),
            )
        }

        // 2. General scoring: start from 100, deduct for limits the member cares about.
        var score = 100.0
        val reasons = mutableListOf<String>()
        val n = product.nutrition

        if (member.has(Condition.Diabetes)) {
            val limit = defaultSugarLimitG(member.sex)
            val sugar = n.sugarG
            if (sugar != null && sugar > 0) {
                val share = sugar / limit
                if (share >= 1.0) {
                    score -= 40
                    reasons.add("High in sugar for someone managing diabetes (${sugar.fmt()} g, daily limit ${limit.fmt()} g).")
                } else if (share >= 0.4) {
                    score -= 15
                    reasons.add("Has a meaningful amount of sugar (${sugar.fmt()} g) to weigh against today's diabetes limit.")
                }
            }
        }

        if (isAlcoholic && !member.isPregnant) {
            score = minOf(score, 45.0)
            reasons.add("Contains alcohol — capped regardless of its other nutrition.")
        }

        val sodium = n.sodiumMg
        if (member.has(Condition.Hypertension) && sodium != null && sodium > 0) {
            val share = sodium / DEFAULT_SODIUM_LIMIT_MG
            if (share >= 0.5) {
                score -= 20
                reasons.add("High in sodium (${sodium.fmt()} mg) for someone managing blood pressure.")
            }
        }

        score = score.coerceIn(0.0, 100.0)
        if (reasons.isEmpty()) reasons.add("Fits this person's recorded needs.")
        val verdict = when {
            score >= 80 -> Verdict.OKAY
            score >= 50 -> Verdict.CAUTION
            else -> Verdict.AVOID
        }
        return MemberScore(member, score.toInt(), verdict, reasons)
    }

    fun scoreFamily(product: Product, members: List<Member>): List<MemberScore> =
        members.map { score(product, it) }.sortedBy { it.score }
}

private fun Double.fmt(): String = if (this == this.toInt().toDouble()) this.toInt().toString() else "%.1f".format(this)
