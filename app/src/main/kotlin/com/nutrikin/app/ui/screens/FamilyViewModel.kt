package com.nutrikin.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrikin.app.data.FamilyRepository
import com.nutrikin.app.data.HouseholdDto
import com.nutrikin.app.data.MemberDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface FamilyUiState {
    data object Loading : FamilyUiState
    /** Signed in, but this account hasn't created or joined a household yet - same first-run state
     * SignInView.swift's onboarding (Create family / Join with invite code) handles on iOS. */
    data object NeedsHousehold : FamilyUiState
    data class Loaded(val householdId: String, val inviteCode: String, val members: List<MemberDto>) : FamilyUiState
    data class Failed(val message: String) : FamilyUiState
}

class FamilyViewModel(private val repo: FamilyRepository = FamilyRepository()) : ViewModel() {
    private val _state = MutableStateFlow<FamilyUiState>(FamilyUiState.Loading)
    val state: StateFlow<FamilyUiState> = _state

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = FamilyUiState.Loading
            _state.value = try {
                val householdId = repo.currentHouseholdId()
                if (householdId == null) {
                    FamilyUiState.NeedsHousehold
                } else {
                    val members = repo.fetchMembers()
                    // inviteCode isn't carried by a plain members fetch; a real screen would also read
                    // the household row's invite_code. Left blank here - this scaffold's create/join
                    // actions already surface the code at the moment it matters (right after creating).
                    FamilyUiState.Loaded(householdId, inviteCode = "", members = members)
                }
            } catch (e: Exception) {
                FamilyUiState.Failed(e.message ?: "Couldn't load your family.")
            }
        }
    }

    fun createHousehold(name: String) = runAction {
        val household = repo.createHousehold(name)
        applyHousehold(household)
    }

    fun joinHousehold(code: String) = runAction {
        val household = repo.joinHousehold(code)
        applyHousehold(household)
    }

    private suspend fun applyHousehold(household: HouseholdDto) {
        _state.value = FamilyUiState.Loaded(household.id, household.inviteCode, repo.fetchMembers())
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isWorking.value = true
            _actionError.value = null
            try {
                block()
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Something went wrong. Try again."
            } finally {
                _isWorking.value = false
            }
        }
    }
}
