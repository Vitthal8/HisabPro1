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

        // Check if Supabase project is configured
        if (!SupabaseConfig.isLiveConfigured(context)) {
            return@withContext Result.failure(Exception("Supabase Cloud is not configured."))
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
        batchSize: Int = 200,
        userId: String? = null
    ): Result<JSONArray> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isLiveConfigured(context)) {
            return@withContext Result.failure(Exception("Supabase Cloud is not configured."))
        }

        // Build queryParam:
        // Do NOT append updated_at filter if table is "invoice_items" or if sinceTimestampMillis <= 0 (fresh install)
        val queryParamBuilder = StringBuilder()
        if (!table.equals("invoice_items", ignoreCase = true) && sinceTimestampMillis > 0L) {
            queryParamBuilder.append("&updated_at=gt.$sinceTimestampMillis")
        }
        if (!userId.isNullOrBlank()) {
            queryParamBuilder.append("&user_id=eq.$userId")
        }
        val queryParam = queryParamBuilder.toString()

        val allRecords = JSONArray()
        var offset = 0

        while (true) {
            var connection: HttpURLConnection? = null
            try {
                val projectUrl = SupabaseConfig.getProjectUrl(context)
                val anonKey = SupabaseConfig.getAnonKey(context)

                val urlString = "$projectUrl/rest/v1/$table?select=*&limit=$batchSize&offset=$offset&order=id.asc$queryParam"
                val url = URL(urlString)

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("apikey", anonKey)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                }

                val isAuthAttached = token.isNotBlank()
                val isAuthenticated = isAuthAttached && !token.contains("placeholder")
                val usingUpdatedAtFilter = !table.equals("invoice_items", ignoreCase = true) && sinceTimestampMillis > 0L
                android.util.Log.d(
                    "SupabaseApiClient",
                    "SUPABASE_FETCH table=$table url=$urlString sinceTimestampMillis=$sinceTimestampMillis usingUpdatedAtFilter=$usingUpdatedAtFilter authAttached=$isAuthAttached authenticated=$isAuthenticated"
                )

                val statusCode = connection.responseCode
                android.util.Log.d("SupabaseApiClient", "SUPABASE_RESPONSE table=$table status=$statusCode")

                if (statusCode in 200..299) {
                    val stream = connection.inputStream
                    val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                    val batch = JSONArray(response)
                    for (i in 0 until batch.length()) {
                        allRecords.put(batch.getJSONObject(i))
                    }
                    android.util.Log.d("SupabaseApiClient", "SUPABASE_SUCCESS table=$table recordCount=${allRecords.length()}")
                    if (batch.length() < batchSize) {
                        break
                    }
                    offset += batchSize
                } else {
                    val errorStream = connection.errorStream ?: connection.inputStream
                    val errorBody = errorStream?.use {
                        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
                    } ?: "Unknown error"
                    android.util.Log.e("SupabaseApiClient", "SUPABASE_ERROR table=$table status=$statusCode error=$errorBody")
                    return@withContext Result.failure(Exception("PostgREST Fetch Error $statusCode on $table: $errorBody"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
        Result.success(allRecords)
    }

    suspend fun softDeleteBatch(
        table: String,
        token: String,
        ids: List<String>,
        deletedAtMillis: Long = System.currentTimeMillis()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(0)
        if (!SupabaseConfig.isLiveConfigured(context)) {
            return@withContext Result.failure(Exception("Supabase Cloud is not configured."))
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

            val patchBody = JSONObject().apply {
                put("deleted_at", deletedAtMillis)
                if (!table.equals("invoice_items", ignoreCase = true)) {
                    put("updated_at", deletedAtMillis)
                }
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
