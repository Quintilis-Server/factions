package org.quintilis.factions.task

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.events.CoreDamageEvent
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.QuintilisScheduler
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.services.FactionsServices.antiCoreCache
import org.quintilis.factions.services.FactionsServices.coreCache
import kotlin.math.pow

@AutoTask(configPath = "anticore.time", period = 20L)
class AntiCoreDamageTask(private val plugin: Factions): BukkitRunnable() {
    override fun run() {
        val anticores = antiCoreCache.findAllActive()
        val radiusSq = ConfigManager.getAnticoreActivationRadius().pow(2)
        anticores.forEachIndexed { index, anticore ->
            val delay = (index * 2L) + 1
            val location = anticore.getLocation() ?: return@forEachIndexed

            QuintilisScheduler.runDelayedAtLocation(plugin, location, Runnable {
                val location = anticore.getLocation() ?: return@Runnable
                val block = location.block
                val data = block.blockData
                if(data is RespawnAnchor){

                    val attackerClanId = anticore.clanId

                    val isAttackerNear = location.world.players.any { player ->
                        val playerClan = player.getClan()
                        playerClan?.id == attackerClanId &&
                        player.location.distanceSquared(location) < radiusSq
                    }

                    if (!isAttackerNear) {
                        block.world.spawnParticle(Particle.SMOKE, location.add(0.5, 0.8, 0.5), 1)
                        return@Runnable
                    }
                    val warKey = "factions:war:heartbeat:${anticore.clanId}:${anticore.getTargetClan()?.id!!}"
                    RedisManager.run { jedis ->
                        jedis.setex(warKey, 3600, System.currentTimeMillis().toString()) // Expira em 30min se ninguém renovar
                    }

                    val targetCore = anticore.getCore() ?: return@Runnable

                    if (targetCore.type == CoreType.NEXUS && coreCache.hasActiveSubCores(targetCore.clanId)) {
                        // Efeito visual de que o Nexus está invulnerável
                        block.world.spawnParticle(Particle.ENCHANT, block.location.add(0.5, 1.0, 0.5), 10)
                        return@Runnable
                    }

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
            }, delay)
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

        antiCore.getStructure().castRay()

        updateAnchorCharges(block, data, antiCore)
    }

    private fun updateAnchorCharges(block: Block, data: RespawnAnchor, antiCore: AntiCoreEntity) {

        val charges = ((antiCore.shots - antiCore.shotsLeft).toFloat() / antiCore.shots.toFloat() * 4).toInt()

        data.charges = charges.coerceIn(0, 4)

        block.setBlockData(data, false)

        block.world.playSound(block.location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.2f)

        if (antiCore.shotsLeft <= 0) {
            block.type = Material.AIR
            block.world.spawnParticle(Particle.LARGE_SMOKE, block.location.add(0.5, 0.5, 0.5), 10)
        }
    }
}