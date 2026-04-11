package org.quintilis.factions.util.json

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant

class JavaTimeTypeAdapterFactory : TypeAdapterFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        // Inspeciona a classe exata que o Gson está tentando ler agora
        if (type.rawType == Instant::class.java) {

            // Retorna o nosso adaptador forçadamente, impedindo o uso de Reflection
            return object : TypeAdapter<Instant>() {
                override fun write(out: JsonWriter, value: Instant?) {
                    if (value == null) {
                        out.nullValue()
                    } else {
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
            }.nullSafe() as TypeAdapter<T>
        }

        // Se não for Instant, devolve null pro Gson seguir a vida dele normalmente
        return null
    }
}