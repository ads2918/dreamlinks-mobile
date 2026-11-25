package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.Dreamscape
import com.mydreamlinks.app.data.Graph
import com.mydreamlinks.app.data.Kin
import com.mydreamlinks.app.ui.Go
import com.mydreamlinks.app.ui.components.Avatar
import com.mydreamlinks.app.ui.components.BarRow
import com.mydreamlinks.app.ui.components.ChipTone
import com.mydreamlinks.app.ui.components.Chips
import com.mydreamlinks.app.ui.components.EmptyState
import com.mydreamlinks.app.ui.components.Eyebrow
import com.mydreamlinks.app.ui.components.LoadBox
import com.mydreamlinks.app.ui.components.MoonIcon
import com.mydreamlinks.app.ui.components.PosterButton
import com.mydreamlinks.app.ui.components.QuietButton
import com.mydreamlinks.app.ui.components.Sticker
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink
import com.mydreamlinks.app.ui.theme.Mono
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun DreamscapeScreen(api: Api, go: Go, with: Int?, onCompare: (Int?) -> Unit) {
    val loader = rememberLoader(with) { api.dreamscape(with) }
    LoadBox(loader) { d ->
        var symbol by remember(d) { mutableStateOf<String?>(null) }
        var phase by remember(d) { mutableStateOf<Int?>(null) }
        val comparing = d.compare != null
        Page {
            item { PageHead("Dreamscape", if (comparing) "You & ${d.compare!!.username}" else "Your symbol map") }
            d.compareNote?.let { note -> item { Text(note, style = MaterialTheme.typography.bodyMedium, color = Ink.muted) } }

            if (d.graph.nodes.isEmpty()) {
                item { EmptyState("No map yet.", "Log a few dreams and the symbols in them will show up here, joined when they appear together.") }
            } else {
                item {
                    Sticker(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            SymbolMap(d.graph, comparing, symbol) { symbol = if (symbol == it) null else it }
                            Legend(comparing, d.compare?.username)
                        }
                    }
                }
                symbol?.let { s -> item { SymbolPanel(d, s, comparing, go) { symbol = it } } }
            }

            d.compare?.let { c -> item { ComparePanel(c, onClear = { onCompare(null) }, onProfile = { go.user(c.id) }) } }

            if (d.topSymbols.isNotEmpty()) {
                item {
                    Panel("Top symbols") {
                        val top = d.topSymbols.maxOf { it.n }
                        d.topSymbols.take(8).forEach { BarRow(it.symbol, it.n, top) }
                    }
                }
            }

            item {
                Panel("Dreams by moon phase") {
                    MoonBars(d, phase) { phase = if (phase == it) null else it }
                    val p = phase
                    if (p == null) {
                        Text("Tap a phase to see what you dream about then.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        MoonDetail(d, p, go)
                    }
                }
            }

            if (d.kin.isNotEmpty()) {
                item {
                    Panel("Dreamers like you") {
                        d.kin.forEach { k -> KinRow(k, onOpen = { go.user(k.id) }, onCompare = { onCompare(k.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Sticker(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

/* ---------- the map ---------- */

private class Node(val id: String, val mine: Int, val theirs: Int, val spotted: Boolean, var x: Float, var y: Float) {
    val r = 9f + sqrt((mine + theirs).toFloat()) * 6f
    var vx = 0f
    var vy = 0f
}

private const val W = 600f
private const val H = 640f

/** The same force layout as dreamscape.js: push apart, pull linked symbols together, settle, then fit the box. */
private fun layout(g: Graph): Pair<List<Node>, List<Triple<Node, Node, Int>>> {
    val nodes = g.nodes.mapIndexed { i, n ->
        val a = i * 2.39996
        val r = 30 + 30 * sqrt(i.toDouble())
        Node(n.id, n.mine, n.theirs, n.spotted, (W / 2 + kotlin.math.cos(a) * r).toFloat(), (H / 2 + kotlin.math.sin(a) * r).toFloat())
    }
    val byId = nodes.associateBy { it.id }
    val links = g.links.mapNotNull { l -> val s = byId[l.source]; val t = byId[l.target]; if (s != null && t != null) Triple(s, t, l.weight) else null }

    for (step in 0 until 400) {
        val alpha = 1f - step / 400f
        for (i in nodes.indices) for (j in i + 1 until nodes.size) {
            val a = nodes[i]; val b = nodes[j]
            var dx = b.x - a.x; var dy = b.y - a.y
            val d2 = max(dx * dx + dy * dy, 0.01f); val d = sqrt(d2)
            var f = 6000f / d2
            val minD = a.r + b.r + 16f
            if (d < minD) f += (minD - d) * 0.5f
            dx /= d; dy /= d
            a.vx -= dx * f; a.vy -= dy * f; b.vx += dx * f; b.vy += dy * f
        }
        links.forEach { (s, t, w) ->
            var dx = t.x - s.x; var dy = t.y - s.y
            val d = max(sqrt(dx * dx + dy * dy), 0.01f)
            val target = 80f + s.r + t.r - min(w, 4) * 8f
            val f = (d - target) * 0.02f * min(w, 3)
            dx /= d; dy /= d
            s.vx += dx * f; s.vy += dy * f; t.vx -= dx * f; t.vy -= dy * f
        }
        nodes.forEach { n ->
            n.vx += (W / 2 - n.x) * 0.008f; n.vy += (H / 2 - n.y) * 0.008f
            n.x += (n.vx * alpha).coerceIn(-20f, 20f); n.y += (n.vy * alpha).coerceIn(-20f, 20f)
            n.vx *= 0.5f; n.vy *= 0.5f
        }
    }
    // stretch to fill the box, leaving room for labels
    val x0 = nodes.minOf { it.x }; val x1 = nodes.maxOf { it.x }; val y0 = nodes.minOf { it.y }; val y1 = nodes.maxOf { it.y }
    val padX = 50f; val padTop = 30f; val padBottom = 40f
    var sx = (W - padX * 2) / max(x1 - x0, 1f); var sy = (H - padTop - padBottom) / max(y1 - y0, 1f)
    sx = min(sx, sy * 1.6f); sy = min(sy, sx * 1.6f)
    val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2
    nodes.forEach { n ->
        n.x = W / 2 + (n.x - cx) * sx
        n.y = padTop + (H - padTop - padBottom) / 2 + (n.y - cy) * sy
    }
    return nodes to links
}

private fun fillFor(n: Node, comparing: Boolean): Color = when {
    comparing && n.mine > 0 && n.theirs > 0 -> Ink.yellow
    comparing && n.mine > 0 -> Ink.pink
    comparing -> Ink.teal
    n.spotted -> Ink.yellow
    else -> Ink.pink
}

@Composable
private fun SymbolMap(g: Graph, comparing: Boolean, selected: String?, onTap: (String) -> Unit) {
    val (nodes, links) = remember(g) { layout(g) }
    val maxW = remember(links) { max(links.maxOfOrNull { it.third } ?: 1, 1) }
    val measurer = rememberTextMeasurer()
    val near = remember(selected, links) {
        selected?.let { s -> links.filter { it.first.id == s || it.second.id == s }.flatMap { listOf(it.first.id, it.second.id) }.toSet() }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(W / H)
            .pointerInput(nodes) {
                detectTapGestures { p ->
                    val k = size.width / W
                    nodes.minByOrNull { n -> (n.x * k - p.x).let { it * it } + (n.y * k - p.y).let { it * it } }
                        ?.takeIf { n -> sqrt((n.x * k - p.x).let { it * it } + (n.y * k - p.y).let { it * it }) < (n.r + 14) * k }
                        ?.let { onTap(it.id) }
                }
            },
    ) {
        val k = size.width / W
        links.forEach { (s, t, w) ->
            val dim = near != null && !(s.id == selected || t.id == selected)
            drawLine(
                Ink.ink.copy(alpha = if (dim) 0.07f else 0.25f + (w.toFloat() / maxW) * 0.35f),
                Offset(s.x * k, s.y * k), Offset(t.x * k, t.y * k),
                strokeWidth = (1.5f + (w.toFloat() / maxW) * 5f) * k, cap = StrokeCap.Round,
            )
        }
        nodes.forEach { n ->
            val dim = near != null && n.id !in near && n.id != selected
            val c = Offset(n.x * k, n.y * k)
            val alpha = if (dim) 0.25f else 1f
            drawCircle(fillFor(n, comparing).copy(alpha = alpha), n.r * k, c)
            drawCircle(Ink.ink.copy(alpha = alpha), n.r * k, c, style = Stroke((if (n.id == selected) 4.5f else 2.5f) * k))
            val label = measurer.measure(n.id, TextStyle(fontFamily = Mono, fontSize = 11.sp, color = Ink.ink.copy(alpha = alpha), textAlign = TextAlign.Center))
            drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y + n.r * k + 2f))
        }
    }
}

@Composable
private fun Legend(comparing: Boolean, theirName: String?) {
    val items = if (comparing) listOf(Ink.pink to "only you", Ink.teal to "only ${theirName ?: "them"}", Ink.yellow to "both")
    else listOf(Ink.yellow to "spotted in the text", Ink.pink to "your tags")
    Row(Modifier.padding(top = 6.dp, start = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (c, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(12.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(c).border(1.5.dp, Ink.ink, RoundedCornerShape(6.dp)))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SymbolPanel(d: Dreamscape, s: String, comparing: Boolean, go: Go, onSymbol: (String) -> Unit) {
    val node = d.graph.nodes.first { it.id == s }
    val withCounts = d.graph.links.filter { it.source == s || it.target == s }
        .sortedByDescending { it.weight }.map { if (it.source == s) it.target else it.source }.take(8)
    val dreams = d.graph.dreams.filter { s in it.symbols }
    Panel(s) {
        Text(
            if (comparing) "In ${node.mine} of your dreams and ${node.theirs} of ${d.compare?.username}'s." else "In ${node.mine} ${if (node.mine == 1) "dream" else "dreams"}.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (withCounts.isNotEmpty()) {
            Eyebrow("Shows up with")
            Chips(withCounts, onClick = onSymbol)
        }
        Eyebrow("Dreams")
        dreams.take(12).forEach { gd ->
            Row(
                Modifier.fillMaxWidth().clickable { if (gd.side == "mine") go.dream(gd.id) else go.user(gd.owner) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(30.dp).background(if (gd.side == "mine") Ink.pink else Ink.teal))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(gd.title, style = MaterialTheme.typography.titleMedium)
                    Text(gd.date + if (comparing) (if (gd.side == "mine") " · you" else " · ${d.compare?.username}") else "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (dreams.size > 12) Text("and ${dreams.size - 12} more", style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------- moon ---------- */

@Composable
private fun MoonBars(d: Dreamscape, selected: Int?, onTap: (Int) -> Unit) {
    val top = max(d.moon.maxOfOrNull { it.count } ?: 1, 1)
    Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        d.moon.forEach { m ->
            val on = selected == m.phase
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Ink.yellow else Color.Transparent)
                    .clickable { onTap(m.phase) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(0.7f), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(max(m.count.toFloat() / top, 0.03f))
                            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(if (on) Ink.pink else Ink.violet)
                            .border(2.dp, Ink.ink, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp)),
                    )
                }
                Text("${m.count}", style = MaterialTheme.typography.labelMedium)
                MoonIcon(m.phase / 8.0, 18.dp)
            }
        }
    }
}

@Composable
private fun MoonDetail(d: Dreamscape, p: Int, go: Go) {
    val m = d.moon[p]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        Text(m.name, style = MaterialTheme.typography.headlineSmall)
        if (m.count == 0) {
            Text("No dreams logged under this moon yet.", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        Text("${m.count} ${if (m.count == 1) "dream" else "dreams"}, ${m.share}% of your journal.", style = MaterialTheme.typography.bodySmall)
        if (m.symbols.isNotEmpty()) {
            Eyebrow("Symbols under this moon")
            Chips(m.symbols.map { "${it.symbol} · ${it.n}" + if (it.standsOut) " ↑" else "" },
                tone = { label -> if (label.endsWith("↑")) ChipTone.Yellow else ChipTone.Plain })
            if (m.symbols.any { it.standsOut }) Text("Yellow ↑ means it comes up more under this moon than in your dreams overall.", style = MaterialTheme.typography.bodySmall)
        }
        Eyebrow("Dreams")
        m.dreams.forEach { md ->
            Text(
                md.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().clickable { go.dream(md.id) }.padding(vertical = 3.dp),
            )
        }
        if (m.count > m.dreams.size) Text("and ${m.count - m.dreams.size} more", style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------- people ---------- */

@Composable
private fun ComparePanel(c: Kin, onClear: () -> Unit, onProfile: () -> Unit) {
    Panel("${(c.similarity * 100).toInt()}% alike") {
        Text("Based on your dreams and the ${c.dreams} of ${c.username}'s you're allowed to see. Their private dreams are never counted.", style = MaterialTheme.typography.bodySmall)
        if (c.shared.isNotEmpty()) { Eyebrow("You both dream of"); Chips(c.shared) { ChipTone.Yellow } }
        if (c.onlyMine.isNotEmpty()) { Eyebrow("Only you"); Chips(c.onlyMine) { ChipTone.Pink } }
        if (c.onlyTheirs.isNotEmpty()) { Eyebrow("Only ${c.username}"); Chips(c.onlyTheirs) { ChipTone.Teal } }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietButton("Their profile", onClick = onProfile)
            QuietButton("Just my map", onClick = onClear)
        }
    }
}

@Composable
private fun KinRow(k: Kin, onOpen: () -> Unit, onCompare: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(onClick = onOpen)) { Avatar(k.avatar, 40.dp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(k.username, style = MaterialTheme.typography.titleMedium)
            Text("${(k.similarity * 100).toInt()}% alike" + if (k.shared.isNotEmpty()) " · " + k.shared.take(3).joinToString(", ") else "", style = MaterialTheme.typography.bodySmall)
        }
        PosterButton("Compare", color = Ink.yellow, textColor = Ink.ink, onClick = onCompare)
    }
}
