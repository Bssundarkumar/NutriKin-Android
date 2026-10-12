package com.nutrikin.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrikin.app.data.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Mirrors the "Continue with Google" path on SignInView.swift - same Supabase Auth project, so an
 * account that signed in on iOS with Google can sign in here with the same Google account and land in
 * the same household. (Apple Sign-In and email+password, also offered on iOS, are not wired up here
 * yet; Google is the one that's natively at home on both platforms.)
 */
class AuthViewModel : ViewModel() {
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isSigningIn.value = true
            _error.value = null
            try {
                supabase.auth.signInWith(Google)
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't sign in. Try again."
            } finally {
                _isSigningIn.value = false
            }
        }
    }
}
