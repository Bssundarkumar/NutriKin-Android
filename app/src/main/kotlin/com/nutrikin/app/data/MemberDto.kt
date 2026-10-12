package com.nutrikin.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray

/**
 * Row shape of the `members` table (backend/schema.sql + migration_005_auth.sql). RLS already scopes
 * this to households the signed-in user belongs to (`is_household_member`), so a plain
 * `supabase.from("members").select()` after sign-in returns exactly what this account should see -
 * same trust model the iOS app relies on, nothing Android-specific to replicate here.
 */
@Serializable
data class MemberDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val name: String,
    /** The `conditions` jsonb column, as raw JSON (NOT a string - jsonb comes back as a real JSON
     * array/object over Postgrest, so this must be JsonElement, not String, or decoding throws a type
     * mismatch). Mapping this into core.Condition's tagged-union shape is still TODO - see the Android
     * repo README. */
    val conditions: JsonElement = JsonArray(emptyList()),
    val age: Int? = null,
    val sex: String? = null,
    @SerialName("is_managed_by_parent") val isManagedByParent: Boolean = false,
)
