package com.hisabpro.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.sync.SupabaseAuthManager
import com.hisabpro.app.data.sync.SupabaseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER, FORGOT_PASSWORD
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = SupabaseAuthManager.getInstance(application)

    val isCloudConfigured = MutableStateFlow(SupabaseConfig.isLiveConfigured(application))

    private val _showConfigDialog = MutableStateFlow(false)
    val showConfigDialog: StateFlow<Boolean> = _showConfigDialog.asStateFlow()

    private val _authMode = MutableStateFlow(AuthMode.LOGIN)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun updateEmail(value: String) { _email.value = value }
    fun updatePassword(value: String) { _password.value = value }
    fun updateConfirmPassword(value: String) { _confirmPassword.value = value }
    fun updateBusinessName(value: String) { _businessName.value = value }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun signIn() {
        val em = _email.value.trim()
        val pass = _password.value
        if (em.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter email and password."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            val result = authManager.signInWithEmail(em, pass)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed. Please check credentials."
            }
        }
    }

    fun signUp() {
        val em = _email.value.trim()
        val pass = _password.value
        val confirmPass = _confirmPassword.value
        val bizName = _businessName.value.trim()

        if (em.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please fill in all required fields."
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }
        if (pass != confirmPass) {
            _errorMessage.value = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            val result = authManager.signUpWithEmail(em, pass)
            _isLoading.value = false
            if (result.isSuccess) {
                if (bizName.isNotBlank()) {
                    authManager.setPendingRegistrationBusinessName(em, bizName)
                }
                _successMessage.value = "Registration successful! Please check your email to verify your account before logging in."
                _authMode.value = AuthMode.LOGIN
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Registration failed. Account may already exist."
            }
        }
    }

    fun sendPasswordReset() {
        val em = _email.value.trim()
        if (em.isBlank()) {
            _errorMessage.value = "Please enter your registered email address."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            val result = authManager.resetPasswordForEmail(em)
            _isLoading.value = false
            if (result.isSuccess) {
                _successMessage.value = "Password recovery instructions sent to your email."
                _authMode.value = AuthMode.LOGIN
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Password recovery failed."
            }
        }
    }

    fun openConfigDialog() {
        _showConfigDialog.value = true
    }

    fun closeConfigDialog() {
        _showConfigDialog.value = false
    }

    fun getSavedSupabaseUrl(): String = SupabaseConfig.getProjectUrl(getApplication())

    fun getSavedSupabaseAnonKey(): String = SupabaseConfig.getAnonKey(getApplication())

    fun saveSupabaseConfig(url: String, anonKey: String) {
        SupabaseConfig.setCustomConfig(getApplication(), url, anonKey)
        isCloudConfigured.value = SupabaseConfig.isLiveConfigured(getApplication())
        _errorMessage.value = null
        _successMessage.value = "Supabase server configured successfully! You can now sign in or register."
        _showConfigDialog.value = false
    }

    fun continueOffline() {
        authManager.continueOffline()
    }
}
