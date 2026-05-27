package org.quintilis.factions.managers

import fr.skytasul.glowingentities.GlowingBlocks
import fr.skytasul.glowingentities.GlowingEntities
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.exceptions.ConfigFileNullValueException

object ConfigManager: BaseConfigManager() {

    fun getMaxInvitationTime(): Int{
        return this.getInt("invite.maxInvitationTime")
    }

    fun getMaxAllyInvitationTime(): Int{
        return this.getInt("invite.maxAllyInvitationTime")
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
//    fun getAnticoreTime(): Int{
//        return this.getInt("anticore.time")
//    }
    fun getAnticoreShots(): Int{
        return this.getInt("anticore.shots")
    }

    fun getAnticoreActivationRadius(): Double {
        return this.getDouble("anticore.activation-radius", 15.0)
    }

    fun getAnticoreCompassExpiry(): Int{
        return this.getInt("anticore.compass-expiry")
    }
    fun getAnticoreCompassPrice(): Int{
        return this.getInt("anticore.compass-price")
    }
    fun getWarTimeout(): Int{
        return this.getInt("war.timeout")
    }
    fun getNexusTeleportCost(): Int{
        return this.getInt("clan.teleportCost")
    }
}