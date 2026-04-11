package org.quintilis.factions.structure

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.factory.CoreLootFactory
import java.util.UUID
import kotlin.random.Random

class CoreStructure(
    val worldUUID: UUID,
    val chunkX: Int,
    val chunkZ: Int,
    val core: ClanCoreEntity,
    val structureLocation: MutableMap<Location, Material> = mutableMapOf()
) {
    companion object {
         val REPLACEABLE_MATERIALS = setOf(
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


        fun fromCore(core: ClanCoreEntity): CoreStructure {
            val world = core.getWorld()
            val chunk = core.getOwnChunk()
            val location = core.getLocation()
            val structure = CoreStructure(
                worldUUID = world.uid,
                chunkX = chunk.x,
                chunkZ = chunk.z,
                core = core
            )
            if (location != null) {
                val center = location.clone().subtract(0.0, 1.0, 0.0)
                val coreMaterial = when(core.type) {
                    CoreType.NEXUS -> Material.DIAMOND_BLOCK
                    CoreType.SUB_CORE -> Material.IRON_BLOCK
                }
                for (x in -1..1) {
                    for (z in -1..1) {
                        val location = center.clone().add(x.toDouble(), 0.0, z.toDouble())
                        structure.structureLocation[location] = coreMaterial
                    }
                }
            }
            return structure
        }
    }
    fun getWorld(): World? {
        return Bukkit.getWorld(worldUUID)
    }

    fun getChunk(): Chunk? {
        return getWorld()?.getChunkAt(chunkX, chunkZ)
    }

    fun isPartOfStructure(targetBlock: Block): Boolean {
        if(targetBlock.world.uid != worldUUID) return false

        val targetLocation = targetBlock.location

        if(targetLocation == core.getLocation()) return true

        return structureLocation.containsKey(targetLocation)
    }

    fun destroyStructure(){
        val coreLocation = core.getLocation()!!
        val world = coreLocation.world

        coreLocation.block.type = Material.AIR
        for(loc in structureLocation.keys){
            loc.block.type = Material.AIR
        }
        world.spawnParticle(Particle.EXPLOSION, coreLocation.add(0.5, 0.5, 0.5), 1)
        world.playSound(coreLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)

        val rewards = CoreLootFactory.getDestructionLoot(core.type)

        for (item in rewards) {
            // Dropa os itens levemente para cima para dar um efeito de "explosão de loot"
            world.dropItemNaturally(coreLocation.clone().add(0.5, 1.0, 0.5), item).apply {
                velocity = org.bukkit.util.Vector(
                    (Random.nextDouble() - 0.5) * 0.2,
                    0.4,
                    (Random.nextDouble() - 0.5) * 0.2
                )
            }
        }
    }

    fun placeStructure(){
        val world = getWorld() ?: return
        val center = (core.getLocation() ?: return).clone().subtract(0.0, 1.0, 0.0)

        val coreMaterial = when(core.type){
            CoreType.NEXUS -> Material.DIAMOND_BLOCK
            CoreType.SUB_CORE -> Material.IRON_BLOCK
        }

        for (x in -1..1) {
            for (z in -1..1) {
                val location = center.clone().add(x.toDouble(), 0.0, z.toDouble())
                val block = world.getBlockAt(location)
                block.type = coreMaterial
                structureLocation[location] = coreMaterial
            }
        }
    }
}