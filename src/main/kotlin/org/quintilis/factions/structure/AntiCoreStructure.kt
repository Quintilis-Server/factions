package org.quintilis.factions.structure

import fr.skytasul.guardianbeam.Laser
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.services.FactionsServices.glowingBlocks

class AntiCoreStructure(
    val location: Location? = null,
    val antiCore: AntiCoreEntity,
) {
    companion object{
        fun fromEntity(entity: AntiCoreEntity): AntiCoreStructure {
            return AntiCoreStructure(
                location = entity.getLocation(),
                antiCore = entity,
            )
        }
    }
    fun castRay(plugin: JavaPlugin){
        val core = antiCore.getCore() ?: return
        val coreLocation = core.getLocation()

        val laser = Laser.CrystalLaser(location, coreLocation, 10, -1)
        try {
            laser.start(plugin)
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                laser.stop()
            }, 20L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun glow(glow: Boolean = true){
        if(location == null) return
        val clan = antiCore.getCore()?.getClan() ?: return
        val members = clan.getMembers()
        for(m in members){
            val player = m.getPlayer() ?: continue
            if(glow){
                glowingBlocks.setGlowing(location, player, ChatColor.RED)
            }else{
                glowingBlocks.unsetGlowing(location, player)
            }
        }
    }

    fun destroy() {
        val loc = location!!
        loc.block.type// Remove o bloco fisicamente
        loc.world.spawnParticle(Particle.EXPLOSION, loc.add(0.5, 0.5, 0.5), 1)
        loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
        glow(false) // Remove o contorno brilhante
    }
}