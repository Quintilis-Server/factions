package org.quintilis.factions.cache

import org.quintilis.factions.managers.RedisManager
import redis.clients.jedis.Jedis

/**
 * Cache Base Genérico.
 * @param K O tipo da chave (ex: UUID, Int, String)
 * @param V O tipo do valor retornado (ex: List<String>, ClanEntity)
 */
abstract class BaseRedisCache<K, V>(
    private val keyPrefix: String,
    private val ttlSeconds: Long
) {
    /**
     * Método principal. Tenta pegar do cache, se falhar, usa o 'dbFetcher'.
     */
    fun getOrFetch(key: K, dbFetcher: (K) -> V): V {
        val redisKey = "$keyPrefix$key"

        // 1. Tenta ler do Redis
        try {
            val cachedValue = RedisManager.run { jedis ->
                readFromRedis(jedis, redisKey)
            }
            if (cachedValue != null) {
                return cachedValue
            }
        } catch (e: Exception) {
            e.printStackTrace() // Loga erro do Redis, mas não para o fluxo
        }

        // 2. Cache Miss (ou Redis offline): Busca do Banco de Dados
        val dbValue = dbFetcher(key)

        // 3. Salva no Redis (apenas se o valor for válido)
        put(key, dbValue)

        return dbValue
    }

    open fun invalidate(key: K) {
        try {
            RedisManager.run { jedis ->
                jedis.del("$keyPrefix$key")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Salva ou Atualiza um valor manualmente no Cache.
     * Útil quando você já tem o objeto atualizado e quer evitar um round-trip no Banco.
     */
    fun put(key: K, value: V) {
        // Respeita a regra de negócio (ex: não salvar nulos)
        if (!shouldCache(value)) return

        val redisKey = "$keyPrefix$key"

        try {
            RedisManager.run { jedis ->
                writeToRedis(jedis, redisKey, value)
                if (ttlSeconds > 0) {
                    jedis.expire(redisKey, ttlSeconds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Métodos que as classes filhas devem implementar ---

    // Como ler esse tipo de dado do Redis? (String, List, Map, JSON?)
    protected abstract fun readFromRedis(jedis: Jedis, key: String): V?

    // Como escrever esse tipo de dado no Redis?
    protected abstract fun writeToRedis(jedis: Jedis, key: String, value: V)

    // Opcional: Critério para salvar (ex: não salvar listas vazias)
    protected open fun shouldCache(value: V): Boolean = true
}