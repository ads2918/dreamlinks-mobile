package com.mydreamlinks.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A failed API call, with the server's message ready to show to the user. */
class ApiException(val status: Int, message: String, val fields: Map<String, String> = emptyMap()) : Exception(message)

/**
 * Talks to the site's api/v1 endpoints. Every call after sign-in carries the Bearer token.
 * A 401 means the token is gone (signed out elsewhere, or account removed): [onSignedOut] runs
 * so the app can return to the sign-in screen.
 */
class Api(
    siteUrl: String,
    private val tokens: TokenStore,
    private val onSignedOut: suspend () -> Unit,
) {
    private val base = siteUrl.trimEnd('/') + "/api/v1/"
    val siteUrl: String = siteUrl.trimEnd('/') + "/"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** For a reading written on the spot: the server may wait on the cron, then on Gemini. */
    private val slowHttp = http.newBuilder().readTimeout(90, TimeUnit.SECONDS).build()

    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /* ---- sign in ---- */

    suspend fun login(email: String, password: String): LoginResponse {
        val body = buildJsonObject {
            put("email", email); put("password", password); put("device", android.os.Build.MODEL ?: "Android")
        }
        val res: LoginResponse = call("POST", "auth/login", body, auth = false)
        tokens.save(res.token)
        return res
    }

    suspend fun logout() {
        try { call<JsonElement>("POST", "auth/logout") } catch (_: Exception) { /* signing out locally either way */ }
        tokens.clear()
    }

    /* ---- me ---- */

    suspend fun me(): Me = call("GET", "me")
    suspend fun updateMe(changes: JsonObject): Me = call("POST", "me", changes)

    /** Deletes the account and everything in it for good, then signs out. */
    suspend fun deleteAccount(password: String) {
        call<JsonElement>("POST", "me/delete", buildJsonObject { put("password", password) })
        tokens.clear()
    }

    /* ---- dreams ---- */

    suspend fun feed(page: Int = 1, filter: DreamFilter = DreamFilter()): DreamsResponse = call("GET", "feed?page=$page" + filter.query())
    suspend fun journal(page: Int = 1, filter: DreamFilter = DreamFilter()): DreamsResponse = call("GET", "dreams?page=$page" + filter.query())
    suspend fun dream(id: Int): Dream = call("GET", "dreams/$id")
    suspend fun createDream(input: DreamInput): Dream = call("POST", "dreams", json.encodeToJsonElement(DreamInput.serializer(), input))
    suspend fun updateDream(id: Int, input: DreamInput): Dream = call("PUT", "dreams/$id", json.encodeToJsonElement(DreamInput.serializer(), input))
    suspend fun deleteDream(id: Int) { call<JsonElement>("DELETE", "dreams/$id") }
    suspend fun resonate(id: Int): Resonance = call("POST", "dreams/$id/resonate")

    /* ---- people ---- */

    suspend fun profile(id: Int): ProfileResponse = call("GET", "users/$id")
    suspend fun search(q: String): SearchResponse = call("GET", "search?q=" + java.net.URLEncoder.encode(q, "UTF-8"))
    suspend fun links(): LinksResponse = call("GET", "links")
    suspend fun requestLink(userId: Int): LinkStateResponse = call("POST", "users/$userId/link")
    suspend fun answerLink(linkId: Int, accept: Boolean): LinksResponse = call("POST", "links/$linkId/" + if (accept) "accept" else "decline")
    suspend fun unlink(linkId: Int): LinksResponse = call("DELETE", "links/$linkId")
    suspend fun block(userId: Int): BlocksResponse = call("POST", "users/$userId/block")
    suspend fun blocks(): BlocksResponse = call("GET", "blocks")
    suspend fun unblock(blockId: Int): BlocksResponse = call("DELETE", "blocks/$blockId")

    /** Report a dream ([dreamId]) or a dreamer ([userId]). [reason] is a key of REPORT_REASONS. */
    suspend fun report(reason: String, note: String, dreamId: Int? = null, userId: Int? = null) {
        call<JsonElement>("POST", "reports", buildJsonObject {
            put("reason", reason); put("note", note)
            dreamId?.let { put("dream_id", it) }
            userId?.let { put("user_id", it) }
        })
    }

    /* ---- insights ---- */

    suspend fun dreamscape(with: Int? = null): Dreamscape = call("GET", "dreamscape" + (with?.let { "?with=$it" } ?: ""))
    suspend fun readings(): Readings = call("GET", "readings")
    /** Read waiting dreams now. status is done, none, capped, failed, busy or off. */
    suspend fun readNow(): ReadNow = call("POST", "readings/now", client = slowHttp)
    suspend fun symbols(): SymbolsResponse = call("GET", "symbols")

    /* ---- plumbing ---- */

    private suspend inline fun <reified T> call(method: String, path: String, body: JsonElement? = null, auth: Boolean = true, client: OkHttpClient = http): T =
        call(method, path, body, auth, serializer(), client)

    private suspend fun <T> call(method: String, path: String, body: JsonElement?, auth: Boolean, des: KSerializer<T>, client: OkHttpClient): T {
        val token = if (auth) tokens.token() else null
        val request = Request.Builder()
            .url((base + path).toHttpUrl())
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .method(method, when {
                body != null -> body.toString().toRequestBody(jsonType)
                method == "GET" || method == "DELETE" -> null
                else -> "{}".toRequestBody(jsonType)
            })
            .build()

        val (status, text) = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
            } catch (e: IOException) {
                throw ApiException(0, "Can't reach DreamLinks. Check your connection and try again.")
            }
        }
        if (status == 401 && auth) {
            tokens.clear()
            onSignedOut()
        }
        if (status !in 200..299) {
            val err = runCatching { json.decodeFromString(ApiError.serializer(), text) }.getOrNull()
            throw ApiException(status, err?.error ?: "Something went wrong ($status).", err?.fields ?: emptyMap())
        }
        return try {
            json.decodeFromString(des, text)
        } catch (e: Exception) {
            throw ApiException(status, "The server sent something the app didn't understand.")
        }
    }
}

/** Small helper for building settings updates. */
fun settings(vararg pairs: Pair<String, Any?>): JsonObject = JsonObject(pairs.associate { (k, v) ->
    k to when (v) {
        is Boolean -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        null -> kotlinx.serialization.json.JsonNull
        else -> JsonPrimitive(v.toString())
    }
})
