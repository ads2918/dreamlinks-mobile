package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mydreamlinks.app.data.Api
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.theme.Ink

/** Page heading like the site's .dl-page-head: small mono eyebrow, big poster title with a pink drop. */
@Composable
fun PageHead(eyebrow: String, title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink.ink) }
            }
            Eyebrow(eyebrow, Modifier.weight(1f))
            actions()
        }
        if (title.isNotEmpty()) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall.copy(shadow = Shadow(Ink.pink, Offset(5f, 5f), 0f)),
            )
        }
    }
}

/** Header button that signs out after a quick check, so a stray tap doesn't. */
@Composable
fun LogoutButton(api: Api) {
    var asking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    IconButton(onClick = { asking = true }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Log out", tint = Ink.ink) }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("Log out?") },
            text = { Text("You'll need your email and password to get back in.") },
            confirmButton = { TextButton(onClick = { asking = false; scope.launch { api.logout() } }) { Text("Log out", color = Ink.danger) } },
            dismissButton = { TextButton(onClick = { asking = false }) { Text("Stay", color = Ink.ink) } },
            containerColor = Ink.card,
        )
    }
}

/** A scrolling page with the standard side gutter and room above the bottom bar. */
@Composable
fun Page(content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}
