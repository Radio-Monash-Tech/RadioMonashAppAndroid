package com.radmon.radmonradio.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central API client providing access to all remote services.
 * Uses Retrofit for HTTP networking with shared OkHttp client configuration.
 */
object ApiClient {
    private const val BASE_URL = "https://api.radiocult.fm/"
    private const val ITUNES_BASE_URL = "https://itunes.apple.com/"
    private const val SONGLINK_BASE_URL = "https://api.song.link/"

    /** Shared HTTP client with configured timeouts */
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Service for fetching radio station metadata */
    val apiService: RadioApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioApiService::class.java)
    }

    /** Service for searching iTunes music catalog */
    val iTunesApiService: ITunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ITUNES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesApiService::class.java)
    }

    /** Service for fetching multi-platform music links via Song.link */
    val songLinkApiService: SongLinkApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SONGLINK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SongLinkApiService::class.java)
    }
}