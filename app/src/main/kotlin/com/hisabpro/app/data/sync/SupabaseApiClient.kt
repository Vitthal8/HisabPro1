package com.hisabpro.app.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * High-performance, offline-resilient Supabase PostgREST client for HisabPro.
 * Executes bulk upserts and delta pulls with Row Level Security (RLS) headers.
 */
class SupabaseApiClient(private val context: Context) {

    suspend fun upsertBatch(
        table: String,
        token: String,
        records: JSONArray,
        onConflict: String = "id"
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (records.length() == 0) return@withContext Result.success(0)

        // Sandbox / Test Mode handling when live Supabase project is not yet configured
        if (!SupabaseConfig.isLiveConfigured(context)) {
            kotlinx.coroutines.delay(150) // Simulate fast network sync
            return@withContext Result.success(records.length())
        }

        var connection: HttpURLConnection? = null
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(context)
            val anonKey = SupabaseConfig.getAnonKey(context)
            val urlString = "$projectUrl/rest/v1/$table?on_conflict=$onConflict"

            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Prefer", "resolution=merge-duplicates, return=minimal")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(records.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                Result.success(records.length())
            } else {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = errorStream?.use {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
                } ?: "Unknown error"
                Result.failure(Exception("PostgREST Error $statusCode on $table: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchDelta(
        table: String,
        token: String,
        sinceTimestampMillis: Long,
        limit: Int = 200
    ): Result<JSONArray> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isLiveConfigured(context)) {
            return@withContext Result.success(JSONArray())
        }

        var connection: HttpURLConnection? = null
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(context)
            val anonKey = SupabaseConfig.getAnonKey(context)

            // Convert epoch millis to ISO 8601 or compare updated_at
            val queryParam = if (sinceTimestampMillis > 0) {
                val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date(sinceTimestampMillis))
                "&updated_at=gt.${URLEncoder.encode(isoDate, "UTF-8")}"
            } else {
                ""
            }

            val urlString = "$projectUrl/rest/v1/$table?select=*&limit=$limit&order=updated_at.asc$queryParam"
            val url = URL(urlString)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val stream = connection.inputStream
                val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                val array = JSONArray(response)
                Result.success(array)
            } else {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = errorStream?.use {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
                } ?: "Unknown error"
                Result.failure(Exception("PostgREST Fetch Error $statusCode on $table: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun softDeleteBatch(
        table: String,
        token: String,
        ids: List<String>,
        deletedAtMillis: Long = System.currentTimeMillis()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(0)
        if (!SupabaseConfig.isLiveConfigured(context)) {
            return@withContext Result.success(ids.size)
        }

        var connection: HttpURLConnection? = null
        try {
            val projectUrl = SupabaseConfig.getProjectUrl(context)
            val anonKey = SupabaseConfig.getAnonKey(context)

            val inFilter = ids.joinToString(",")
            val urlString = "$projectUrl/rest/v1/$table?id=in.($inFilter)"

            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Prefer", "return=minimal")
            }

            val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(deletedAtMillis))

            val patchBody = JSONObject().apply {
                put("deleted_at", isoDate)
                put("updated_at", isoDate)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(patchBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                Result.success(ids.size)
            } else {
                Result.failure(Exception("PostgREST Soft Delete Error $statusCode on $table"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
