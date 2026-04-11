package org.quintilis.factions.listeners

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.events.CoreDamageEvent

class CoreDamageListener: Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCoreDamage(event: CoreDamageEvent) {
        val core = event.targetCore
        val damage = event.damage

        core.takeDamage(damage)

        val location = core.getLocation() !!
        core.getWorld().getNearbyPlayers(location, 20.0).forEach { player ->
            player.sendActionBar(Component.text("<yellow>NEXUS: §f${core.health} HP §7(-$damage)"))
        }

        if (core.health <= 0) {
            core.active = false
            core.save<ClanCoreEntity>()
            handleCoreDestruction(core, event.antiCore)
        } else {
            // Apenas salva a vida atual
            core.save<ClanCoreEntity>()
        }

        location.world.spawnParticle(Particle.ELECTRIC_SPARK, location.add(0.5, 1.0, 0.5), 10)
    }
    private fun handleCoreDestruction(core: ClanCoreEntity, attacker: AntiCoreEntity) {
        // Lógica de explosão, remoção do bloco e anúncio global de vitória
        core.getStructure().destroyStructure()
        Bukkit.broadcast(Component.text("§4§l[!] §fUm Core de §6${core.clanId} §ffoi destruído!"))
    }
}