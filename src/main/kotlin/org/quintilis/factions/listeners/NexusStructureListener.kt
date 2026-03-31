package org.quintilis.factions.listeners

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.isClanLeader
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.extensions.isNexusItem
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.util.Keys

@AutoRegister
class NexusStructureListener(private val plugin: Factions) : Listener {
    private val coreCache get() = FactionsServices.coreCache
    private val coreService = FactionsServices.coreService
    private val chunkService = FactionsServices.chunkService

    private val BASE_MATERIAL = Material.IRON_BLOCK

    private val REPLACEABLE_MATERIALS = setOf(
        Material.AIR,
        Material.CAVE_AIR,
        Material.VOID_AIR,
        Material.WATER,
        Material.LAVA,
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.PODZOL,
        Material.STONE,
        Material.COBBLESTONE, // Opcional (gera em dungeons, mas players usam muito)
        Material.GRANITE,
        Material.DIORITE,
        Material.ANDESITE,
        Material.DEEPSLATE,
        Material.TUFF,
        Material.SAND,
        Material.RED_SAND,
        Material.GRAVEL,
        Material.CLAY,
        Material.SNOW,
        Material.SNOW_BLOCK,
        Material.ICE,
        Material.PACKED_ICE,
        Material.BLUE_ICE,
        Material.NETHERRACK,
        Material.SOUL_SAND,
        Material.SOUL_SOIL,
        Material.BASALT,
        Material.BLACKSTONE,
        Material.END_STONE,
        // Vegetação
        Material.TALL_GRASS,
        Material.SEAGRASS,
        Material.KELP,
        Material.FERN,
        Material.LARGE_FERN,
        Material.VINE,
        Material.SHORT_GRASS
    )
    /**
     * Listener de evento de quebra de bloco para checagem de quebra de nexus
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val location = block.location
        val player = event.player

        val core = coreCache.findByLocation(location)

        if (core != null) {
//            if(player.hasPermission("factions.admin")) return

            event.isCancelled = true
            player.sendTranslatable("nexus.protect.indestructible")
            return
        }

        if(block.type == Material.IRON_BLOCK) {
            val world = block.world
            val upY = block.y + 1

            for(x in -1..1){
                for(z in -1..1){
                    val checkLocation = Location(
                        world,
                        (block.x + x).toDouble(),
                        upY.toDouble(),
                        (block.z + z).toDouble()
                    )

                    if(coreCache.findByLocation(checkLocation) != null) {
//                        if(player.hasPermission("factions.admin")) return

                        event.isCancelled = true
                        player.sendTranslatable("nexus.protect.indestructible")
                        return
                    }
                }
            }
        }
    }


    private fun cancelEventWithError(event: Cancellable, player: Player, error: Result.Error? = null) {
        event.isCancelled = true
        if(error != null) {
            player.sendTranslatable(error)
        }else{
            val error = Result.Error("error.internal_error")
            player.sendTranslatable(error)
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        try {
            val item = event.itemInHand

            if (!item.isNexusItem()) return

            val nexusId = event.itemInHand.itemMeta.persistentDataContainer
                .get(Keys.NEXUS_ITEM, PersistentDataType.INTEGER)
            if (nexusId == null) {
                //TODO: Mandar a mensagem ao usuario
                event.isCancelled = true
                return
            }
            val clan = event.player.getClanAsLeader()

            if (!event.player.isClanLeader() || clan == null) {
                return cancelEventWithError(event, event.player)
            }

            val core = coreCache.findById(nexusId) ?: return cancelEventWithError(event, event.player)

            coreService.placeCore(event.block.location, core)
            val chunkResult = chunkService.claimChunk(
                player = event.player,
                clan = clan,
                chunk = event.block.chunk,
                core
            )
            if (chunkResult is Result.Error) {
                cancelEventWithError(event, event.player, chunkResult)
            }
            core.save<ClanCoreEntity>()

            val center = event.block.location.clone().subtract(0.0, 1.0, 0.0)
            val world = center.world

            for (x in -1..1) {
                for (z in -1..1) {
                    val block = world.getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                    if (!block.isReplaceable && !REPLACEABLE_MATERIALS.contains(block.type)) {
                        event.isCancelled = true
                        //TODO: fazer funcionar a tradução do bloco usando o minimessages
                        player.sendTranslatable("nexus.place.error")
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)

                        return
                    }

                }
            }

            for (x in -1..1) {
                for (z in -1..1) {
                    val block = world.getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                    block.type = BASE_MATERIAL
                }
            }

            player.sendTranslatable("nexus.place.success")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } catch (e: Exception) {
            event.isCancelled = true
            player.sendTranslatable("nexus.error")
            e.printStackTrace()
        }
    }
}