package org.quintilis.factions.util.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter: JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        if(src == null) return JsonNull.INSTANCE
        return JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? {
        if (json == null || json.isJsonNull) return null
        return try {
            Instant.parse(json.asString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}