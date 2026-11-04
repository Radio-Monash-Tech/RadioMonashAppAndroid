/** Repository for finding multi-platform links for songs. */
package com.radmon.radmonradio.data.repository
import android.util.Log
import com.radmon.radmonradio.data.network.ApiClient
import com.radmon.radmonradio.data.network.PlatformData
import com.radmon.radmonradio.data.network.SongData

class SongFinderRepository {
    private val iTunesApi = ApiClient.iTunesApiService
    private val songLinkApi = ApiClient.songLinkApiService
    
    suspend fun findSongLinks(songName: String, artistName: String): Result<SongData> {
        return try {
            val searchTerm = "$songName $artistName"
            Log.d(TAG, "Searching iTunes for: $searchTerm")
            val iTunesResponse = iTunesApi.searchTrack(searchTerm)
            if (iTunesResponse.resultCount == 0 || iTunesResponse.results.isEmpty()) {
                Log.w(TAG, "No results found on iTunes")
                return Result.failure(Exception("Song not found."))
            }
            val firstResult = iTunesResponse.results[0]
            val trackUrl = firstResult.trackViewUrl
                ?: return Result.failure(Exception("Song not found."))
            Log.d(TAG, "Found iTunes track: $trackUrl")
            Log.d(TAG, "Fetching platform links...")
            val songLinkResponse = songLinkApi.getPlatformLinks(trackUrl)
            val entities = songLinkResponse.entitiesByUniqueId
            var title = songName
            var artist = artistName
            entities?.values?.firstOrNull { entity ->
                entity.title?.contains("youtube", ignoreCase = true) == false
            }?.let { entity ->
                title = entity.title ?: songName
                artist = entity.artistName ?: artistName
            }
            val platforms = mutableListOf<PlatformData>()
            songLinkResponse.linksByPlatform?.forEach { (platformKey, platformLink) ->
                platformLink.url?.let { url ->
                    val displayName = mapPlatformName(platformKey)
                    platforms.add(
                        PlatformData(
                            platformName = platformKey,
                            displayName = displayName,
                            url = url,
                            nativeUri = platformLink.nativeAppUriMobile
                                ?: platformLink.nativeAppUriDesktop
                        )
                    )
                }
            }
            val allowedPlatforms = listOf("spotify", "tidal", "youtube", "youtubemusic", "applemusic", "soundcloud", "bandcamp", "deezer")
            val filteredPlatforms = platforms.filter {
                it.platformName.lowercase() in allowedPlatforms
            }
            if (filteredPlatforms.isEmpty()) {
                return Result.failure(Exception("Song not available on supported platforms"))
            }
            val sortedPlatforms = filteredPlatforms.sortedBy {
                PLATFORM_PRIORITY.indexOf(it.platformName.lowercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
            val songData = SongData(
                title = title,
                artist = artist,
                platforms = sortedPlatforms
            )
            Log.d(TAG, "Found ${sortedPlatforms.size} platform links")
            Result.success(songData)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding song links", e)
            Result.failure(e)
        }
    }
    
    private fun mapPlatformName(platformKey: String): String {
        return when (platformKey.lowercase()) {
            "spotify" -> "Spotify"
            "applemusic" -> "Apple Music"
            "youtube" -> "YouTube"
            "youtubemusic" -> "YouTube Music"
            "deezer" -> "Deezer"
            "tidal" -> "Tidal"
            "soundcloud" -> "SoundCloud"
            "bandcamp" -> "Bandcamp"
            else -> platformKey.replaceFirstChar { it.uppercase() }
        }
    }
    companion object {
        private const val TAG = "SongFinderRepository"
        private val PLATFORM_PRIORITY = listOf(
            "spotify",
            "applemusic",
            "youtubemusic",
            "youtube",
            "deezer",
            "tidal",
            "soundcloud",
            "bandcamp"
        )
    }
}
