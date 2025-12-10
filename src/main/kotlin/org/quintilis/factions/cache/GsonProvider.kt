package org.quintilis.factions.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object GsonProvider {
    val gson: Gson = GsonBuilder()
        .disableHtmlEscaping() // Recomendado para JSONs limpos
        .registerTypeAdapter(OffsetDateTime::class.java, object : JsonSerializer<OffsetDateTime>,
            JsonDeserializer<OffsetDateTime> {
            override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            }
            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): OffsetDateTime {
                return OffsetDateTime.parse(json!!.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        })
        .registerTypeAdapter(UUID::class.java, object : JsonSerializer<UUID>, JsonDeserializer<UUID> {
            override fun serialize(src: UUID?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src.toString())
            }
            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UUID {
                return UUID.fromString(json!!.asString)
            }
        })
        .create()
}