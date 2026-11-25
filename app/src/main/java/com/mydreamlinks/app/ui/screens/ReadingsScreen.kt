package com.mydreamlinks.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.Reading
import com.mydreamlinks.app.ui.Go
import com.mydreamlinks.app.ui.components.BarRow
import com.mydreamlinks.app.ui.components.EmptyState
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.LoadBox
import com.mydreamlinks.app.ui.components.MoonIcon
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.components.prettyDate
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink

@Composable
fun ReadingsScreen(api: Api, go: Go) {
    val loader = rememberLoader { api.readings() }
    LoadBox(loader) { r ->
        val latest = r.readings.firstOrNull()
        Page {
            item { PageHead("Readings", "What your dreams are saying") }
            item {
                Sticker(color = Ink.night) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row {
                            Stat("Next reading", when { !r.enabled -> "Readings are off"; r.waiting > 0 -> "${r.waiting} ${if (r.waiting == 1) "dream" else "dreams"} waiting"; else -> "All caught up" }, Modifier.weight(1f))
                            Stat("Arrives", if (r.enabled && r.waiting > 0) "Overnight, ~3 am" else "After your next dream", Modifier.weight(1f))
                        }
                        Row {
                            Column(Modifier.weight(1f)) {
                                Eyebrow("Tonight", color = Ink.yellow)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MoonIcon(r.tonight.phase, 18.dp, dark = Ink.night)
                                    Spacer(Modifier.width(6.dp))
                                    Text(r.tonight.name.lowercase(), color = Ink.card, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Stat("So far", "${r.readings.size} ${if (r.readings.size == 1) "reading" else "readings"} ·${r.dreamCount} dreams", Modifier.weight(1f))
                        }
                    }
                }
            }
            if (!r.enabled) {
                item { EmptyState("AI readings are switched off.", "Turn them on and each night your new dreams get read for patterns, symbols and what the sky was doing.") { PosterButton("Turn on readings", onClick = go::settings) } }
            }
            if (latest != null) {
                item { LatestReading(latest) }
            } else if (r.enabled) {
                item { EmptyState("Your first reading is on its way.", "Log a dream or two. Readings are written overnight from the dreams you added that day.") }
            }
            if (r.readings.size > 1) {
                item { Text("Earlier readings", style = MaterialTheme.typography.headlineMedium) }
                items(r.readings.drop(1)) { EarlierReading(it) }
            }
            if (r.topSymbols.isNotEmpty()) {
                item {
                    Sticker(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Keeps coming back", style = MaterialTheme.typography.headlineSmall)
                            val top = r.topSymbols.maxOf { it.n }
                            r.topSymbols.forEach { BarRow(it.symbol, it.n, top) }
                            Text("Counted from your own dreams and tags.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Eyebrow(label, color = Ink.yellow)
        Text(value, color = Ink.card, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LatestReading(r: Reading) {
    Box {
        Sticker(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Eyebrow("Latest reading")
                Text(r.overview, style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
                ReadingSections(r)
            }
        }
        // the date stamp, tilted like the site's
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp)
                .rotate(3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Ink.yellow)
                .border(2.5.dp, Ink.ink, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // white lit side, or a bright moon vanishes into the yellow stamp
            MoonIcon(r.moonPhase, 26.dp, lit = Ink.card)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(prettyDate(r.date), style = MaterialTheme.typography.labelMedium)
                Text(r.moon.lowercase(), style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            }
        }
    }
}

@Composable
private fun ReadingSections(r: Reading) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Triple("Patterns", Ink.pink, r.patterns), Triple("Symbols", Ink.yellow, r.symbols), Triple("In the sky", Ink.teal, r.celestial))
            .filter { it.third.isNotEmpty() }
            .forEach { (label, color, lines) -> Section(label, color, lines) }
    }
}

@Composable
private fun Section(label: String, color: Color, lines: List<String>) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Ink.card).border(2.5.dp, Ink.ink, shape)) {
        Eyebrow(label, color = if (color == Ink.yellow) Ink.ink else Ink.card,
            modifier = Modifier.fillMaxWidth().background(color).padding(horizontal = 14.dp, vertical = 8.dp))
        Box(Modifier.fillMaxWidth().height(2.5.dp).background(Ink.ink))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEachIndexed { i, line ->
                Row {
                    Text("${i + 1}.", style = MaterialTheme.typography.labelMedium, color = Ink.muted, modifier = Modifier.width(22.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun EarlierReading(r: Reading) {
    var open by remember { mutableStateOf(false) }
    Sticker(Modifier.fillMaxWidth().animateContentSize(), shadow = 3.dp, onClick = { open = !open }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonIcon(r.moonPhase, 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(prettyDate(r.date), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(10.dp))
                Text(if (open) "" else r.overview, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (open) "▴" else "▾", style = MaterialTheme.typography.titleMedium)
            }
            if (open) {
                Text(r.overview, style = MaterialTheme.typography.bodyMedium)
                ReadingSections(r)
            }
        }
    }
}
