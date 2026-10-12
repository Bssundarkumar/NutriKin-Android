package com.nutrikin.app.data

import com.nutrikin.core.Allergen
import com.nutrikin.core.Condition
import kotlinx.serialization.json.*

/**
 * The `conditions` jsonb column's wire format, verified empirically against what the iOS app's shared
 * Postgrest encoder (JSONEncoder with keyEncodingStrategy = .convertToSnakeCase) actually produces for
 * Swift's `[Condition]` - Swift's *default* enum-with-associated-value synthesis, run through
 * convertToSnakeCase, NOT the `{"type":"diabetes"}` shape an old schema.sql comment suggested (that
 * comment is stale/aspirational; this codec matches real behavior, confirmed by running the iOS struct
 * through `swift` directly rather than trusting the comment):
 *
 *   [{"diabetes":{}}, {"high_cholesterol":{}}, {"allergy":{"_0":"peanuts"}},
 *    {"custom_allergy":{"_0":"kiwi"}}, {"custom":{"_0":"Celiac disease"}}]
 *
 * Case names snake_case (highCholesterol -> high_cholesterol, customAllergy -> custom_allergy);
 * single-word cases (diabetes, hypertension, allergy, custom) are unaffected; the associated value
 * always sits under the key "_0" (Swift's synthesized name for an unlabelled enum payload).
 */
object ConditionCodec {
    fun encode(conditions: List<Condition>): JsonArray = JsonArray(conditions.map(::encodeOne))

    private fun encodeOne(c: Condition): JsonObject = when (c) {
        Condition.Diabetes -> wrap("diabetes", null)
        Condition.Hypertension -> wrap("hypertension", null)
        Condition.HighCholesterol -> wrap("high_cholesterol", null)
        is Condition.Allergy -> wrap("allergy", c.allergen.name.lowercase())
        is Condition.CustomAllergy -> wrap("custom_allergy", c.name)
        is Condition.Custom -> wrap("custom", c.name)
    }

    private fun wrap(key: String, payload: String?): JsonObject = buildJsonObject {
        putJsonObject(key) { payload?.let { put("_0", it) } }
    }

    fun decode(json: JsonElement?): List<Condition> {
        val array = json as? JsonArray ?: return emptyList()
        return array.mapNotNull { element -> (element as? JsonObject)?.let(::decodeOne) }
    }

    private fun decodeOne(obj: JsonObject): Condition? {
        val (key, value) = obj.entries.firstOrNull() ?: return null
        val payload = (value as? JsonObject)?.get("_0")?.jsonPrimitive?.contentOrNull
        return when (key) {
            "diabetes" -> Condition.Diabetes
            "hypertension" -> Condition.Hypertension
            "high_cholesterol" -> Condition.HighCholesterol
            "allergy" -> payload?.let { name ->
                Allergen.entries.find { it.name.equals(name, ignoreCase = true) }?.let(Condition::Allergy)
            }
            "custom_allergy" -> payload?.let(Condition::CustomAllergy)
            "custom" -> payload?.let(Condition::Custom)
            else -> null
        }
    }
}
