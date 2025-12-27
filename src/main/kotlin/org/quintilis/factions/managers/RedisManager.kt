package org.quintilis.factions.managers

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisManager {
    private lateinit var pool: JedisPool

    fun connect(){
        val config = JedisPoolConfig()
        config.maxTotal = 16

        pool = JedisPool(
            config,
            ConfigManager.getRedisHost(),
            ConfigManager.getRedisPort(),
            2000,  // timeout em ms
            null,  // password (null se não tiver)
            ConfigManager.getRedisDatabase()  // database index (0-15)
        )
    }
    fun close(){
        if(::pool.isInitialized) pool.close()
    }

    fun <T> run(action: (Jedis) -> T): T {
        return pool.resource.use{jedis ->
            action(jedis)
        }
    }
}