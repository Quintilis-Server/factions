package org.quintilis.factions.util.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter: TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            // O toString() do Instant já gera o formato ISO-8601 nativamente
            out.value(value.toString())
        }
    }

    override fun read(input: JsonReader): Instant? {
        if (input.peek() == JsonToken.NULL) {
            input.nextNull()
            return null
        }
        return Instant.parse(input.nextString())
    }
}