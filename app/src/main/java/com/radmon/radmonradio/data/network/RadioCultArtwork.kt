package com.radmon.radmonradio.data.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

data class ArtworkVariants(val urls: Map<String, String>) {
    /** The first size on the list that exists, otherwise any. */
    fun best(preferring: List<String> = listOf("512x512", "default", "original")): String? {
        for (key in preferring) {
            urls[key]?.let { return it }
        }
        return urls.values.firstOrNull()
    }
}

/** RadioCult sends the artwork field as a raw `{size: url}` map, with no wrapper key. */
class ArtworkVariantsDeserializer : JsonDeserializer<ArtworkVariants> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ArtworkVariants {
        val urls = mutableMapOf<String, String>()
        if (json.isJsonObject) {
            json.asJsonObject.entrySet().forEach { entry ->
                if (entry.value.isJsonPrimitive) urls[entry.key] = entry.value.asString
            }
        }
        return ArtworkVariants(urls)
    }
}
