package org.quintilis.factions.managers

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.exceptions.ConfigFileNullValueException

abstract class BaseConfigManager {
    private lateinit var config: FileConfiguration


    fun initialize(config: FileConfiguration, plugin: JavaPlugin) {
        this.config = config
    }

    protected fun getString(path: String): String{
        val value = this.config.getString(path)
        if(value.isNullOrBlank()){
            throw ConfigFileNullValueException(path)
        }
        return value
    }
    protected fun getInt(path: String): Int{
        val value = this.config.getInt(path)
        return value
    }

    protected fun getPercentage(path: String): Double{
        val value = this.getInt(path).toDouble()
        return value / 100
    }

    protected fun getDouble(path: String, default: Double?): Double{
        return if(default != null) this.config.getDouble(path, default) else this.config.getDouble(path)
    }


    fun getHost(): String{
        return this.getString("database.host")
    }

    fun getPort(): Int{
        return this.config.getInt("database.port")
    }

    fun getUsername(): String{
        return this.getString("database.username")
    }

    fun getPassword(): String{
        return this.getString("database.password")
    }

    fun getDatabaseName(): String{
        return this.getString("database.dbName")
    }

    fun getRedisPort(): Int{
        return this.getInt("redis.port")
    }

    fun getRedisHost(): String{
        return this.getString("redis.host")
    }

    fun getRedisDatabase(): Int{
        return this.getInt("redis.database")
    }
}