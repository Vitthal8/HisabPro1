package com.hisabpro.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.sync.AuthState
import com.hisabpro.app.data.sync.CloudSyncManager
import com.hisabpro.app.data.sync.CloudSyncUiState
import com.hisabpro.app.data.sync.SupabaseAuthManager
import com.hisabpro.app.domain.subscription.SubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val syncManager = CloudSyncManager.getInstance(application)
    private val authManager = SupabaseAuthManager.getInstance(application)

    val syncState: StateFlow<CloudSyncUiState> = syncManager.syncState
    val authState: StateFlow<AuthState> = authManager.authState

    private val _loginInProgress = MutableStateFlow(false)
    val loginInProgress: StateFlow<Boolean> = _loginInProgress.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun triggerSync() {
        viewModelScope.launch {
            syncManager.triggerSync(isManual = true)
        }
    }

    fun requestPhoneOtp(phone: String) {
        if (phone.isBlank()) {
            _authError.value = "Please enter mobile number"
            return
        }
        viewModelScope.launch {
            _loginInProgress.value = true
            _authError.value = null
            val result = authManager.signInWithPhone(phone)
            _loginInProgress.value = false
            if (result.isSuccess) {
                _otpSent.value = true
            } else {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send OTP"
            }
        }
    }

    fun verifyPhoneOtp(phone: String, otp: String) {
        if (otp.isBlank()) {
            _authError.value = "Please enter OTP"
            return
        }
        viewModelScope.launch {
            _loginInProgress.value = true
            _authError.value = null
            val result = authManager.verifyPhoneOtp(phone, otp)
            _loginInProgress.value = false
            if (result.isSuccess) {
                _otpSent.value = false
                syncManager.triggerSync(isManual = true)
            } else {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Invalid OTP"
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Please enter email and password"
            return
        }
        viewModelScope.launch {
            _loginInProgress.value = true
            _authError.value = null
            val result = authManager.signInWithEmail(email, pass)
            _loginInProgress.value = false
            if (result.isSuccess) {
                syncManager.triggerSync(isManual = true)
            } else {
                _authError.value = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
            }
        }
    }

    fun quickConnectAccount(phoneOrEmail: String) {
        viewModelScope.launch {
            _loginInProgress.value = true
            _authError.value = null
            authManager.connectCloudAccount(phoneOrEmail)
            _loginInProgress.value = false
            syncManager.triggerSync(isManual = true)
        }
    }

    fun signOut() {
        authManager.signOut()
        _otpSent.value = false
        _authError.value = null
    }

    fun clearError() {
        _authError.value = null
    }
}
