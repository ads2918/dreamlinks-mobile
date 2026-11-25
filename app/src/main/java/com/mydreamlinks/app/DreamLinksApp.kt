package com.mydreamlinks.app

import android.app.Application
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.SymbolSpotter
import com.mydreamlinks.app.data.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow

/** App-wide singletons: the API client and the sign-in token. */
class DreamLinksApp : Application() {
    lateinit var tokens: TokenStore
    lateinit var api: Api

    /** Fires when the server says the token is no longer valid. */
    val signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var spotter: SymbolSpotter? = null

    /** The symbol word list, fetched once per run. */
    suspend fun spotter(): SymbolSpotter = spotter ?: SymbolSpotter(api.symbols().symbols).also { spotter = it }

    override fun onCreate() {
        super.onCreate()
        tokens = TokenStore(this)
        api = Api(BuildConfig.SITE_URL, tokens) { signedOut.tryEmit(Unit) }
    }
}
