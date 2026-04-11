package org.quintilis.factions.managers

import fr.skytasul.glowingentities.GlowingBlocks
import fr.skytasul.glowingentities.GlowingEntities
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.exceptions.ConfigFileNullValueException

object ConfigManager {
    private lateinit var config: FileConfiguration


    fun initialize(config: FileConfiguration, plugin: JavaPlugin) {
        this.config = config
    }
    private fun getString(path: String): String{
        val value = this.config.getString(path)
        if(value.isNullOrBlank()){
            throw ConfigFileNullValueException(path)
        }
        return value
    }
    private fun getInt(path: String): Int{
        val value = this.config.getInt(path)
        return value
    }

    private fun getPercentage(path: String): Double{
        val value = this.getInt(path).toDouble()
        return value / 100
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

    fun getMaxInvitationTime(): Int{
        return this.getInt("invite.maxInvitationTime")
    }

    fun getMaxAllyInvitationTime(): Int{
        return this.getInt("invite.maxAllyInvitationTime")
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

    fun getWarEnemyPoints(): Int{
        return this.getInt("war.enemy-points")
    }

    fun getWarNeutralPoints(): Int{
        return this.getInt("war.neutral-points")
    }

    fun getWarAllyPoints(): Int{
        return this.getInt("war.ally-points")
    }

    fun getNeutralKillPercentage(): Double{
        return this.getPercentage("kill.neutralStealPoints")
    }

    fun getEnemyKillPercentage(): Double{
        return this.getPercentage("kill.enemyStealPoints")
    }

    fun getKillPoints(): Int{
        return this.getInt("kill.points")
    }

    fun getClanCorePrice(): Int{
        return this.getInt("clan.core-price")
    }

    fun getClanAntiCorePrice(): Int{
        return this.getInt("clan.anti-core-price")
    }

    fun getGlowstonePrice(): Int{
        return this.getInt("clan.glowstone-price")
    }

    fun getAnticoreDamage(): Int{
        return this.getInt("anticore.damage")
    }
    fun getAnticoreTime(): Int{
        return this.getInt("anticore.time")
    }
    fun getAnticoreShots(): Int{
        return this.getInt("anticore.shots")
    }
}