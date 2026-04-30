package org.quintilis.factions.commands.home

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.Factions
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.commands.Commands
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.getPlayerEntity
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.CombatManager
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.FactionsServices.coreCache
import org.quintilis.factions.task.NexusTeleportTask

class HomeCommand(private val plugin: Factions): BaseCommand(
    name = "home",
    description = "Teleport player home",
    usage = "/home",
    aliases = listOf("home"),
    commands = emptyList()
) {
    override fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = commandSender as Player
        val playerEntity = player.getPlayerEntity() ?: return true
        val clan = player.getClan() ?: return run {
            player.sendTranslatable("clan.error.no_clan")
            true
        }

        if (CombatManager.isInCombat(player)) {
            player.sendTranslatable("teleport.error.in_combat")
            return true
        }

        val nexus = coreCache.findNexusByClanId(clan.id!!)
        if(nexus == null || !nexus.active){
            player.sendTranslatable("nexus.error.no_nexus")
            return true
        }

        val cost = ConfigManager.getNexusTeleportCost()
        if (playerEntity.points < cost) {
            player.sendTranslatable("teleport.error.insufficient_points", Argument.string("cost", cost.toString()))
            return true
        }

        val startLoc = player.location.toBlockLocation()
        val delay = 5

        player.sendTranslatable("teleport.starting", Argument.numeric("seconds", delay))

        val task = NexusTeleportTask(plugin, player, playerEntity, nexus, cost, startLoc, 5)

        player.scheduler.runAtFixedRate(
            plugin,
            task, // O nosso Consumer
            null, // Ação ao cancelar (opcional)
            1L,   // Delay inicial (ticks)
            20L   // Período (ticks)
        )
        return true
    }

    override fun onTabComplete(
        commandSender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return mutableListOf()
    }
}