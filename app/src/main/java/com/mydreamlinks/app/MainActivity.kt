package com.mydreamlinks.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mydreamlinks.app.ui.AppNav
import com.mydreamlinks.app.ui.components.CodeRain
import com.mydreamlinks.app.ui.screens.LoginScreen
import com.mydreamlinks.app.ui.theme.DreamLinksTheme
import com.mydreamlinks.app.ui.theme.Ink

private object Unknown

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DreamLinksApp
        setContent {
            DreamLinksTheme {
                Root(app)
            }
        }
    }
}

@Composable
private fun Root(app: DreamLinksApp) {
    // Unknown until DataStore has been read, so a signed-in user never sees the login screen flash.
    val token by app.tokens.tokenFlow.collectAsState(initial = Unknown)
    Box(Modifier.fillMaxSize().background(Ink.paper)) {
        // the site's code rain runs behind every screen, login included
        CodeRain(Modifier.fillMaxSize())
        when (token) {
            Unknown -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Ink.pink)
            null -> LoginScreen(app.api)
            else -> AppNav(app)
        }
    }
}
