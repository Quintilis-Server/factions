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
//        println("[BaseRedisCache] Tentando buscar: $redisKey")

        // 1. Tenta ler do Redis
        try {
            val cachedValue = RedisManager.run { jedis ->
                val exists = jedis.exists(redisKey)
//                println("[BaseRedisCache] Chave existe no Redis? $exists")
                if (exists) {
                    val rawValue = jedis.get(redisKey)
//                    println("[BaseRedisCache] Valor bruto do Redis: ${rawValue}...")
                }
                readFromRedis(jedis, redisKey)
            }
//            println("[BaseRedisCache] Valor após readFromRedis: ${if (cachedValue != null) "NÃO-NULO" else "NULO"}")
            if (cachedValue != null) {
//                println("[BaseRedisCache] ✅ CACHE HIT - Retornando do cache")
                return cachedValue
            }
        } catch (e: Exception) {
//            println("[BaseRedisCache] ❌ Erro ao ler do Redis: ${e.message}")
            e.printStackTrace() // Loga erro do Redis, mas não para o fluxo
        }

        // 2. Cache Miss (ou Redis offline): Busca do Banco de Dados
//        println("[BaseRedisCache] ⚠️ CACHE MISS - Executando dbFetcher para key: $key")
        val dbValue = dbFetcher(key)

        // 3. Salva no Redis (apenas se o valor for válido)
        if (shouldCache(dbValue)) {
//            println("[BaseRedisCache] Salvando no Redis (shouldCache=true)")
            try {
                RedisManager.run { jedis ->
                    writeToRedis(jedis, redisKey, dbValue)
                    if (ttlSeconds > 0) {
                        jedis.expire(redisKey, ttlSeconds)
                    }
                }
            } catch (e: Exception) {
//                println("[BaseRedisCache] ❌ Erro ao escrever no Redis: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("[BaseRedisCache] ❌ NÃO salvando no Redis (shouldCache=false)")
        }

        return dbValue
    }

    fun invalidate(key: K) {
        try {
            RedisManager.run { jedis ->
                jedis.del("$keyPrefix$key")
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