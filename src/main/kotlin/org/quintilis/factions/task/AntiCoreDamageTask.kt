package org.quintilis.factions.task

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.events.CoreDamageEvent
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.FactionsServices.antiCoreCache

@AutoTask(configPath = "anticore.damage-time", period = 20L)
class AntiCoreDamageTask(private val plugin: JavaPlugin): BukkitRunnable() {
    override fun run() {
        val anticores = antiCoreCache.findAllActive()
        for (anticore in anticores) {
            val block = anticore.getLocation().block
            val data = block.blockData
            if(data is RespawnAnchor){

                val targetCore = anticore.getCore() ?: continue

                val damage = ConfigManager.getAnticoreDamage()

                val damageEvent = CoreDamageEvent(
                    antiCore = anticore,
                    targetCore = targetCore,
                    damage = damage
                )
                Bukkit.getPluginManager().callEvent(damageEvent)

                if(!damageEvent.isCancelled){
                    processPulse(
                        block,
                        data,
                        anticore
                    )
                }
            }
        }
    }
    private fun processPulse(
        block: Block,
        data: RespawnAnchor,
        antiCore: AntiCoreEntity,
    ) {
        antiCore.shotsLeft -= 1;
        if(antiCore.shotsLeft <= 0) antiCore.active = false;
        antiCore.save<BaseEntity>()

        antiCore.getStructure().castRay(plugin)

        updateAnchorCharges(block, data, antiCore)
    }

    private fun updateAnchorCharges(block: Block, data: RespawnAnchor, antiCore: AntiCoreEntity) {

        val charges = ((antiCore.shotsLeft - antiCore.shots) / antiCore.shots) * 4

        data.charges = charges.coerceIn(0, 4)

        block.setBlockData(data, false)

        block.world.playSound(block.location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.2f)

        if (antiCore.shotsLeft <= 0) {
            block.type = Material.AIR
            block.world.spawnParticle(Particle.LARGE_SMOKE, block.location.add(0.5, 0.5, 0.5), 10)
        }
    }
}