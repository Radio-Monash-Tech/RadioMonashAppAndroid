/** The journalism filters, one per tag on the Squarespace site. Mirrors the iOS app's ArticleTopic. */
package com.radmon.radmonradio.data.model

enum class ArticleTopic(val label: String, val siteTag: String) {
    ANALYSIS("Analysis", "Analysis"),
    POP_CULTURE("Pop Culture", "Pop Culutre"), // gotta ask the journo to fix this
    GIG_REVIEWS("Gig Reviews", "Gig review"),
    MOVIES_AND_TV("Movies & TV Shows", "Movies"),
    NEW_MUSIC("New Music", "New Music"),
    RADIO_MONASH("What's Happening at Radio Monash", "Radio Monash");

    companion object {
        /** The first filter whose tag the article has, null if none. */
        fun fromTags(tags: List<String>): ArticleTopic? {
            val lowered = tags.map { it.lowercase() }.toSet()
            return entries.firstOrNull { it.siteTag.lowercase() in lowered }
        }
    }
}
