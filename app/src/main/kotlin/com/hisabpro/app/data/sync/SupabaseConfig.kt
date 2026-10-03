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

    val DEFAULT_PROJECT_URL: String get() = BuildConfig.SUPABASE_URL
    val DEFAULT_ANON_KEY: String get() = BuildConfig.SUPABASE_ANON_KEY

    fun getProjectUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROJECT_URL, null)?.ifBlank { null } ?: DEFAULT_PROJECT_URL
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANON_KEY, null)?.ifBlank { null } ?: DEFAULT_ANON_KEY
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PROJECT_URL, url.trim().trimEnd('/'))
            .putString(KEY_ANON_KEY, anonKey.trim())
            .apply()
    }
}
