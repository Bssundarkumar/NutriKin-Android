package com.nutrikin.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrikin.app.data.FamilyRepository
import com.nutrikin.app.data.MemberDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface FamilyUiState {
    data object Loading : FamilyUiState
    data class Loaded(val members: List<MemberDto>) : FamilyUiState
    data class Failed(val message: String) : FamilyUiState
}

class FamilyViewModel(private val repo: FamilyRepository = FamilyRepository()) : ViewModel() {
    private val _state = MutableStateFlow<FamilyUiState>(FamilyUiState.Loading)
    val state: StateFlow<FamilyUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = FamilyUiState.Loading
            _state.value = try {
                FamilyUiState.Loaded(repo.fetchMembers())
            } catch (e: Exception) {
                FamilyUiState.Failed(e.message ?: "Couldn't load your family.")
            }
        }
    }
}
