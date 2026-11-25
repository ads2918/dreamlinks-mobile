package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.data.settings
import com.mydreamlinks.app.ui.Go
import com.mydreamlinks.app.ui.components.ChipTone
import com.mydreamlinks.app.ui.components.Chips
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.LoadBox
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.QuietButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink
import kotlinx.coroutines.launch
import java.time.ZoneId

private val VIS = mapOf("private" to "Just me", "friends" to "My links", "public" to "Everyone")

@Composable
fun SettingsScreen(api: Api, go: Go) {
    val loader = rememberLoader { api.me() }
    val scope = rememberCoroutineScope()
    LoadBox(loader) { me ->
        var bio by remember { mutableStateOf(me.bio.orEmpty()) }
        var ai by remember { mutableStateOf(me.aiAnalysisEnabled) }
        var vis by remember { mutableStateOf(me.defaultVisibility) }
        var remind by remember { mutableStateOf(me.reminderEnabled) }
        var hour by remember { mutableStateOf(me.reminderHour) }
        var status by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        val tz = remember { ZoneId.systemDefault().id }

        Page {
            item { PageHead("Account", me.username, onBack = go::back) }
            item {
                Sticker(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(bio, { bio = it.take(200) }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Toggle("AI readings", "Each night your new dreams get read for patterns and symbols.", ai) { ai = it }
                        Eyebrow("New dreams are seen by")
                        Chips(VIS.values.toList(), tone = { if (VIS[vis] == it) ChipTone.Pink else ChipTone.Card }) { label ->
                            vis = VIS.entries.first { it.value == label }.key
                        }
                        Toggle("Morning reminder", "An email nudge to write down your dream, at the hour you pick ($tz).", remind) { remind = it }
                        if (remind) {
                            Chips((4..12).map { if (it == 12) "12 pm" else "$it am" }, tone = { if (it.startsWith("$hour ")) ChipTone.Yellow else ChipTone.Card }) { label ->
                                hour = label.substringBefore(" ").toInt()
                            }
                        }
                        status?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.startsWith("Saved")) Ink.teal else Ink.danger) }
                        PosterButton(if (busy) "Saving…" else "Save", Modifier.fillMaxWidth(), enabled = !busy) {
                            busy = true; status = null
                            scope.launch {
                                try {
                                    val saved = api.updateMe(settings(
                                        "bio" to bio, "ai_analysis_enabled" to ai, "default_dream_visibility" to vis,
                                        "reminder_enabled" to remind, "reminder_hour" to hour, "timezone" to tz,
                                    ))
                                    loader.update { saved }
                                    status = "Saved."
                                } catch (e: ApiException) {
                                    status = e.message
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Profile picture, username, email and password are changed on the website for now.", style = MaterialTheme.typography.bodySmall)
            }
            item {
                QuietButton("Log out", color = Ink.danger, onClick = { scope.launch { api.logout() } })
            }
        }
    }
}

@Composable
private fun Toggle(title: String, hint: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Ink.teal, checkedThumbColor = Ink.card, uncheckedBorderColor = Ink.ink))
    }
}
