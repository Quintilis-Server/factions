package org.quintilis.factions.managers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.quintilis.factions.util.json.JavaTimeTypeAdapterFactory
import java.lang.reflect.Modifier

object GsonProvider {
    val gson: Gson = GsonBuilder()
        // AQUI ESTÁ A MÁGICA! A fábrica tem prioridade absoluta sobre a Reflection:
        .registerTypeAdapterFactory(JavaTimeTypeAdapterFactory())
        .excludeFieldsWithModifiers(
            Modifier.TRANSIENT,
            Modifier.STATIC
        )
        .create()
}