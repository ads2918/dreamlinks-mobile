package com.mydreamlinks.app.data

import java.time.LocalDate
import java.time.ZoneId

/** Moon phase as a fraction of the synodic month: 0 new, 0.5 full. Same maths as the site's MoonPhase.php. */
object Moon {
    private const val SYNODIC = 29.530588853
    private const val REF_NEW_MOON_MS = 947_182_440_000L // 2000-01-06 18:14 UTC

    val NAMES = listOf(
        "New moon", "Waxing crescent", "First quarter", "Waxing gibbous",
        "Full moon", "Waning gibbous", "Last quarter", "Waning crescent",
    )

    fun phase(epochMs: Long): Double {
        val days = (epochMs - REF_NEW_MOON_MS) / 86_400_000.0
        return ((days % SYNODIC) + SYNODIC) % SYNODIC / SYNODIC
    }

    /** For a calendar date, measured at local noon like the site does. */
    fun phase(date: String): Double = runCatching {
        val d = LocalDate.parse(date.take(10))
        phase(d.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }.getOrDefault(0.0)

    fun index(p: Double): Int = Math.floorMod(Math.floor(p * 8 + 0.5).toInt(), 8)
}
