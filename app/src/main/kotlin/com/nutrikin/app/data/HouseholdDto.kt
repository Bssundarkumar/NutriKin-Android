package com.nutrikin.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape returned by the create_household/join_household RPCs (backend/migration_005_auth.sql and
 * later migrations) - same functions the iOS app calls from FamilyStore.createHousehold/joinHousehold. */
@Serializable
data class HouseholdDto(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class HouseholdUserRow(
    @SerialName("household_id") val householdId: String,
    val role: String,
)
