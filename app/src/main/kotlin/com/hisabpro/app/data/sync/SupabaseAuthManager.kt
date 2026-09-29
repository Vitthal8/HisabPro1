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

    private fun loadSavedSession() {
        val userId = prefs.getString("user_id", null)
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)

        if (!userId.isNullOrBlank() && !token.isNullOrBlank()) {
            if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
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
                    isDemoAccount = false
                )
                _authState.value = AuthState.Authenticated(session)
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
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

    fun isAuthenticated(): Boolean {
        return getCurrentSession() != null
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(appContext)
            val anonKey = SupabaseConfig.getAnonKey(appContext)
            if (SupabaseConfig.isLiveConfigured(appContext).not() && (projectUrl.contains("placeholder") || anonKey.contains("placeholder"))) {
                return@withContext Result.failure(Exception("Supabase project is not configured with valid credentials."))
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
            if (SupabaseConfig.isLiveConfigured(appContext).not() && (projectUrl.contains("placeholder") || anonKey.contains("placeholder"))) {
                return@withContext Result.failure(Exception("Supabase project is not configured with valid credentials."))
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
            if (SupabaseConfig.isLiveConfigured(appContext).not() && (projectUrl.contains("placeholder") || anonKey.contains("placeholder"))) {
                return@withContext Result.failure(Exception("Supabase project is not configured with valid credentials."))
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
            if (SupabaseConfig.isLiveConfigured(appContext).not() && (projectUrl.contains("placeholder") || anonKey.contains("placeholder"))) {
                return@withContext Result.failure(Exception("Supabase project is not configured with valid credentials."))
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
            if (SupabaseConfig.isLiveConfigured(appContext).not() && (projectUrl.contains("placeholder") || anonKey.contains("placeholder"))) {
                return@withContext Result.failure(Exception("Supabase project is not configured with valid credentials."))
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

    fun signOut() {
        prefs.edit().clear().apply()
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
