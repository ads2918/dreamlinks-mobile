package com.mydreamlinks.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("session")

/**
 * Keeps the sign-in token in the app's private storage. Backups are off in the manifest,
 * so the token never leaves the device. Signing out deletes it here and on the server.
 */
class TokenStore(private val context: Context) {
    private val key = stringPreferencesKey("token")

    val tokenFlow: Flow<String?> = context.store.data.map { it[key] }

    suspend fun token(): String? = tokenFlow.first()
    suspend fun save(token: String) { context.store.edit { it[key] = token } }
    suspend fun clear() { context.store.edit { it.remove(key) } }
}
