package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.data.LinkState
import com.mydreamlinks.app.data.LinkedPerson
import com.mydreamlinks.app.data.Person
import com.mydreamlinks.app.ui.Go
import com.mydreamlinks.app.ui.components.Avatar
import com.mydreamlinks.app.ui.components.Chip
import com.mydreamlinks.app.ui.components.ChipTone
import com.mydreamlinks.app.ui.components.DreamCard
import com.mydreamlinks.app.ui.components.EmptyState
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.Load
import com.mydreamlinks.app.ui.components.LoadBox
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.QuietButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(api: Api, go: Go, id: Int) {
    val loader = rememberLoader(id) { api.profile(id) }
    val scope = rememberCoroutineScope()
    var reporting by remember { mutableStateOf(false) }
    var blocking by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LoadBox(loader) { p ->
        val me = p.user.isSelf
        Page {
            item { PageHead("Dreamer", "", onBack = go::back) }
            item {
                Sticker(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(p.user.avatar, 96.dp)
                        Text(p.user.username, style = MaterialTheme.typography.headlineMedium)
                        p.user.bio?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink.muted) }
                        if (!me) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LinkButton(p.user.link) {
                                    scope.launch {
                                        runCatching { api.requestLink(id) }.onSuccess { r -> loader.update { it.copy(user = it.user.copy(link = r.link)) } }
                                    }
                                }
                                QuietButton("Compare", onClick = { go.compare(id) })
                            }
                            Row {
                                TextButton(onClick = { reporting = true }) { Text("Report", color = Ink.muted) }
                                TextButton(onClick = { blocking = true }) { Text("Block", color = Ink.muted) }
                            }
                            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.teal) }
                        }
                    }
                }
            }
            if (p.dreams.isEmpty()) item { EmptyState("Nothing to see yet.", "${p.user.username} hasn't shared any dreams you can see.") }
            items(p.dreams, key = { it.id }) { d ->
                DreamCard(d, onOpen = { go.dream(d.id) }, onResonate = {
                    scope.launch {
                        runCatching { api.resonate(d.id) }.onSuccess { r ->
                            loader.update { res -> res.copy(dreams = res.dreams.map { if (it.id == d.id) it.copy(resonated = r.on, resonance = r.count) else it }) }
                        }
                    }
                })
            }
        }
        if (reporting) {
            ReportDialog(api, p.user.username, userId = id, onDismiss = { reporting = false }, onSent = {
                reporting = false; notice = "Thanks for telling us. You can also block them."
            })
        }
        if (blocking) {
            BlockDialog(api, id, p.user.username, onDismiss = { blocking = false }, onBlocked = { blocking = false; go.back() })
        }
    }
}

@Composable
private fun LinkButton(link: LinkState?, onRequest: () -> Unit) {
    when (link?.state) {
        "linked" -> Chip("Linked", ChipTone.Teal)
        "requested" -> Chip("Request sent", ChipTone.Card)
        "incoming" -> Chip("Wants to link · see Links", ChipTone.Yellow)
        else -> PosterButton("Link", onClick = onRequest)
    }
}

@Composable
fun SearchScreen(api: Api, go: Go) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Person>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        if (q.trim().length < 2) { results = emptyList(); searched = false; return@LaunchedEffect }
        delay(300) // wait for typing to pause
        results = runCatching { api.search(q.trim()).users }.getOrDefault(emptyList())
        searched = true
    }
    Page {
        item { PageHead("Find dreamers", "Who's out there?", onBack = go::back) }
        item { OutlinedTextField(q, { q = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (searched && results.isEmpty()) item { Text("No dreamers match \"$q\".", style = MaterialTheme.typography.bodyMedium, color = Ink.muted) }
        items(results, key = { it.id }) { u ->
            PersonRow(u.username, u.avatar, onOpen = { go.user(u.id) }) {
                when (u.link?.state) {
                    "linked" -> Chip("Linked", ChipTone.Teal)
                    "requested" -> Chip("Sent", ChipTone.Card)
                    "incoming" -> Chip("Asked you", ChipTone.Yellow)
                    else -> {}
                }
            }
        }
    }
}

/** The feed's row of linked dreamers, with a nudge when someone is asking to link. */
@Composable
fun LinksStrip(api: Api, go: Go) {
    val loader = rememberLoader { api.links() }
    val links = (loader.state as? Load.Ready)?.value ?: return
    Sticker(Modifier.fillMaxWidth(), shadow = 3.dp) {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Your links · ${links.linked.size}", Modifier.weight(1f))
                if (links.pending.isNotEmpty()) {
                    Chip("${links.pending.size} asking to link", ChipTone.Yellow, onClick = go::links)
                    Spacer(Modifier.width(6.dp))
                }
                Text("See all", style = MaterialTheme.typography.labelMedium, color = Ink.ink, modifier = Modifier.clickable(onClick = go::links).padding(4.dp))
            }
            if (links.linked.isEmpty()) {
                Text("No links yet. Find dreamers and link up to see the dreams they share with their links.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.muted,
                    modifier = Modifier.padding(horizontal = 16.dp).clickable(onClick = go::search))
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(links.linked, key = { it.linkId }) { l ->
                        Column(Modifier.width(76.dp).clickable { go.user(l.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(l.avatar, 52.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(l.username, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinksScreen(api: Api, go: Go) {
    val loader = rememberLoader { api.links() }
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    fun act(block: suspend () -> com.mydreamlinks.app.data.LinksResponse) {
        scope.launch {
            try { val r = block(); loader.update { r } } catch (e: ApiException) { error = e.message }
        }
    }
    LoadBox(loader) { links ->
        Page {
            item { PageHead("Links", "Your people", onBack = go::back) }
            error?.let { item { Text(it, color = Ink.danger) } }
            if (links.pending.isNotEmpty()) {
                item { Text("Asking to link", style = MaterialTheme.typography.headlineSmall) }
                items(links.pending, key = { "p" + it.linkId }) { l ->
                    PersonRow(l.username, l.avatar, onOpen = { go.user(l.id) }) {
                        TextButton(onClick = { act { api.answerLink(l.linkId, true) } }) { Text("Accept", color = Ink.teal) }
                        TextButton(onClick = { act { api.answerLink(l.linkId, false) } }) { Text("Decline", color = Ink.danger) }
                    }
                }
            }
            item { Text("Linked", style = MaterialTheme.typography.headlineSmall) }
            if (links.linked.isEmpty()) {
                item { EmptyState("No links yet.", "Linked dreamers can see the dreams you share with your links.") { PosterButton("Find dreamers", onClick = go::search) } }
            }
            items(links.linked, key = { "l" + it.linkId }) { l: LinkedPerson ->
                PersonRow(l.username, l.avatar, onOpen = { go.user(l.id) }) {
                    TextButton(onClick = { go.compare(l.id) }) { Text("Compare", color = Ink.ink) }
                    TextButton(onClick = { act { api.unlink(l.linkId) } }) { Text("Unlink", color = Ink.muted) }
                }
            }
            item { BlockedList(api) }
        }
    }
}

@Composable
fun PersonRow(name: String, avatar: String?, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    Sticker(Modifier.fillMaxWidth(), shadow = 3.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatar, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            actions()
        }
    }
}
