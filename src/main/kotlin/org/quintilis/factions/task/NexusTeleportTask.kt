package org.quintilis.factions.task

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.quintilis.factions.Factions
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.extensions.asKyori
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.CombatManager
import java.util.function.Consumer

class NexusTeleportTask(
    private val plugin: Factions,
    private val player: Player,
    private val playerEntity: PlayerEntity,
    private val nexus: ClanCoreEntity,
    private val cost: Int,
    private val startLoc: Location,
    private var remaining: Int
): Consumer<ScheduledTask> {

    override fun accept(task: ScheduledTask) {
        if(!player.isOnline){
            task.cancel()
            return
        }

        if (player.location.blockX != startLoc.blockX ||
            player.location.blockY != startLoc.blockY ||
            player.location.blockZ != startLoc.blockZ) {

            player.sendTranslatable("teleport.error.moved")
            player.playSound(Sound.ENTITY_VILLAGER_NO.asKyori())
            task.cancel()
            return
        }

        if (CombatManager.isInCombat(player)) {
            player.sendTranslatable("teleport.error.in_combat")
            task.cancel()
            return
        }

        if(remaining <= 0){
            val target = nexus.getLocation()!!.add(0.5, 1.1, 0.5)

            player.teleport(target)
            playerEntity.points -= cost

            playerEntity.save<PlayerEntity>()

            player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT.asKyori())
            player.sendTranslatable("teleport.success")
            task.cancel()
            return
        }

        player.playSound(Sound.UI_BUTTON_CLICK.asKyori(volume = 0.5f, pitch = 1.5f))
        remaining--
    }
}