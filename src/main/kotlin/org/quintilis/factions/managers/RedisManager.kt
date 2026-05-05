package org.quintilis.factions.managers

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.ScanParams

object RedisManager {
    private lateinit var pool: JedisPool

    fun connect(configManager: BaseConfigManager){
        val config = JedisPoolConfig()
        config.maxTotal = 16

        val host = configManager.getRedisHost()
        val port = configManager.getRedisPort()
        val database = configManager.getRedisDatabase()

        println("[RedisManager] Database host: $host, port: $port")
        println("[RedisManager] Database number: $database")
        this.pool = JedisPool(
            config,
            host,
            port,
            2000,  // timeout em ms
            null,  // password (null se não tiver)
            database  // database index (0-15)
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

    fun flushDatabase() {
        run { jedis ->
            jedis.flushDB()
            println("[RedisManager] Database ${ConfigManager.getRedisDatabase()} resetada.")
        }
    }

    fun deleteByPattern(pattern: String) {
        run { jedis ->
            var cursor = ScanParams.SCAN_POINTER_START
            val params = ScanParams().match(pattern).count(100)

            do {
                val scanResult = jedis.scan(cursor, params)
                val keys = scanResult.result

                if (keys.isNotEmpty()) {
                    jedis.del(*keys.toTypedArray())
                }

                cursor = scanResult.cursor
            } while (cursor != ScanParams.SCAN_POINTER_START)
        }
    }
}