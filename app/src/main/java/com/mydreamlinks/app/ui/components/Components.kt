package com.mydreamlinks.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.data.Dream
import com.mydreamlinks.app.data.Moon
import com.mydreamlinks.app.ui.theme.Ink
import com.mydreamlinks.app.ui.theme.Mono
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/* ---------- loading data ---------- */

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

class Loader<T>(initial: Load<T>) {
    var state by mutableStateOf(initial)
    internal var tick by mutableIntStateOf(0)
    fun reload() { tick++ }
    /** Replace the loaded value in place, e.g. after toggling a button. */
    fun update(transform: (T) -> T) { (state as? Load.Ready)?.let { state = Load.Ready(transform(it.value)) } }
}

/** Runs [fetch] when the screen opens, when [keys] change and when reload() is called. */
@Composable
fun <T> rememberLoader(vararg keys: Any?, fetch: suspend () -> T): Loader<T> {
    val loader = remember(*keys) { Loader<T>(Load.Loading) }
    LaunchedEffect(loader, loader.tick) {
        if (loader.state !is Load.Ready) loader.state = Load.Loading
        loader.state = try {
            Load.Ready(fetch())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Load.Failed(e.message ?: "Something went wrong.")
        } catch (e: Exception) {
            Load.Failed("Something went wrong.")
        }
    }
    return loader
}

/** Shows a spinner, an error with a retry button, or [content]. */
@Composable
fun <T> LoadBox(loader: Loader<T>, content: @Composable (T) -> Unit) {
    when (val s = loader.state) {
        is Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink.pink) }
        is Load.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Sticker {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("That didn't load.", style = MaterialTheme.typography.headlineSmall)
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    PosterButton("Try again", onClick = loader::reload)
                }
            }
        }
        is Load.Ready -> content(s.value)
    }
}

/* ---------- poster pieces ---------- */

/** White card with a thick ink outline and a hard offset shadow, like the site's .dl-sticker. */
@Composable
fun Sticker(
    modifier: Modifier = Modifier,
    color: Color = Ink.card,
    shadow: Dp = 5.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.padding(end = shadow, bottom = shadow)) {
        Box(Modifier.matchParentSize().offset(shadow, shadow).background(Ink.ink, shape))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(color)
                .border(BorderStroke(2.5.dp, Ink.ink), shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content,
        )
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ink.muted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun PosterButton(text: String, modifier: Modifier = Modifier, color: Color = Ink.pink, textColor: Color = Ink.card, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(2.5.dp, Ink.ink),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor, disabledContainerColor = Ink.paper, disabledContentColor = Ink.muted),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, color: Color = Ink.ink, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, containerColor = Ink.card),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

enum class ChipTone(val bg: Color, val fg: Color) {
    Plain(Ink.paper, Ink.ink), Pink(Ink.pink, Ink.card), Teal(Ink.teal, Ink.card), Yellow(Ink.yellow, Ink.ink), Card(Ink.card, Ink.ink)
}

@Composable
fun Chip(text: String, tone: ChipTone = ChipTone.Plain, onClick: (() -> Unit)? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = tone.fg,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(tone.bg)
            .border(2.dp, Ink.ink, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(items: List<String>, tone: (String) -> ChipTone = { ChipTone.Plain }, onClick: ((String) -> Unit)? = null) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { s -> Chip(s, tone(s), onClick?.let { { it(s) } }) }
    }
}

@Composable
fun Avatar(url: String?, size: Dp = 40.dp) {
    val m = Modifier.size(size).clip(CircleShape).background(Ink.yellow).border(2.5.dp, Ink.ink, CircleShape)
    if (url == null) {
        Box(m, contentAlignment = Alignment.Center) { Text("☾", color = Ink.ink, style = MaterialTheme.typography.titleMedium) }
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = m)
    }
}

/**
 * The moon at phase [p] (0 new, 0.5 full): a lit half plus an ellipse that either hides part of it
 * (crescent) or adds to it (gibbous).
 */
@Composable
fun MoonIcon(p: Double, size: Dp = 22.dp, lit: Color = Ink.yellow, dark: Color = Ink.ink, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).semantics { contentDescription = Moon.NAMES[Moon.index(p)] }) {
        val r = this.size.minDimension / 2 - 1.5.dp.toPx()
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val k = kotlin.math.cos(2 * Math.PI * p).toFloat()
        val waxing = p < 0.5
        drawCircle(dark, r, c)
        val half = Path().apply {
            arcTo(Rect(c, r), if (waxing) -90f else 90f, 180f, true)
            close()
        }
        drawPath(half, lit)
        val ew = kotlin.math.abs(k) * r
        val ellipse = Rect(Offset(c.x - ew, c.y - r), Size(ew * 2, r * 2))
        drawOval(if (k > 0) dark else lit, ellipse.topLeft, ellipse.size)
        drawCircle(Ink.ink, r, c, style = Stroke(1.6.dp.toPx()))
    }
}

/** "2026-11-21" → "Nov 21, 2026" */
fun prettyDate(date: String): String = runCatching {
    LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}.getOrDefault(date)

/** One dream as a sticker card, like the site's feed. */
@Composable
fun DreamCard(
    dream: Dream,
    onOpen: () -> Unit,
    onOwner: (() -> Unit)? = null,
    onResonate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Sticker(modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.then(if (onOwner != null) Modifier.clickable(onClick = onOwner) else Modifier)) { Avatar(dream.owner.avatar, 38.dp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(dream.owner.username, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoonIcon(Moon.phase(dream.date), 14.dp)
                        Spacer(Modifier.width(5.dp))
                        Text("${prettyDate(dream.date)} · ${dream.moon.lowercase()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                when (dream.visibility) {
                    "friends" -> Chip("links", ChipTone.Teal)
                    "private" -> Chip("private", ChipTone.Card)
                }
            }
            Text(dream.title, style = MaterialTheme.typography.headlineSmall)
            Text(dream.text, style = MaterialTheme.typography.bodyLarge, maxLines = 8, overflow = TextOverflow.Ellipsis)
            if (dream.symbols.isNotEmpty()) Chips(dream.symbols.take(8))
            ResonanceButton(dream, onResonate)
        }
    }
}

@Composable
fun ResonanceButton(dream: Dream, onResonate: (() -> Unit)?) {
    val on = dream.resonated
    val label = if (dream.own) "${dream.resonance} dreamt this too" else "I've dreamt this · ${dream.resonance}"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (on) Ink.teal else if (dream.own) Ink.card else Ink.yellow)
            .border(2.dp, Ink.ink, CircleShape)
            .then(if (!dream.own && onResonate != null) Modifier.clickable(onClick = onResonate) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = if (on) Ink.card else Ink.ink, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) Ink.card else Ink.ink, fontFamily = Mono)
    }
}

@Composable
fun EmptyState(title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Sticker(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
            action?.invoke()
        }
    }
}

/** A horizontal bar row: label, filled bar, count. */
@Composable
fun BarRow(label: String, n: Int, max: Int, color: Color = Ink.pink) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(1f).height(16.dp).clip(CircleShape).background(Ink.paper).border(2.dp, Ink.ink, CircleShape)) {
            Box(Modifier.fillMaxWidth(n.toFloat() / maxOf(max, 1)).height(16.dp).background(color))
        }
        Text("$n", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(30.dp).padding(start = 8.dp))
    }
}
