package com.nutrikin.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mirrors FamilyStore.swift: households are created/joined through the same RPCs the iOS app calls
 * (create_household/join_household - see backend/migration_005_auth.sql), never by inserting into
 * `households` directly. Members are a plain insert into `members`, scoped by RLS to the caller's
 * household - same trust model as iOS, see backend/migration_005_auth.sql's is_household_member().
 */
class FamilyRepository {
    /** Null if this account hasn't joined/created a household yet. */
    suspend fun currentHouseholdId(): String? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        val rows: List<HouseholdUserRow> = supabase.from("household_users")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
        return rows.firstOrNull()?.householdId
    }

    suspend fun fetchMembers(): List<MemberDto> =
        supabase.from("members").select().decodeList()

    suspend fun createHousehold(name: String): HouseholdDto =
        decodeHouseholdRpc(supabase.postgrest.rpc("create_household", mapOf("p_name" to name)).data)

    suspend fun joinHousehold(code: String): HouseholdDto =
        decodeHouseholdRpc(supabase.postgrest.rpc("join_household", mapOf("p_code" to code.trim().uppercase())).data)

    private fun decodeHouseholdRpc(raw: String): HouseholdDto {
        // The RPC returns a single household row as a JSON object (the pg function is declared
        // `returns households`, same shape the iOS app's Backend.decodeRow helper expects).
        val json = Json.parseToJsonElement(raw) as JsonObject
        return HouseholdDto(
            id = json["id"]!!.jsonPrimitive.content,
            name = json["name"]!!.jsonPrimitive.content,
            inviteCode = json["invite_code"]!!.jsonPrimitive.content,
        )
    }

    suspend fun addMember(householdId: String, name: String) {
        supabase.from("members").insert(
            mapOf("household_id" to householdId, "name" to name),
        )
    }
}
