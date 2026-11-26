package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.DreamLinksApp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.data.Dream
import com.mydreamlinks.app.data.DreamFilter
import com.mydreamlinks.app.data.DreamInput
import com.mydreamlinks.app.data.DreamsResponse
import com.mydreamlinks.app.data.SymbolSpotter
import com.mydreamlinks.app.ui.Go
import com.mydreamlinks.app.ui.components.ChipTone
import com.mydreamlinks.app.ui.components.Chips
import com.mydreamlinks.app.ui.components.DreamCard
import com.mydreamlinks.app.ui.components.EmptyState
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.LoadBox
import com.mydreamlinks.app.ui.components.Loader
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.QuietButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink
import java.time.LocalDate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Toggles "I've dreamt this too" and patches the dream in whatever list holds it. */
private suspend fun resonate(api: Api, dream: Dream, patch: (Dream) -> Unit) {
    try {
        val r = api.resonate(dream.id)
        patch(dream.copy(resonated = r.on, resonance = r.count))
    } catch (_: ApiException) { /* leave it as it was */ }
}

private fun Loader<DreamsResponse>.patch(d: Dream) = update { res -> res.copy(dreams = res.dreams.map { if (it.id == d.id) d else it }) }

@Composable
fun FeedScreen(api: Api, go: Go) {
    var filter by rememberSaveable(stateSaver = DreamFilter.Saver) { mutableStateOf(DreamFilter()) }
    val loader = rememberLoader { api.feed(filter = filter) }
    // a new filter refetches in place, so the list (and the search box) stays on screen
    LaunchedEffect(Unit) { snapshotFlow { filter }.drop(1).collect { loader.reload() } }
    val scope = rememberCoroutineScope()
    LoadBox(loader) { res ->
        Page {
            item {
                PageHead("The commons", "What's everyone dreaming?") {
                    IconButton(onClick = go::search) { Icon(Icons.Filled.Search, "Find dreamers", tint = Ink.ink) }
                    IconButton(onClick = go::links) { Icon(Icons.Filled.AccountCircle, "Links", tint = Ink.ink) }
                    IconButton(onClick = go::settings) { Icon(Icons.Filled.Settings, "Account", tint = Ink.ink) }
                    LogoutButton(api)
                }
            }
            item { LinksStrip(api, go) }
            if (!res.pulse.isNullOrEmpty() && filter.isDefault) {
                item {
                    Sticker(color = Ink.night) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Eyebrow("Last 30 days", color = Ink.yellow)
                            res.pulse.forEach { p ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${p.dreamers} dreamers dreamt about ", color = Ink.card, style = MaterialTheme.typography.bodyMedium)
                                    Chips(listOf(p.symbol))
                                }
                            }
                        }
                    }
                }
            }
            item { FilterBar(filter, feed = true) { filter = it } }
            if (res.dreams.isEmpty() && !filter.isDefault) {
                item { NoMatches { filter = DreamFilter() } }
            } else if (res.dreams.isEmpty()) {
                item { EmptyState("It's quiet in here.", "No one you can see has shared a dream yet. Find dreamers to link with, or share one of your own.") }
            }
            items(res.dreams, key = { it.id }) { d ->
                DreamCard(d, onOpen = { go.dream(d.id) }, onOwner = { go.user(d.owner.id) }, onResonate = {
                    scope.launch { resonate(api, d) { loader.patch(it) } }
                })
            }
        }
    }
}

@Composable
fun JournalScreen(api: Api, go: Go) {
    var filter by rememberSaveable(stateSaver = DreamFilter.Saver) { mutableStateOf(DreamFilter()) }
    val loader = rememberLoader { api.journal(filter = filter) }
    LaunchedEffect(Unit) { snapshotFlow { filter }.drop(1).collect { loader.reload() } }
    LoadBox(loader) { res ->
        Page {
            item {
                PageHead("Journal", "Your dreams") {
                    IconButton(onClick = go::links) { Icon(Icons.Filled.AccountCircle, "Links", tint = Ink.ink) }
                    LogoutButton(api)
                }
            }
            if (res.dreams.isNotEmpty() || !filter.isDefault) {
                item { FilterBar(filter, feed = false) { filter = it } }
            }
            if (res.dreams.isEmpty() && !filter.isDefault) {
                item { NoMatches { filter = DreamFilter() } }
            } else if (res.dreams.isEmpty()) {
                item {
                    EmptyState("Nothing logged yet.", "Write down last night's dream while you still remember it.") {
                        PosterButton("Log a dream", onClick = go::newDream)
                    }
                }
            }
            items(res.dreams, key = { it.id }) { d -> DreamCard(d, onOpen = { go.dream(d.id) }) }
        }
    }
}

@Composable
fun DreamDetailScreen(api: Api, go: Go, id: Int) {
    val loader = rememberLoader(id) { api.dream(id) }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LoadBox(loader) { d ->
        Page {
            item { PageHead(if (d.own) "Your dream" else d.owner.username, "", onBack = go::back) }
            item {
                DreamCard(d.copy(), onOpen = {}, onOwner = { go.user(d.owner.id) }, onResonate = {
                    scope.launch { resonate(api, d) { new -> loader.update { new } } }
                })
            }
            if (d.tags.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("Tags")
                        Chips(d.tags) { ChipTone.Card }
                    }
                }
            }
            if (d.own) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PosterButton("Edit", onClick = { go.editDream(d.id) })
                        QuietButton("Delete", color = Ink.danger, onClick = { confirm = true })
                    }
                    error?.let { Text(it, color = Ink.danger) }
                }
            }
        }
        if (confirm) {
            AlertDialog(
                onDismissRequest = { confirm = false },
                title = { Text("Delete this dream?") },
                text = { Text("It's gone for good, along with its tags.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirm = false
                        scope.launch {
                            try { api.deleteDream(d.id); go.back() } catch (e: ApiException) { error = e.message }
                        }
                    }) { Text("Delete", color = Ink.danger) }
                },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep it") } },
            )
        }
    }
}

private val VISIBILITY = listOf("private" to "Just me", "friends" to "My links", "public" to "Everyone")

/** Log a new dream ([id] null) or edit one. Symbols are spotted on the phone as you type. */
@Composable
fun DreamEditorScreen(app: DreamLinksApp, go: Go, id: Int?) {
    val api = app.api
    // editing: load the dream; new: load the default visibility from settings
    val start = rememberLoader(id) {
        if (id != null) api.dream(id).let { DreamInput(it.title, it.text, it.date, it.visibility, it.tags) }
        else DreamInput("", "", LocalDate.now().toString(), api.me().defaultVisibility, emptyList())
    }
    var spotter by remember { mutableStateOf<SymbolSpotter?>(null) }
    LaunchedEffect(Unit) { spotter = runCatching { app.spotter() }.getOrNull() }

    LoadBox(start) { initial ->
        var title by remember { mutableStateOf(initial.title) }
        var text by remember { mutableStateOf(initial.text) }
        var date by remember { mutableStateOf(initial.date) }
        var visibility by remember { mutableStateOf(initial.visibility) }
        var tags by remember { mutableStateOf(initial.tags) }
        var tagDraft by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val spotted = remember(title, text, spotter) { spotter?.detect("$title $text").orEmpty() }

        fun addTag(t: String) {
            val clean = t.trim().take(25)
            if (clean.isNotEmpty() && tags.none { it.equals(clean, true) } && tags.size < 15) tags = tags + clean
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PageHead(if (id == null) "Log a dream" else "Edit dream", if (id == null) "What did you dream?" else "Edit your dream", onBack = if (id != null) go::back else null)
            Sticker(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { title = it.take(100) }, label = { Text("Title") }, singleLine = true, isError = "title" in errors,
                        supportingText = errors["title"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                    OutlinedTextField(date, { date = it }, label = { Text("Night of (YYYY-MM-DD)") }, singleLine = true, isError = "date" in errors,
                        supportingText = errors["date"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(text, { text = it.take(700) }, label = { Text("What happened?") }, minLines = 6, isError = "text" in errors,
                        supportingText = { Text(errors["text"] ?: "${700 - text.length} characters left") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))

                    if (spotted.isNotEmpty()) {
                        Eyebrow("Symbols spotted · tap to tag")
                        Chips(spotted, tone = { s -> if (tags.any { it.equals(s, true) }) ChipTone.Teal else ChipTone.Yellow }) { addTag(it) }
                    }

                    Eyebrow("Tags")
                    if (tags.isNotEmpty()) Chips(tags.map { "$it ×" }, tone = { ChipTone.Pink }) { t -> tags = tags - t.removeSuffix(" ×") }
                    OutlinedTextField(tagDraft, { v ->
                        if (v.endsWith(",")) { addTag(v.dropLast(1)); tagDraft = "" } else tagDraft = v
                    }, label = { Text("Add a tag, then a comma") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = "tags" in errors, supportingText = errors["tags"]?.let { { Text(it) } })

                    Eyebrow("Who can see it")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VISIBILITY.forEach { (value, label) ->
                            val on = visibility == value
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Ink.card else Ink.ink,
                                modifier = Modifier.clip(CircleShape).background(if (on) Ink.violet else Ink.card).border(2.dp, Ink.ink, CircleShape)
                                    .clickable { visibility = value }.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
            }
            error?.let { Text(it, color = Ink.danger) }
            PosterButton(if (busy) "Saving…" else if (id == null) "Save dream" else "Save changes", Modifier.fillMaxWidth(), enabled = !busy) {
                if (tagDraft.isNotBlank()) { addTag(tagDraft); tagDraft = "" }
                busy = true; error = null; errors = emptyMap()
                scope.launch {
                    try {
                        val input = DreamInput(title.trim(), text.trim(), date.trim(), visibility, tags)
                        val saved = if (id == null) api.createDream(input) else api.updateDream(id, input)
                        when {
                            id != null -> go.back()
                            runCatching { api.me().aiAnalysisEnabled }.getOrDefault(false) -> go.readNewDream()
                            else -> go.savedNewDream(saved.id)
                        }
                    } catch (e: ApiException) {
                        errors = e.fields; error = e.message
                    } finally {
                        busy = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
