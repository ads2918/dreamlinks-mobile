package com.mydreamlinks.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* Shapes returned by the site's api/v1 endpoints (see app/Controllers/Api on the server). */

@Serializable
data class Owner(val id: Int, val username: String, val avatar: String? = null)

@Serializable
data class Dream(
    val id: Int,
    val title: String,
    val text: String,
    val date: String,
    val visibility: String,
    val owner: Owner,
    val own: Boolean = false,
    val tags: List<String> = emptyList(),
    val symbols: List<String> = emptyList(),
    val moon: String = "",
    val resonance: Int = 0,
    val resonated: Boolean = false,
)

@Serializable
data class Pulse(val symbol: String, val dreamers: Int)

@Serializable
data class DreamsResponse(val dreams: List<Dream>, val pulse: List<Pulse>? = null)

@Serializable
data class Me(
    val id: Int,
    val username: String,
    val email: String = "",
    val bio: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    @SerialName("ai_analysis_enabled") val aiAnalysisEnabled: Boolean = false,
    @SerialName("default_dream_visibility") val defaultVisibility: String = "private",
    @SerialName("reminder_enabled") val reminderEnabled: Boolean = false,
    @SerialName("reminder_hour") val reminderHour: Int = 8,
    val timezone: String = "America/Indianapolis",
)

@Serializable
data class LoginResponse(val token: String, val user: Me)

@Serializable
data class Resonance(val on: Boolean, val count: Int)

@Serializable
data class ReadNow(val status: String)

@Serializable
data class LinkState(val state: String, @SerialName("link_id") val linkId: Int? = null)

@Serializable
data class Person(
    val id: Int,
    val username: String,
    val avatar: String? = null,
    val bio: String? = null,
    @SerialName("self") val isSelf: Boolean = false,
    val link: LinkState? = null,
)

@Serializable
data class ProfileResponse(val user: Person, val dreams: List<Dream>)

@Serializable
data class SearchResponse(val users: List<Person>)

@Serializable
data class LinkedPerson(@SerialName("link_id") val linkId: Int, val id: Int, val username: String, val avatar: String? = null)

@Serializable
data class LinksResponse(val linked: List<LinkedPerson>, val pending: List<LinkedPerson>)

@Serializable
data class LinkStateResponse(val link: LinkState)

@Serializable
data class BlockedPerson(@SerialName("block_id") val blockId: Int, val id: Int, val username: String, val avatar: String? = null)

@Serializable
data class BlocksResponse(val blocked: List<BlockedPerson>)

@Serializable
data class SymbolCount(val symbol: String, val n: Int)

@Serializable
data class MoonSymbol(val symbol: String, val n: Int, @SerialName("stands_out") val standsOut: Boolean = false)

@Serializable
data class MiniDream(val id: Int, val title: String, val date: String)

@Serializable
data class MoonPhaseDetail(
    val phase: Int,
    val name: String,
    val count: Int,
    val share: Int,
    val symbols: List<MoonSymbol> = emptyList(),
    val dreams: List<MiniDream> = emptyList(),
)

@Serializable
data class GraphNode(val id: String, val mine: Int, val theirs: Int, val spotted: Boolean = false)

@Serializable
data class GraphLink(val source: String, val target: String, val weight: Int)

@Serializable
data class GraphDream(val id: Int, val side: String, val title: String, val date: String, val owner: Int, val symbols: List<String>)

@Serializable
data class Graph(val nodes: List<GraphNode>, val links: List<GraphLink>, val dreams: List<GraphDream>)

@Serializable
data class Kin(
    val id: Int,
    val username: String,
    val avatar: String? = null,
    val dreams: Int,
    val similarity: Double,
    val shared: List<String> = emptyList(),
    val linked: Boolean = false,
    @SerialName("only_mine") val onlyMine: List<String> = emptyList(),
    @SerialName("only_theirs") val onlyTheirs: List<String> = emptyList(),
)

@Serializable
data class Dreamscape(
    @SerialName("dream_count") val dreamCount: Int,
    @SerialName("top_symbols") val topSymbols: List<SymbolCount>,
    val moon: List<MoonPhaseDetail>,
    val graph: Graph,
    val kin: List<Kin>,
    val dreamers: List<Kin>,
    val compare: Kin? = null,
    @SerialName("compare_note") val compareNote: String? = null,
)

@Serializable
data class Tonight(val name: String, val phase: Double)

@Serializable
data class Reading(
    val date: String,
    val overview: String,
    val patterns: List<String> = emptyList(),
    val symbols: List<String> = emptyList(),
    val celestial: List<String> = emptyList(),
    val moon: String = "",
    @SerialName("moon_phase") val moonPhase: Double = 0.0,
)

@Serializable
data class Readings(
    val enabled: Boolean,
    val waiting: Int,
    /** Hit today's reading cap, so waiting dreams get read tomorrow. Older servers don't send it. */
    val capped: Boolean = false,
    @SerialName("dream_count") val dreamCount: Int,
    val tonight: Tonight,
    @SerialName("top_symbols") val topSymbols: List<SymbolCount>,
    val readings: List<Reading>,
)

@Serializable
data class SymbolsResponse(val symbols: Map<String, String>)

@Serializable
data class ApiError(val error: String = "Something went wrong.", val fields: Map<String, String>? = null)

/** What the dream editor sends. */
@Serializable
data class DreamInput(
    val title: String,
    val text: String,
    val date: String,
    val visibility: String,
    val tags: List<String>,
)
