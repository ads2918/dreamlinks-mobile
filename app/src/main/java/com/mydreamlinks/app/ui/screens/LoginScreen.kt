package com.mydreamlinks.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.MoonPortal
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.theme.Ink
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(api: Api) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun submit() {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try {
                api.login(email.trim(), password)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        MoonPortal(190.dp)
        Spacer(Modifier.height(14.dp))
        Text("DreamLinks", style = MaterialTheme.typography.displaySmall.copy(color = Ink.pink, shadow = Shadow(Ink.ink, Offset(4f, 4f), 0f)))
        Text("Your dream journal, shared with the people you trust.", style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        Spacer(Modifier.height(26.dp))
        Sticker(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Log in")
                OutlinedTextField(
                    email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { submit() }),
                    trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "hide" else "show") } },
                )
                error?.let { Text(it, color = Ink.danger, style = MaterialTheme.typography.bodyMedium) }
                PosterButton(if (busy) "Logging in…" else "Log in", Modifier.fillMaxWidth(), enabled = !busy && email.isNotBlank() && password.isNotEmpty(), onClick = ::submit)
            }
        }
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(api.siteUrl + "user/register")))
        }) { Text("New here? Join on the website", color = Ink.ink) }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(api.siteUrl + "user/forgot-password")))
        }) { Text("Forgot your password?", color = Ink.muted) }
    }
}
