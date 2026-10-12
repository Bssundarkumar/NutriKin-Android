package com.nutrikin.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Same Supabase project as the iOS app (FamilyFoodScanner/Services/Core/SupabaseConfig.swift) - this is
 * the whole point: one backend, one set of households/members/tables, shared by both platforms. The
 * anon key is safe to ship (it identifies the project, not a user; RLS in backend/schema.sql and the
 * migrations protects every table) - never put a service_role key here, same rule as iOS.
 */
private const val SUPABASE_URL = "https://guihtuotohipipagkbri.supabase.co"
private const val SUPABASE_ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd1aWh0dW90b2hpcGlwYWdrYnJpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAxOTE3MzMsImV4cCI6MjEwNTc2NzczM30.0YhZF6e72akhWZ9NJBUvoZfwxEyYfPCnVLb5ye-OlLQ"

val supabase = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_ANON_KEY,
) {
    install(Postgrest)
    install(Auth) {
        scheme = "nutrikin"
        host = "login-callback"
    }
}
