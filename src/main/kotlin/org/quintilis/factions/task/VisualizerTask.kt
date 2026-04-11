package org.quintilis.factions.task

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.isAntiCore
import org.quintilis.factions.extensions.isCoreItem
import org.quintilis.factions.extensions.isNexusItem
import org.quintilis.factions.services.FactionsServices

@AutoTask(period = 10L, async = true)
class VisualizerTask: BukkitRunnable() {
    private val newTerritoryColor = Particle.DustOptions(Color.LIME, 1.5f)
    private val enemyClanTerritoryColor = Particle.DustOptions(Color.RED, 1.5f)
    private val clanTerritoryColor = Particle.DustOptions(Color.AQUA, 1.5f)

    override fun run() {
        for(player in Bukkit.getOnlinePlayers()) {
            val item = player.inventory.itemInMainHand

            if(!item.hasItemMeta()) continue

            val isAntiCore = item.isAntiCore()

            if(!item.isNexusItem() && !item.isCoreItem() && !isAntiCore) continue

            val clanId = player.getClan()?.id

            if(!isAntiCore) showNewTerritoryBorder(player)
            showCurrentTerritory(player, clanId)
        }
    }

    private fun showCurrentTerritory(player: Player, clanId: Int?){
        val centerChunkX = player.location.chunk.x
        val centerChunkZ = player.location.chunk.z
        val worldUuid = player.world.uid
        val world = player.world
        val y = player.location.blockY.toDouble() + 0.5

        val viewRadius = 4

        for(cx in (centerChunkX - viewRadius)..(centerChunkX+viewRadius)) {
            for(cz in (centerChunkZ-viewRadius)..(centerChunkZ+viewRadius)){
                val chunkOwnerId = FactionsServices.clanChunkCache.getChunkOwner(worldUuid, cx, cz) ?: continue

                val color = if(chunkOwnerId == clanId) clanTerritoryColor else enemyClanTerritoryColor

                val minX = cx * 16;
                val minZ = cz * 16;
                val maxX = minX + 16;
                val maxZ = minZ + 16;

                if(FactionsServices.clanChunkCache.getChunkOwner(worldUuid, cx, cz - 1) != chunkOwnerId) {
                    for(x in minX..maxX){
                        player.spawnParticle(Particle.DUST, Location(world, x.toDouble(), y, minZ.toDouble()), 1, color)
                    }
                }

                if(FactionsServices.clanChunkCache.getChunkOwner(worldUuid, cx, cz + 1) != chunkOwnerId) {
                    for(x in minX..maxX){
                        player.spawnParticle(Particle.DUST, Location(world, x.toDouble(), y, maxZ.toDouble()), 1, color)
                    }
                }

                if(FactionsServices.clanChunkCache.getChunkOwner(worldUuid, cx - 1, cz) != chunkOwnerId) {
                    for(z in minZ..maxZ){
                        player.spawnParticle(Particle.DUST, Location(world, minX.toDouble(), y, z.toDouble()), 1, color)
                    }
                }

                if(FactionsServices.clanChunkCache.getChunkOwner(worldUuid, cx + 1, cz) != chunkOwnerId) {
                    for(z in minZ..maxZ){
                        player.spawnParticle(Particle.DUST, Location(world, maxX.toDouble(), y, z.toDouble()), 1, color)
                    }
                }
            }
        }
    }

    private fun showNewTerritoryBorder(player: Player) {
        val centerChunk = player.location.chunk
        val world = player.world

        val minX = (centerChunk.x -1) * 16
        val minZ = (centerChunk.z -1) * 16
        val maxX = (centerChunk.x + 2) * 16-1
        val maxZ = (centerChunk.z + 2) * 16-1

        val y = player.location.blockY.toDouble()+0.5

        val particleOptions = newTerritoryColor

        for(x in minX..maxX) {
            player.spawnParticle(Particle.DUST, Location(world, x.toDouble(), y, minZ.toDouble()), 1, particleOptions)
            player.spawnParticle(Particle.DUST, Location(world, x.toDouble(), y, maxZ.toDouble()), 1, particleOptions)
        }

        for(z in minZ..maxZ) {
            player.spawnParticle(Particle.DUST, Location(world, minX.toDouble(), y, z.toDouble()), 1, particleOptions)
            player.spawnParticle(Particle.DUST, Location(world, maxX.toDouble(), y, z.toDouble()), 1, particleOptions)
        }
    }
}