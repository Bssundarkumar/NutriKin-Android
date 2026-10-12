package com.nutrikin.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScoringEngineTest {
    private val engine = ScoringEngine()

    private val granolaBar = Product(
        barcode = "0000000000000", name = "Peanut Oat Granola Bar",
        ingredientsText = "rolled oats, peanuts, honey, brown rice syrup, palm oil, salt",
        allergenTags = listOf("en:peanuts"),
        nutrition = Nutrition(calories = 190.0, sugarG = 12.0, sodiumMg = 140.0),
    )

    @Test
    fun `allergy is a hard block`() {
        val kid = Member(id = "1", name = "Arjun", conditions = listOf(Condition.Allergy(Allergen.PEANUTS)))
        val result = engine.score(granolaBar, kid)
        assertEquals(Verdict.AVOID, result.verdict)
        assertEquals(0, result.score)
        assertTrue(result.blockedByAllergy)
    }

    @Test
    fun `allergy falls back to ingredient text when tags are missing`() {
        val product = granolaBar.copy(allergenTags = emptyList())
        val kid = Member(id = "1", name = "Arjun", conditions = listOf(Condition.Allergy(Allergen.PEANUTS)))
        assertTrue(engine.score(product, kid).blockedByAllergy)
    }

    private val beer = Product(
        barcode = "0000000000001", name = "Lager Beer",
        ingredientsText = "water, malted barley, hops, yeast",
        nutrition = Nutrition(calories = 150.0, sugarG = 0.0, sodiumMg = 10.0),
    )

    @Test
    fun `alcohol is hard blocked for children`() {
        val kid = Member(id = "1", name = "Arjun", age = 10)
        val result = engine.score(beer, kid)
        assertEquals(Verdict.AVOID, result.verdict)
        assertEquals(0, result.score)
    }

    @Test
    fun `alcohol is hard blocked for a parent-managed member with no age entered`() {
        val kid = Member(id = "1", name = "Arjun", isManagedByParent = true)
        assertEquals(Verdict.AVOID, engine.score(beer, kid).verdict)
    }

    @Test
    fun `alcohol is never scored okay for an adult either`() {
        val adult = Member(id = "1", name = "Amma", age = 40)
        val result = engine.score(beer, adult)
        assertNotEquals(Verdict.OKAY, result.verdict)
        assertTrue(result.reasons.any { it.contains("alcohol", ignoreCase = true) })
    }

    @Test
    fun `alcohol is detected by a known brand name even with no OFF data`() {
        // Regression case for the real bug fixed on iOS: a barcode lookup with no ingredients, nutrition
        // or category tags at all - Grey Goose vodka, brand name only.
        val product = Product(barcode = "0000000000099", name = "Grey goose original", brand = "Grey Goose")
        val kid = Member(id = "1", name = "Sarada", age = 7)
        val result = engine.score(product, kid)
        assertEquals(Verdict.AVOID, result.verdict)
        assertEquals(0, result.score)
    }

    @Test
    fun `healthy member with no conditions scores okay`() {
        val priya = Member(id = "1", name = "Priya")
        val result = engine.score(granolaBar, priya)
        assertEquals(Verdict.OKAY, result.verdict)
    }

    @Test
    fun `family is sorted most at-risk first`() {
        val members = listOf(
            Member(id = "1", name = "Priya"),
            Member(id = "2", name = "Arjun", conditions = listOf(Condition.Allergy(Allergen.PEANUTS))),
        )
        val results = engine.scoreFamily(granolaBar, members)
        assertEquals("Arjun", results.first().member.name)
    }
}
