package com.nutrikin.app.data

import com.nutrikin.core.Condition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * Row shape of the `members` table (backend/schema.sql + migration_002/003_*.sql + migration_005_auth.sql).
 * RLS already scopes this to households the signed-in user belongs to (`is_household_member`), so a plain
 * `supabase.from("members").select()` after sign-in returns exactly what this account should see - same
 * trust model the iOS app relies on, nothing Android-specific to replicate here.
 */
@Serializable
data class MemberDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val name: String,
    /** The `conditions` jsonb column, as raw JSON (NOT a string - jsonb comes back as a real JSON
     * array/object over Postgrest). Use `.decodedConditions` for the typed form - see ConditionCodec
     * for the exact wire shape, verified against what the iOS app actually writes. */
    val conditions: JsonElement = JsonArray(emptyList()),
    val age: Int? = null,
    val sex: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("is_managed_by_parent") val isManagedByParent: Boolean = false,
) {
    val decodedConditions: List<Condition> get() = ConditionCodec.decode(conditions)
}

/** What's sent to insert/update a member - mirrors FamilyStore.swift's NewMember/updateMember payload. */
@Serializable
data class NewMemberPayload(
    @SerialName("household_id") val householdId: String,
    val name: String,
    val conditions: JsonElement,
    val age: Int? = null,
    val sex: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("is_managed_by_parent") val isManagedByParent: Boolean = false,
)
