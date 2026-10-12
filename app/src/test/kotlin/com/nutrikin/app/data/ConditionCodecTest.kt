package com.nutrikin.app.data

import com.nutrikin.core.Allergen
import com.nutrikin.core.Condition
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionCodecTest {
    @Test
    fun `encodes exactly what the iOS app writes to the conditions jsonb column`() {
        val conditions = listOf(
            Condition.Diabetes,
            Condition.HighCholesterol,
            Condition.Allergy(Allergen.PEANUTS),
            Condition.CustomAllergy("kiwi"),
            Condition.Custom("Celiac disease"),
        )
        val expected = """[{"diabetes":{}},{"high_cholesterol":{}},{"allergy":{"_0":"peanuts"}},""" +
            """{"custom_allergy":{"_0":"kiwi"}},{"custom":{"_0":"Celiac disease"}}]"""
        assertEquals(expected, ConditionCodec.encode(conditions).toString())
    }

    @Test
    fun `round-trips through encode and decode`() {
        val conditions = listOf(
            Condition.Diabetes, Condition.Hypertension, Condition.HighCholesterol,
            Condition.Allergy(Allergen.MILK), Condition.CustomAllergy("kiwi"), Condition.Custom("Celiac disease"),
        )
        assertEquals(conditions, ConditionCodec.decode(ConditionCodec.encode(conditions)))
    }

    @Test
    fun `decodes a row shaped exactly like what iOS actually stores`() {
        val raw = """[{"diabetes":{}},{"allergy":{"_0":"peanuts"}}]"""
        val decoded = ConditionCodec.decode(Json.parseToJsonElement(raw))
        assertEquals(listOf(Condition.Diabetes, Condition.Allergy(Allergen.PEANUTS)), decoded)
    }
}
