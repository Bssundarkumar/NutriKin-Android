package com.nutrikin.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrikin.app.data.FamilyRepository
import com.nutrikin.app.data.ProductService
import com.nutrikin.core.MemberScore
import com.nutrikin.core.Product
import com.nutrikin.core.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Result(val product: Product, val scores: List<MemberScore>) : ScanUiState
    data class Failed(val message: String) : ScanUiState
}

/**
 * Barcode lookup (manual entry - camera/ML Kit scanning isn't ported yet, see the README) against the
 * same Open Food Facts endpoint ProductService.swift uses, then scored by the already-ported, already-
 * tested core.ScoringEngine against every member of the signed-in household.
 */
class ScanViewModel(
    private val productService: ProductService = ProductService(),
    private val familyRepo: FamilyRepository = FamilyRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state

    fun lookUp(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _state.value = ScanUiState.Loading
            _state.value = try {
                val product = productService.fetch(trimmed)
                val members = familyRepo.fetchMembers()
                    .map { it.toCoreMember() }
                val scores = ScoringEngine().scoreFamily(product, members)
                ScanUiState.Result(product, scores)
            } catch (e: Exception) {
                ScanUiState.Failed(e.message ?: "Couldn't look that up. Try again.")
            }
        }
    }

    fun reset() {
        _state.value = ScanUiState.Idle
    }
}
