package org.quintilis.factions.listeners

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.Death
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.getPlayerEntity
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.TagManager
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.services.FactionsServices.clanRelationCache
import org.quintilis.factions.services.FactionsServices.playerCache
import java.lang.IllegalArgumentException
import java.sql.SQLException

@AutoRegister
class PlayerListener(private val plugin: Factions): Listener {
    private val playerDao: PlayerDao = FactionsServices.playerDao
    @EventHandler
    @Throws(IllegalArgumentException::class, IllegalStateException::class, SQLException::class)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId


        if(!this.playerDao.isInDatabase(uuid)){
            plugin.logger.info("Player ${player.name} is not in the database")
            val playerEntity = PlayerEntity(uuid, player.name, 0);
            playerEntity.save<PlayerEntity>();
            plugin.logger.info("Player ${player.name} joined successfully")
            return
        }

        plugin.logger.info("Player ${player.name} joined successfully")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        TagManager.remove(event.player)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val killerEntity = killer.getPlayerEntity() ?: return

        val victimEntity = event.player.getPlayerEntity() ?: return

        Death.fromEvent(event, killerEntity)

        val killerClan = killerEntity.getClan()
        val victimClan = victimEntity.getClan()

        if(killerClan != null){
            if(victimClan == null){
                killerClan.addPoints(ConfigManager.getWarNeutralPoints())
            } else {
                val isAlly = clanRelationCache.isRelation(killerClan, victimClan, Relation.ALLY)
                val isEnemy = clanRelationCache.isRelation(killerClan, victimClan, Relation.ENEMY)
                when {
                    isAlly -> {
                        return
                    }
                    isEnemy -> {
                        killerClan.addPoints(ConfigManager.getWarEnemyPoints())
                    }
                    else ->{
                        killerClan.addPoints(ConfigManager.getWarNeutralPoints())
                    }
                }
            }
        }
        val relation = if (killerClan != null && victimClan != null &&
            clanRelationCache.isRelation(killerClan.id!!, victimClan.id!!, Relation.ENEMY))
            Relation.ENEMY else Relation.ALLY

        stealPercentagePoints(killer, killerEntity, event.player, victimEntity, relation)

    }

    private fun stealPercentagePoints(
        killer: Player,
        killerEntity: PlayerEntity,
        victim: Player,
        victimEntity: PlayerEntity,
        relation: Relation = Relation.ALLY
    ) {
        val percentage = when(relation) {
            Relation.ENEMY ->  ConfigManager.getEnemyKillPercentage()
            else ->  ConfigManager.getNeutralKillPercentage()
        }
        val stolenPoints = (victimEntity.points * percentage).toInt()
        val finalStolenPoints = if (stolenPoints < 1) ConfigManager.getKillPoints() else stolenPoints
        if(stolenPoints > 1){
            victimEntity.points -= finalStolenPoints
        }

        killerEntity.points += finalStolenPoints

        killerEntity.save<BaseEntity>()
        victimEntity.save<PlayerEntity>()

        killer.sendTranslatable(
            "kill.stolen.killer",
            Argument.numeric("stolen_points", finalStolenPoints),
            Argument.string("victim", victim.name)
        )
        if(stolenPoints > 0) {
            victim.sendTranslatable(
                "kill.stolen.victim",
                Argument.numeric("stolen_points", finalStolenPoints),
                Argument.string("player", killer.name)
            )
        }
        return
    }
}