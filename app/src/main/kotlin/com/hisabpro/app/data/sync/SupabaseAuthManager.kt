package com.hisabpro.app.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class UserSession(
    val userId: String,
    val email: String? = null,
    val phone: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long = 0L,
    val isDemoAccount: Boolean = false
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

class SupabaseAuthManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("hisabpro_supabase_auth_v1", Context.MODE_PRIVATE)

    private val sessionPrefs: SharedPreferences =
        appContext.getSharedPreferences("hisabpro_user_session_v1", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        loadSavedSession()
    }

    companion object {
        @Volatile
        private var INSTANCE: SupabaseAuthManager? = null

        fun getInstance(context: Context): SupabaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getLastActiveUserId(): String? = sessionPrefs.getString("last_active_user_id", null)

    fun setLastActiveUserId(userId: String?) {
        if (userId == null) sessionPrefs.edit().remove("last_active_user_id").apply()
        else sessionPrefs.edit().putString("last_active_user_id", userId).apply()
    }

    fun getLastActiveUserEmail(): String? = sessionPrefs.getString("last_active_user_email", null)

    fun setLastActiveUserEmail(email: String?) {
        if (email == null) sessionPrefs.edit().remove("last_active_user_email").apply()
        else sessionPrefs.edit().putString("last_active_user_email", email).apply()
    }

    fun getPendingRegistrationBusinessName(email: String?): String? {
        if (email.isNullOrBlank()) return null
        return sessionPrefs.getString("pending_biz_${email.trim().lowercase()}", null)
    }

    fun setPendingRegistrationBusinessName(email: String, bizName: String) {
        sessionPrefs.edit().putString("pending_biz_${email.trim().lowercase()}", bizName.trim()).apply()
    }

    fun clearPendingRegistrationBusinessName(email: String?) {
        if (!email.isNullOrBlank()) {
            sessionPrefs.edit().remove("pending_biz_${email.trim().lowercase()}").apply()
        }
    }

    fun isDifferentUser(session: UserSession): Boolean {
        val lastUserId = getLastActiveUserId()
        if (lastUserId != null && lastUserId != session.userId) {
            return true
        }
        val lastEmail = getLastActiveUserEmail()
        if (lastEmail != null && !session.email.isNullOrBlank() && !lastEmail.equals(session.email, ignoreCase = true)) {
            return true
        }
        return false
    }

    private fun loadSavedSession() {
        val userId = prefs.getString("user_id", null)
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        val isDemo = prefs.getBoolean("is_demo_account", false)

        if (!userId.isNullOrBlank() && !token.isNullOrBlank()) {
            if (!isDemo && expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                // Expired session - clear credentials for security
                signOut()
            } else {
                val session = UserSession(
                    userId = userId,
                    email = prefs.getString("user_email", null),
                    phone = prefs.getString("user_phone", null),
                    accessToken = token,
                    refreshToken = prefs.getString("refresh_token", null),
                    expiresAt = expiresAt,
                    isDemoAccount = isDemo
                )
                _authState.value = AuthState.Authenticated(session)
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun continueOffline(): UserSession {
        val offlineSession = UserSession(
            userId = "offline_user_local",
            email = "local@offline.hisabpro",
            phone = null,
            accessToken = "offline_local_token",
            refreshToken = null,
            expiresAt = 0L,
            isDemoAccount = true
        )
        saveSession(offlineSession)
        _authState.value = AuthState.Authenticated(offlineSession)
        return offlineSession
    }

    fun getCurrentSession(): UserSession? {
        val state = _authState.value
        return if (state is AuthState.Authenticated) {
            val session = state.session
            if (session.expiresAt > 0 && System.currentTimeMillis() > session.expiresAt) {
                signOut()
                null
            } else {
                session
            }
        } else null
    }

    suspend fun refreshTokenIfNeeded(): UserSession? = withContext(Dispatchers.IO) {
        val state = _authState.value
        if (state !is AuthState.Authenticated) return@withContext null
        val session = state.session
        if (session.expiresAt > 0 && System.currentTimeMillis() >= session.expiresAt - 60_000L) {
            val refreshToken = session.refreshToken
            if (refreshToken.isNullOrBlank()) {
                signOut()
                return@withContext null
            }
            try {
                val projectUrl = SupabaseConfig.getProjectUrl(appContext)
                val anonKey = SupabaseConfig.getAnonKey(appContext)
                val authUrl = "$projectUrl/auth/v1/token?grant_type=refresh_token"
                val payload = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }
                val response = executeAuthPost(authUrl, anonKey, payload.toString())
                if (response.isSuccess) {
                    val json = JSONObject(response.getOrThrow())
                    val accessToken = json.getString("access_token")
                    val newRefreshToken = json.optString("refresh_token", refreshToken)
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userObj = json.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userEmail = userObj.optString("email", session.email)
                    val userPhone = userObj.optString("phone", session.phone)

                    val newSession = UserSession(
                        userId = userId,
                        email = userEmail,
                        phone = userPhone,
                        accessToken = accessToken,
                        refreshToken = newRefreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
                        isDemoAccount = false
                    )
                    saveSession(newSession)
                    _authState.value = AuthState.Authenticated(newSession)
                    return@withContext newSession
                } else {
                    signOut()
                    return@withContext null
                }
            } catch (e: Exception) {
                signOut()
                return@withContext null
            }
        }
        return@withContext session
    }

    fun isAuthenticated(): Boolean {
        return getCurrentSession() != null
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (!SupabaseConfig.isLiveConfigured(appContext)) {
                return@withContext Result.failure(
                    Exception("Supabase Cloud is not configured. Please tap 'Configure Server' below to enter your Supabase Project URL and Anon Key, or tap 'Continue Offline' to use HisabPro locally.")
                )
            }

            val authUrl = "$projectUrl/auth/v1/token?grant_type=password"

            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }

            val response = executeAuthPost(authUrl, anonKey, payload.toString())
            if (response.isSuccess) {
                val json = JSONObject(response.getOrThrow())
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.optString("email", email)
                val userPhone = if (userObj.has("phone") && !userObj.isNull("phone")) userObj.optString("phone").takeIf { it.isNotBlank() } else null

                val session = UserSession(
                    userId = userId,
                    email = userEmail,
                    phone = userPhone,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
                    isDemoAccount = false
                )

                saveSession(session)
                _authState.value = AuthState.Authenticated(session)
                Result.success(session)
            } else {
                Result.failure(Exception(response.exceptionOrNull()?.message ?: "Authentication failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithPhone(phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (!SupabaseConfig.isLiveConfigured(appContext)) {
                return@withContext Result.failure(
                    Exception("Supabase Cloud is not configured. Please tap 'Configure Server' below to enter your Supabase Project URL and Anon Key, or tap 'Continue Offline'.")
                )
            }

            val authUrl = "$projectUrl/auth/v1/otp"

            val cleanPhone = if (!phone.startsWith("+91") && phone.length == 10) "+91$phone" else phone
            val payload = JSONObject().apply {
                put("phone", cleanPhone)
            }

            val response = executeAuthPost(authUrl, anonKey, payload.toString())
            if (response.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.exceptionOrNull()?.message ?: "Failed to send OTP."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPhoneOtp(phone: String, token: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanPhone = if (!phone.startsWith("+91") && phone.length == 10) "+91$phone" else phone
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (!SupabaseConfig.isLiveConfigured(appContext)) {
                return@withContext Result.failure(
                    Exception("Supabase Cloud is not configured. Please tap 'Configure Server' below to enter your Supabase Project URL and Anon Key, or tap 'Continue Offline'.")
                )
            }

            val authUrl = "$projectUrl/auth/v1/verify"

            val payload = JSONObject().apply {
                put("type", "sms")
                put("phone", cleanPhone)
                put("token", token.trim())
            }

            val response = executeAuthPost(authUrl, anonKey, payload.toString())
            if (response.isSuccess) {
                val json = JSONObject(response.getOrThrow())
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")

                val session = UserSession(
                    userId = userId,
                    email = null,
                    phone = cleanPhone,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
                    isDemoAccount = false
                )
                saveSession(session)
                _authState.value = AuthState.Authenticated(session)
                Result.success(session)
            } else {
                Result.failure(Exception(response.exceptionOrNull()?.message ?: "OTP verification failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (!SupabaseConfig.isLiveConfigured(appContext)) {
                return@withContext Result.failure(
                    Exception("Supabase Cloud is not configured. Please tap 'Configure Server' below to enter your Supabase Project URL and Anon Key, or tap 'Continue Offline' to use HisabPro locally.")
                )
            }

            val authUrl = "$projectUrl/auth/v1/signup"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }

            val response = executeAuthPost(authUrl, anonKey, payload.toString())
            if (response.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.exceptionOrNull()?.message ?: "Registration failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPasswordForEmail(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (!SupabaseConfig.isLiveConfigured(appContext)) {
                return@withContext Result.failure(
                    Exception("Supabase Cloud is not configured. Please tap 'Configure Server' below to enter your Supabase Project URL and Anon Key, or tap 'Continue Offline'.")
                )
            }

            val authUrl = "$projectUrl/auth/v1/recover"
            val payload = JSONObject().apply {
                put("email", email.trim())
            }

            val response = executeAuthPost(authUrl, anonKey, payload.toString())
            if (response.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.exceptionOrNull()?.message ?: "Password recovery failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(clearDeviceData: Boolean = false) {
        prefs.edit().clear().apply()
        if (clearDeviceData) {
            sessionPrefs.edit().clear().apply()
        }
        _authState.value = AuthState.Unauthenticated
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putString("user_id", session.userId)
            .putString("user_email", session.email)
            .putString("user_phone", session.phone)
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putLong("expires_at", session.expiresAt)
            .putBoolean("is_demo_account", session.isDemoAccount)
            .apply()
    }

    private fun executeAuthPost(urlString: String, apiKey: String, body: String): Result<String> {
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            return Result.failure(
                Exception("Supabase server URL is missing or invalid. Please tap 'Configure Server' to enter your Supabase Project URL (e.g. https://xyz.supabase.co), or tap 'Continue Offline'.")
            )
        }
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (statusCode in 200..299) {
                Result.success(response)
            } else {
                Result.failure(Exception("HTTP $statusCode: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
