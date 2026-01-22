package org.quintilis.factions.listeners

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.item.isNexusItem
import org.quintilis.factions.services.Services
import org.quintilis.factions.util.Keys
import java.time.Instant

@AutoRegister
class NexusStructureListener(private val plugin: Factions) : Listener {
    private val coreDao get() = Services.coreDao

    private val nexusKey = NamespacedKey(plugin, "nexus_core_item")

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

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand

        if (!item.isNexusItem()) return

        val player = event.player
        val center = event.block.location.clone().subtract(0.0, 1.0, 0.0)
        val world = center.world

        for (x in -1..1){
            for (z in -1..1){
                val block = world.getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                if(!block.isReplaceable && !REPLACEABLE_MATERIALS.contains(block.type)){
                    event.isCancelled = true

                    player.sendTranslatable("nexus.place.error")
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)

                    return
                }
            }
        }

        for(x in -1..1){
            for (z in -1..1){
                val block = world.getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                block.type = BASE_MATERIAL
            }
        }

        val nexusId = event.itemInHand.itemMeta.persistentDataContainer
            .get(Keys.NEXUS_ITEM, PersistentDataType.STRING)?.toIntOrNull() ?: return

        val core = coreDao.findById(nexusId)

        if (core != null){
            core.placed = true
            core.placedAt = Instant.now()
            core.x = event.block.x
            core.y = event.block.y
            core.z = event.block.z

            core.save<ClanCoreEntity>()
        }
        player.sendTranslatable("nexus.place.success")
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
    }
}