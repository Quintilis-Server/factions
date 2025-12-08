package org.quintilis.factions.cache

import redis.clients.jedis.Jedis

/**
 * Cache especializado em guardar Listas de Strings usando Redis Sets (SADD/SMEMBERS).
 */
abstract class StringSetCache<K>(
    prefix: String,
    ttl: Long,
): BaseRedisCache<K, List<String>>(prefix, ttl) {
    override fun readFromRedis(jedis: Jedis, key: String): List<String>? {
        if (!jedis.exists(key)) return null
        return jedis.smembers(key).toList()
    }

    override fun writeToRedis(jedis: Jedis, key: String, value: List<String>) {
        if (value.isNotEmpty()) {
            jedis.sadd(key, *value.toTypedArray())
        }
    }

    // Não cachear listas vazias para economizar RAM do Redis
    override fun shouldCache(value: List<String>): Boolean {
        return value.isNotEmpty()
    }
}