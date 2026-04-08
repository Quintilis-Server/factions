package org.quintilis.factions.structure

import fr.skytasul.guardianbeam.Laser
import org.bukkit.ChatColor
import org.bukkit.Location
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
                antiCore = entity,
            )
        }
    }
    fun castRay(plugin: JavaPlugin){
        val core = antiCore.getCore() ?: return
        val coreLocation = core.getLocation(core.getWorld())

        val laser = Laser.CrystalLaser(location, coreLocation, 10, -1)
        laser.start(plugin)
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

    fun destroy(){

    }
}