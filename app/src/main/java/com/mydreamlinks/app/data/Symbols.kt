package com.mydreamlinks.app.data

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Spots symbols while the user types, with the same word list and the same \b(?:...) matching
 * the site uses (DreamSymbols.php), so what the app suggests matches what the server finds.
 */
class SymbolSpotter(lexicon: Map<String, String>) {
    private val patterns: List<Pair<String, Pattern>> = lexicon.mapNotNull { (symbol, fragment) ->
        try {
            symbol to Pattern.compile("\\b(?:$fragment)", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
        } catch (_: PatternSyntaxException) {
            null
        }
    }

    /** How many patterns compiled; one the phone's regex engine rejects is skipped. */
    val size: Int get() = patterns.size

    fun detect(text: String): List<String> = patterns.filter { it.second.matcher(text).find() }.map { it.first }
}
