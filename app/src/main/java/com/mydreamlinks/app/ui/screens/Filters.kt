package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.DreamFilter
import com.mydreamlinks.app.ui.components.Chip
import com.mydreamlinks.app.ui.components.ChipTone
import com.mydreamlinks.app.ui.components.EmptyState
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.theme.Ink
import kotlinx.coroutines.delay

private val SORTS = listOf("newest" to "Newest", "oldest" to "Oldest", "resonance" to "Most dreamt")
private val SINCE = listOf(0 to "Any time", 7 to "Past week", 30 to "Past month", 365 to "Past year")
private val FROM = listOf("" to "Everyone", "links" to "My links")
private val VISIBILITY = listOf("" to "All dreams", "public" to "Public", "friends" to "Links only", "private" to "Private")

/** A search box and three dropdown chips above a dream list. [feed] picks who/visibility for the third. */
@Composable
fun FilterBar(filter: DreamFilter, feed: Boolean, onChange: (DreamFilter) -> Unit) {
    var text by rememberSaveable { mutableStateOf(filter.q) }
    // wait for typing to pause before asking the server
    LaunchedEffect(text) {
        if (text.trim() == filter.q) return@LaunchedEffect
        delay(350)
        onChange(filter.copy(q = text.trim()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            text, { text = it },
            placeholder = { Text(if (feed) "Search dreams and tags" else "Search your dreams") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Ink.muted) },
            trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(Icons.Filled.Clear, "Clear search", tint = Ink.muted) } },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Ink.card, unfocusedContainerColor = Ink.card),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DropChip(SORTS, filter.sort) { onChange(filter.copy(sort = it)) }
            DropChip(SINCE, filter.since) { onChange(filter.copy(since = it)) }
            if (feed) DropChip(FROM, filter.from) { onChange(filter.copy(from = it)) }
            else DropChip(VISIBILITY, filter.visibility) { onChange(filter.copy(visibility = it)) }
            if (!filter.isDefault) Chip("Reset", ChipTone.Card) { text = ""; onChange(DreamFilter()) }
        }
    }
}

/** A chip showing the current choice; tapping it opens the list. Yellow once moved off the first option. */
@Composable
private fun <T> DropChip(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        val label = options.firstOrNull { it.first == selected }?.second ?: options.first().second
        Chip("$label ▾", if (selected == options.first().first) ChipTone.Card else ChipTone.Yellow) { open = true }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Ink.card) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(if (value == selected) "✓ $name" else name) },
                    onClick = { open = false; onPick(value) },
                )
            }
        }
    }
}

/** When the filters leave nothing to show. */
@Composable
fun NoMatches(onReset: () -> Unit) {
    EmptyState("No dreams match.", "Try other words, a longer time span or fewer filters.") {
        PosterButton("Reset filters", onClick = onReset)
    }
}
