/** Decodes HTML entities such as `&amp;`, `&rsquo;`, `&#8217;` and `&#x2019;`. */
package com.radmon.radmonradio.util.html

object HtmlEntities {
    private val named: Map<String, String> = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“",
        "ndash" to "–", "mdash" to "—", "hellip" to "…"
    )

    fun decode(input: String): String {
        if (!input.contains("&")) return input
        val result = StringBuilder()
        var index = 0
        while (index < input.length) {
            if (input[index] == '&') {
                // a ";" within 10 characters, and a known entity in between
                val windowEnd = minOf(index + 10, input.length)
                val semicolon = input.indexOf(';', index + 1).takeIf { it in index until windowEnd }
                val decoded = semicolon?.let { decodeEntity(input.substring(index + 1, it)) }
                if (semicolon != null && decoded != null) {
                    result.append(decoded)
                    index = semicolon + 1
                    continue
                }
            }
            result.append(input[index])
            index++
        }
        return result.toString()
    }

    // the text between "&" and ";", e.g. "amp", "#8217" or "#x2019"
    private fun decodeEntity(entity: String): String? {
        if (entity.startsWith("#x") || entity.startsWith("#X")) {
            val codePoint = entity.drop(2).toIntOrNull(16) ?: return null
            return runCatching { String(Character.toChars(codePoint)) }.getOrNull()
        }
        if (entity.startsWith("#")) {
            val codePoint = entity.drop(1).toIntOrNull() ?: return null
            return runCatching { String(Character.toChars(codePoint)) }.getOrNull()
        }
        return named[entity]
    }
}
