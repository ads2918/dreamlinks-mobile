package com.mydreamlinks.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes real responses recorded from the local server (src/test/resources/api) with the
 * same Json settings the app uses, so a change in the PHP API that would break the app fails here.
 * Re-record them after changing the API.
 */
class ApiShapesTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private fun <T> load(name: String, s: KSerializer<T>): T {
        val text = javaClass.classLoader!!.getResource("api/$name.json")!!.readText()
        return json.decodeFromString(s, text)
    }

    @Test fun login() {
        val r = load("login", LoginResponse.serializer())
        assertEquals(64, r.token.length)
        assertEquals("nightswimmer", r.user.username)
    }

    @Test fun me() = assertEquals("nightswimmer", load("me", Me.serializer()).username)

    @Test fun feed() {
        val r = load("feed", DreamsResponse.serializer())
        assertTrue(r.dreams.isNotEmpty())
        assertNotNull(r.pulse)
        assertTrue(r.dreams.all { it.owner.username.isNotEmpty() && it.moon.isNotEmpty() })
    }

    @Test fun journal() {
        val r = load("dreams", DreamsResponse.serializer())
        assertTrue(r.dreams.all { it.own })
        assertTrue(r.dreams.any { it.visibility == "private" })
    }

    @Test fun dream() = assertTrue(load("dream", Dream.serializer()).own)

    @Test fun profile() {
        val r = load("profile", ProfileResponse.serializer())
        assertEquals("linked", r.user.link?.state)
        assertTrue(!r.user.isSelf)
        assertTrue(r.dreams.none { it.visibility == "private" })
    }

    @Test fun search() = assertEquals("moth_and_lamp", load("search", SearchResponse.serializer()).users.single().username)

    @Test fun links() = assertEquals(1, load("links", LinksResponse.serializer()).linked.size)

    @Test fun dreamscape() {
        val d = load("dreamscape", Dreamscape.serializer())
        assertEquals(8, d.moon.size)
        assertTrue(d.graph.nodes.isNotEmpty())
        assertTrue(d.graph.links.all { l -> d.graph.nodes.any { it.id == l.source } && d.graph.nodes.any { it.id == l.target } })
        assertEquals(null, d.compare)
    }

    @Test fun compare() {
        val d = load("compare", Dreamscape.serializer())
        val c = d.compare!!
        assertEquals("moth_and_lamp", c.username)
        assertTrue(c.similarity in 0.0..1.0)
        assertTrue(d.graph.nodes.any { it.theirs > 0 })
    }

    @Test fun readings() {
        val r = load("readings", Readings.serializer())
        assertTrue(r.enabled)
        assertEquals("Feelings rising.", r.readings.single().overview)
        assertTrue(r.tonight.phase in 0.0..1.0)
    }

    @Test fun symbolsCompileAndMatchLikeTheSite() {
        val lexicon = load("symbols", SymbolsResponse.serializer()).symbols
        val spotter = SymbolSpotter(lexicon)
        // every pattern from the PHP lexicon has to compile as a Java regex
        assertEquals(lexicon.size, spotter.size)
        val found = spotter.detect("I was swimming in the ocean and my teeth fell out, then I was flying")
        assertTrue("water" in found)
        assertTrue("teeth" in found)
        assertTrue("flying" in found)
    }
}
