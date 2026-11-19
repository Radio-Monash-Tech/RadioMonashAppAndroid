/** The text between a tag's `<` and `>`, e.g. `a href="/events"` or `/p`. */
package com.radmon.radmonradio.util.html

class HtmlTag private constructor(
    // lowercased, e.g. "a" or "p"
    val name: String,
    // true for `/p`
    val isClosing: Boolean,
    // written as `<br/>`, so there's no closing tag to wait for
    val isSelfClosing: Boolean,
    // the whole tag text, kept for reading attributes
    private val source: String
) {
    // the value of `name="…"` or `name='…'`, still entity-encoded
    fun attribute(name: String): String? {
        for (quote in listOf('"', '\'')) {
            val marker = " $name=$quote"
            val start = source.indexOf(marker)
            if (start == -1) continue
            val restStart = start + marker.length
            val end = source.indexOf(quote, restStart)
            if (end != -1) return source.substring(restStart, end)
        }
        return null
    }

    companion object {
        // null when there's no tag name, e.g. `<!DOCTYPE>` or `< >`
        fun parse(source: String): HtmlTag? {
            val isClosing = source.startsWith("/")
            val afterSlash = if (isClosing) source.substring(1) else source
            val rawName = afterSlash.takeWhile { it.isLetterOrDigit() }
            if (rawName.isEmpty()) return null
            val isSelfClosing = source.endsWith("/")
            return HtmlTag(rawName.lowercase(), isClosing, isSelfClosing, source)
        }
    }
}
