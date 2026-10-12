package com.nutrikin.app.data

import io.github.jan.supabase.postgrest.from

/**
 * The same "anyone with the household id" / RLS-scoped read the iOS FamilyStore does against the
 * `members` table - see backend/migration_005_auth.sql's `is_household_member` policy. No
 * household-id filtering needed here: Postgres only returns rows this signed-in user is allowed to see.
 */
class FamilyRepository {
    suspend fun fetchMembers(): List<MemberDto> =
        supabase.from("members").select().decodeList()
}
