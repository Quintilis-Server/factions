package org.quintilis.factions.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.services.FactionsServices
import java.lang.IllegalArgumentException
import java.sql.SQLException
import java.util.logging.Logger

@AutoRegister
class PlayerJoinListener(private val plugin: Factions): Listener {
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
}