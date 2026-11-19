/** Asks Squarespace for an image at a given width. Mirrors the iOS app's URL.squarespaceSized. */
package com.radmon.radmonradio.util

import java.net.URI

fun String.squarespaceSized(width: Int): String {
    // only change squarespace image links
    val uri = runCatching { URI(this) }.getOrNull() ?: return this
    if (uri.host?.contains("squarespace-cdn.com") != true) return this

    // keep the other query items but drop any old format
    val otherItems = (uri.query ?: "").split("&").filter { it.isNotEmpty() && !it.startsWith("format=") }
    val newQuery = (otherItems + "format=${width}w").joinToString("&")
    return "${this.substringBefore("?")}?$newQuery"
}
