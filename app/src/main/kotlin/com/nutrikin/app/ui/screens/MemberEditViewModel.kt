package com.nutrikin.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrikin.app.data.ConditionCodec
import com.nutrikin.app.data.FamilyRepository
import com.nutrikin.app.data.MemberDto
import com.nutrikin.app.data.NewMemberPayload
import com.nutrikin.core.Allergen
import com.nutrikin.core.Condition
import com.nutrikin.core.Sex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Mirrors MemberEditView.swift, scoped to the fields most load-bearing for scoring: name, age, sex,
 * height/weight, the three boolean conditions, and the built-in allergy set (ScoringEngine's hard block
 * reads exactly these). Deliberately not yet ported: custom conditions/allergies, daily goals,
 * pregnancy, "This is me" linking - see the Android repo README.
 *
 * Takes constructor args (which member - null means "new" - and which household), so it's built with
 * `remember { MemberEditViewModel(...) }` rather than the no-arg `viewModel()` Compose helper.
 */
class MemberEditViewModel(
    private val existing: MemberDto?,
    private val householdId: String,
    private val repo: FamilyRepository = FamilyRepository(),
) : ViewModel() {
    var name by mutableStateOf(existing?.name ?: "")
    var age by mutableStateOf(existing?.age?.toString() ?: "")
    var heightCm by mutableStateOf(existing?.heightCm?.toString() ?: "")
    var weightKg by mutableStateOf(existing?.weightKg?.toString() ?: "")
    var sex by mutableStateOf(existing?.sex?.let { s -> Sex.entries.find { it.name.equals(s, true) } })
    var isManagedByParent by mutableStateOf(existing?.isManagedByParent ?: false)
    var hasDiabetes by mutableStateOf(existing?.decodedConditions?.contains(Condition.Diabetes) ?: false)
    var hasHypertension by mutableStateOf(existing?.decodedConditions?.contains(Condition.Hypertension) ?: false)
    var hasHighCholesterol by mutableStateOf(existing?.decodedConditions?.contains(Condition.HighCholesterol) ?: false)
    var allergies by mutableStateOf(
        existing?.decodedConditions.orEmpty().filterIsInstance<Condition.Allergy>().map { it.allergen }.toSet(),
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun toggleAllergy(a: Allergen) {
        allergies = if (a in allergies) allergies - a else allergies + a
    }

    fun save() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val conditions = buildList {
                    if (hasDiabetes) add(Condition.Diabetes)
                    if (hasHypertension) add(Condition.Hypertension)
                    if (hasHighCholesterol) add(Condition.HighCholesterol)
                    allergies.forEach { add(Condition.Allergy(it)) }
                }
                val payload = NewMemberPayload(
                    householdId = householdId,
                    name = trimmed,
                    conditions = ConditionCodec.encode(conditions),
                    age = age.toIntOrNull(),
                    sex = sex?.name?.lowercase(),
                    heightCm = heightCm.toDoubleOrNull(),
                    weightKg = weightKg.toDoubleOrNull(),
                    isManagedByParent = isManagedByParent,
                )
                if (existing == null) repo.addMember(payload) else repo.updateMember(existing.id, payload)
                _saved.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't save. Try again."
            } finally {
                _isSaving.value = false
            }
        }
    }
}
