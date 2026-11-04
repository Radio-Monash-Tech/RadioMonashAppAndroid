package com.radmon.radmonradio.data.network

import com.google.gson.annotations.SerializedName

/** Response from Song.link API containing multi-platform links. */
data class SongLinkResponse(
    @SerializedName("linksByPlatform")
    val linksByPlatform: Map<String, PlatformLink>?,
    @SerializedName("entitiesByUniqueId")
    val entitiesByUniqueId: Map<String, EntityInfo>?
)

/**
 * Platform-specific link information.
 */
data class PlatformLink(
    @SerializedName("url")
    val url: String?,
    @SerializedName("nativeAppUriDesktop")
    val nativeAppUriDesktop: String?,
    @SerializedName("nativeAppUriMobile")
    val nativeAppUriMobile: String?
)

/**
 * Track entity information.
 */
data class EntityInfo(
    @SerializedName("title")
    val title: String?,
    @SerializedName("artistName")
    val artistName: String?
)

/**
 * Processed song data with platform links.
 *
 * @property title Song title
 * @property artist Artist name
 * @property platforms List of available platform links
 */
data class SongData(
    val title: String,
    val artist: String,
    val platforms: List<PlatformData>
)

/**
 * Individual platform link data.
 *
 * @property platformName Platform identifier (e.g., "spotify")
 * @property displayName User-friendly platform name
 * @property url Web URL to the track
 * @property nativeUri Native app URI if available
 */
data class PlatformData(
    val platformName: String,
    val displayName: String,
    val url: String,
    val nativeUri: String?
)
