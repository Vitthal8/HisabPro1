package com.hisabpro.app.data.sync

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration and secure credential manager for Supabase Cloud Sync.
 * Stores standard public Supabase Anon key & Project URL.
 * NEVER stores Supabase service_role keys.
 */
object SupabaseConfig {

    private const val PREFS_NAME = "hisabpro_supabase_config_v1"
    private const val KEY_PROJECT_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    // Default Supabase project configuration (can be updated dynamically or via build config)
    const val DEFAULT_PROJECT_URL = "https://hisabpro-cloud-sync.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhpc2FicHJvLXN5bmMiLCJyb2xlIjoiYW5vbiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoyMDAwMDAwMDAwfQ.public-anon-key-placeholder"

    fun getProjectUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROJECT_URL, DEFAULT_PROJECT_URL) ?: DEFAULT_PROJECT_URL
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANON_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY
    }

    fun isLiveConfigured(context: Context): Boolean {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() && 
               !url.contains("hisabpro-cloud-sync.supabase.co") && 
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
