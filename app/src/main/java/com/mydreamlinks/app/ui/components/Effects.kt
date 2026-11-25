package com.mydreamlinks.app.ui.components

import android.graphics.Paint
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.mydreamlinks.app.R
import com.mydreamlinks.app.data.Moon
import com.mydreamlinks.app.ui.theme.Ink
import com.mydreamlinks.app.ui.theme.Mono
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** True when the phone has animations turned off, the app's version of prefers-reduced-motion. */
@Composable
fun rememberStill(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/* ---------- code rain: the site's background, always a trickle, heavy bursts every so often ---------- */

private const val CHARS = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン0123456789☾✦"
// aliens turn up most
private val EMOJI = listOf("👽", "👽", "👽", "👾", "🛸", "🌙", "✨", "🔮", "🪐", "👁️", "🍄", "💫", "🌀", "⭐")
private val HEADS = listOf(Ink.pink, Ink.teal, Ink.violet, Ink.orange)

// far columns are small, slow and faint; near ones are big, fast and strong
private class Layer(val size: Float, val alpha: Float, val stepMin: Int, val stepMax: Int)
private val LAYERS = listOf(Layer(11f, 0.45f, 90, 140), Layer(15f, 0.75f, 55, 95), Layer(21f, 1f, 35, 60))

private class Drop(val x: Float, val layer: Layer, val len: Int, val step: Long, var last: Long, val head: Color, val chars: ArrayDeque<String>) {
    var row = 0
}

private fun randomChar() = if (Random.nextFloat() < 0.05f) EMOJI.random() else CHARS[Random.nextInt(CHARS.length)].toString()

private class Rain {
    val drops = mutableListOf<Drop>()
    var burstUntil = 0L
    var nextBurst = -1L
    var scan = -1f

    fun spawn(now: Long, width: Float, density: Float) {
        val layer = LAYERS[if (Random.nextFloat() < 0.45f) 0 else if (Random.nextFloat() < 0.7f) 1 else 2]
        val x = Random.nextFloat() * width
        if (drops.any { abs(it.x - x) < layer.size * density && it.row < it.len + 3 }) return
        val len = 10 + Random.nextInt(20)
        val chars = ArrayDeque<String>().apply { repeat(len) { add(randomChar()) } }
        val step = (layer.stepMin + Random.nextFloat() * (layer.stepMax - layer.stepMin)).toLong()
        drops += Drop(x, layer, len, step, now, HEADS.random(), chars)
    }

    fun advance(now: Long, width: Float, height: Float, density: Float) {
        if (nextBurst < 0) nextBurst = now + 1200
        if (now > nextBurst) {
            burstUntil = now + 4000 + Random.nextLong(4000)
            nextBurst = burstUntil + 4000 + Random.nextLong(5000)
            scan = 0f
        }
        val bursting = now < burstUntil
        if (Random.nextFloat() < (if (bursting) 0.9f else 0.06f)) spawn(now, width, density)
        if (bursting && Random.nextFloat() < 0.4f) spawn(now, width, density)
        drops.removeAll { (it.row - it.len) * it.layer.size * density * 1.15f >= height }
        drops.sortBy { it.layer.size }
        drops.forEach { d ->
            if (now - d.last > d.step) {
                d.row++; d.last = now
                d.chars.addFirst(randomChar()); d.chars.removeLast()
                if (Random.nextFloat() < 0.35f) d.chars[Random.nextInt(d.len)] = randomChar()
            }
        }
    }
}

@Composable
fun CodeRain(modifier: Modifier = Modifier) {
    if (rememberStill()) return
    val context = LocalContext.current
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.dm_mono_medium)
        }
    }
    val rain = remember { Rain() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { now = it } }

    Canvas(modifier) {
        if (now == 0L) return@Canvas
        rain.advance(now, size.width, size.height, density)
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            rain.drops.forEach { d ->
                val textSize = d.layer.size * density
                val lh = textSize * 1.15f
                paint.textSize = textSize
                d.chars.forEachIndexed { i, ch ->
                    val y = (d.row - i) * lh
                    if (y < 0 || y > size.height + lh) return@forEachIndexed
                    val alpha = when {
                        // color emoji ignore the paint color, so they fade with alpha; kept stronger so they stand out
                        ch.length > 1 -> (if (i < 3) 1f else maxOf(0.55f, 0.9f * (1 - i.toFloat() / d.len))) * (0.4f + 0.6f * d.layer.alpha)
                        i == 0 -> 0.9f * d.layer.alpha
                        i < 3 -> 0.7f * d.layer.alpha
                        else -> 0.32f * (1 - i.toFloat() / d.len) * d.layer.alpha
                    }
                    val color = when {
                        ch.length > 1 || i == 0 -> Color.White
                        i < 3 -> d.head
                        else -> Ink.ink
                    }
                    paint.color = color.copy(alpha = alpha).toArgb()
                    c.drawText(ch, d.x, y, paint)
                }
            }
        }
        // a faint scan band sweeps down at the start of each burst
        if (rain.scan >= 0) {
            val band = 60.dp.toPx()
            val sy = rain.scan * (size.height + band * 2) - band
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Ink.violet.copy(alpha = 0.12f), Color.Transparent), sy - band, sy + band),
                Offset(0f, sy - band), Size(size.width, band * 2),
            )
            rain.scan += 0.012f
            if (rain.scan > 1) rain.scan = -1f
        }
    }
}

/* ---------- moon portal: tonight's moon inside melting rings of color, like the site's login poster ---------- */

private val RING = listOf(Ink.pink, Ink.orange, Ink.yellow, Ink.teal, Ink.violet)

@Composable
fun MoonPortal(size: Dp, modifier: Modifier = Modifier) {
    val still = rememberStill()
    val phase = remember { Moon.phase(System.currentTimeMillis()) }
    var t by remember { mutableFloatStateOf(3f) }
    if (!still) {
        LaunchedEffect(Unit) {
            val t0 = withFrameMillis { it }
            while (true) withFrameMillis { t = (it - t0) / 1000f }
        }
    }
    Box(modifier.size(size + 6.dp)) {
        Box(Modifier.size(size).offset(6.dp, 6.dp).background(Ink.ink, CircleShape))
        Box(Modifier.size(size).clip(CircleShape).background(Ink.violet).border(3.dp, Ink.ink, CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val r = w / 2 * 1.08f
                val n = 13
                val c = center
                val s = (t * 0.12f) % 1f
                val k = floor(t * 0.12f).toInt()
                for (i in -1 until n) {
                    val base = r * (1 - (i - s) / n)
                    if (base <= 0) continue
                    val path = Path()
                    var a = 0f
                    while (a <= 2 * PI + 0.01) {
                        val rr = base + sin(a * 5 + t * 0.9f + i * 0.8f) * w * 0.018f * (base / r)
                        val x = c.x + cos(a) * rr
                        val y = c.y + sin(a) * rr
                        if (a == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        a += 0.04f
                    }
                    path.close()
                    drawPath(path, RING[(((i + k) % 5) + 5) % 5])
                    drawPath(path, Ink.ink, style = Stroke(w * 0.008f))
                }
                drawCircle(Ink.ink, w * 0.19f, c)
            }
            MoonIcon(phase, size * 0.32f)
            Text(
                "Tonight · " + Moon.NAMES[Moon.index(phase)].lowercase(),
                style = TextStyle(fontFamily = Mono, fontSize = 10.sp, color = Ink.ink),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = size * 0.12f)
                    .background(Ink.card, CircleShape)
                    .border(2.dp, Ink.ink, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}
