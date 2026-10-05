package com.hisabpro.app.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.BuildConfig

/**
 * Configuration and secure credential manager for Supabase Cloud Sync.
 * Standard public Supabase Anon key & Project URL are loaded securely from BuildConfig
 * (read from local.properties or environment, defaulting to empty string for offline-first).
 * NEVER stores or exposes Supabase service_role keys.
 */
object SupabaseConfig {

    private const val PREFS_NAME = "hisabpro_supabase_config_v1"
    private const val KEY_PROJECT_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    const val HARDCODED_PROJECT_URL = "https://yadqiswozvcusgkcxsgy.supabase.co"
    const val HARDCODED_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlhZHFpc3dvenZjdXNna2N4c2d5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NzMwODEsImV4cCI6MjEwNjI0OTA4MX0.PRr-f86_zbOQ0Lp70FwV1Kf9gtl1JnL7NY45uUya_Jg"

    val DEFAULT_PROJECT_URL: String get() = if (BuildConfig.SUPABASE_URL.isNotBlank() && !BuildConfig.SUPABASE_URL.contains("placeholder")) BuildConfig.SUPABASE_URL else HARDCODED_PROJECT_URL
    val DEFAULT_ANON_KEY: String get() = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && !BuildConfig.SUPABASE_ANON_KEY.contains("placeholder")) BuildConfig.SUPABASE_ANON_KEY else HARDCODED_ANON_KEY

    fun getProjectUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PROJECT_URL, null)?.ifBlank { null }
        return if (saved != null && !saved.contains("placeholder")) saved else DEFAULT_PROJECT_URL
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ANON_KEY, null)?.ifBlank { null }
        return if (saved != null && !saved.contains("placeholder")) saved else DEFAULT_ANON_KEY
    }

    fun isLiveConfigured(context: Context): Boolean {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() &&
               !url.contains("placeholder") &&
               key.isNotBlank() &&
               !key.contains("placeholder")
    }

    fun setCustomConfig(context: Context, url: String, anonKey: String) {
        val trimmed = url.trim().trimEnd('/')
        val cleanUrl = if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PROJECT_URL, cleanUrl)
            .putString(KEY_ANON_KEY, anonKey.trim())
            .apply()
    }

    fun clearCustomConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PROJECT_URL)
            .remove(KEY_ANON_KEY)
            .apply()
    }
}
