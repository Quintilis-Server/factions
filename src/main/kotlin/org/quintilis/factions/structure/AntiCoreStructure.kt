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
import org.quintilis.factions.managers.QuintilisScheduler
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

    fun castRay(){
        val start = location?.clone()?.add(0.5, 0.5, 0.5) ?: return
        val core = antiCore.getCore() ?: return
        val end = core.getLocation()?.clone()?.add(0.0, 0.5, 0.5) ?: return


        val world = start.world

        val distance = start.distance(end)

        if(distance < 1.0) return

        val direction = end.toVector().subtract(start.toVector()).normalize()

        val spacing = 0.5

        var currentDistance = 0.0
        while(currentDistance < distance){
            val currentLoc = start.clone().add(direction.clone().multiply(currentDistance))

            world.spawnParticle(
                Particle.WITCH,
                currentLoc,
                1,
                0.0, 0.0, 0.0,
                0.0
            )

            currentDistance += spacing
        }

        world.spawnParticle(Particle.EXPLOSION_EMITTER, end, 1)
    }

//    fun castRay(plugin: JavaPlugin){
//        val core = antiCore.getCore() ?: return
//        val coreLocation = core.getLocation()
//        val startLoc = location ?: return
//
//        val laser = Laser.CrystalLaser(location, coreLocation, 10, -1)
//        try {
//            laser.start(plugin)
//            QuintilisScheduler.runAtLocation(plugin, startLoc, {
//                laser.start(plugin)
//
//                // Agenda o stop usando o wrapper
//                QuintilisScheduler.runDelayedAtLocation(plugin, startLoc, {
//                    laser.stop()
//                }, 20L)
//            })
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

//    fun glow(glow: Boolean = true){
//        if(location == null) return
//        val clan = antiCore.getCore()?.getClan() ?: return
//        val members = clan.getMembers()
//        for(m in members){
//            val player = m.getPlayer() ?: continue
//            if(glow){
//                glowingBlocks.setGlowing(location, player, ChatColor.RED)
//            }else{
//                glowingBlocks.unsetGlowing(location, player)
//            }
//        }
//    }

    fun destroy() {
        val loc = location!!
        loc.block.type// Remove o bloco fisicamente
        loc.world.spawnParticle(Particle.EXPLOSION, loc.add(0.5, 0.5, 0.5), 1)
        loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
//        glow(false) // Remove o contorno brilhante
    }
}