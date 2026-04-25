package org.quintilis.factions.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.Factions
import org.quintilis.factions.managers.GsonProvider
import org.quintilis.factions.util.json.InstantAdapter
import redis.clients.jedis.Jedis
import java.lang.reflect.Modifier
import java.time.Instant

abstract class JsonCache<K, V>(
    prefix: String,
    ttl: Long,
    val classType: Class<V>,
    plugin: JavaPlugin
): BaseRedisCache<K, V?>(prefix, ttl, plugin = plugin) {
    private val gson: Gson = GsonProvider.gson

    override fun readFromRedis(jedis: Jedis, key: String): V? {
        val json = jedis.get(key) ?: return null

        // Converte a String JSON de volta para o Objeto V
        return try {
            gson.fromJson(json, classType)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Se o JSON estiver corrompido, retorna null para forçar busca no DB
        }
    }

    override fun writeToRedis(jedis: Jedis, key: String, value: V?) {
        if(value != null) {
            val json = gson.toJson(value)
            jedis.set(key, json)
        }
    }

    override fun shouldCache(value: V?): Boolean {
        return value != null
    }
}