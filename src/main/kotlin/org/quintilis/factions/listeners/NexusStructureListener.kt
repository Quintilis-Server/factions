package org.quintilis.factions.listeners

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.isClanLeader
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.extensions.isNexusItem
import org.quintilis.factions.extensions.isOverworld
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.structure.CoreStructure
import org.quintilis.factions.structure.CoreStructure.Companion.REPLACEABLE_MATERIALS
import org.quintilis.factions.util.Keys

@AutoRegister
class NexusStructureListener(private val plugin: Factions) : Listener {
    private val coreCache get() = FactionsServices.coreCache
    private val coreService = FactionsServices.coreService
    private val chunkService = FactionsServices.chunkService


    private fun isProtected(block: org.bukkit.block.Block): Boolean {
        val core = coreCache.findByChunk(block.chunk) ?: return false

        // 2. FILTRO DE MATERIAL (Opcional, mas ajuda na performance)
        // Se o bloco não for o Core (Beacon/Enchant) nem Ferro/Diamante, não precisamos checar a estrutura.
        val type = block.type
        val isPossibleStructure = type == Material.IRON_BLOCK ||
                type == Material.DIAMOND_BLOCK ||
                type == Material.BEACON // Adicione o material real do seu Core aqui

        if (!isPossibleStructure) return false

        // 3. VERIFICAÇÃO PELA ESTRUTURA
        // Perguntamos para a classe CoreStructure se esse bloco faz parte dela.
        // Isso protege tanto o bloco do Core quanto os 9 blocos da base.
        return CoreStructure.fromCore(core)?.isPartOfStructure(block) ?: false
    }
    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { isProtected(it) }
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { isProtected(it) }
    }

    /**
     * Listener de evento de quebra de bloco para checagem de quebra de nexus
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if(this.isProtected(event.block)){
            event.isCancelled = true
            event.player.sendTranslatable("nexus.protect.indestructible")
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

            if(!event.blockPlaced.location.world.isOverworld()){
                return cancelEventWithError(event, event.player, Result.Error("error.only_overworld"))
            }

            val clan = event.player.getClanAsLeader()

            if (!event.player.isClanLeader() || clan == null) {
                return cancelEventWithError(event, event.player)
            }

            val core = coreCache.findById(nexusId) ?: return cancelEventWithError(event, event.player)

            coreService.placeCore(event.blockPlaced.location, core)
            val chunkResult = chunkService.claimChunk(
                player = event.player,
                clan = clan,
                chunk = event.block.chunk,
                core
            )
            if (chunkResult is Result.Error) {
                cancelEventWithError(event, event.player, chunkResult)
                return
            }
            core.save<ClanCoreEntity>()

            val center = event.block.location.clone().subtract(0.0, 1.0, 0.0)
            val centerChunk = center.chunk
            val world = center.world

            for (x in -1..1) {
                for (z in -1..1) {

                    val targetLoc = Location(world, center.blockX + x.toDouble(), center.blockY.toDouble(), center.blockZ + z.toDouble())

                    // 1. ANTI-LEAK: Se o bloco for cair fora do chunk central, bloqueia!
                    if (targetLoc.chunk != centerChunk) {
                        event.isCancelled = true
                        player.sendTranslatable("nexus.place.error_border") // Crie essa tradução: "Você não pode colocar o Core tão perto da borda do Chunk!"
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return
                    }

                    val block = world.getBlockAt(center.blockX + x, center.blockY, center.blockZ + z)
                    if (!block.isReplaceable && !REPLACEABLE_MATERIALS.contains(block.type)) {
                        event.isCancelled = true
                        player.sendTranslatable("nexus.place.error")
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return
                    }

                }
            }

            val structure: CoreStructure = CoreStructure.fromCore(core) ?: run {
                cancelEventWithError(event, player)
                return
            }
            structure.placeStructure()

            player.sendTranslatable("nexus.place.success")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } catch (e: Exception) {
            event.isCancelled = true
            player.sendTranslatable("nexus.error")
            e.printStackTrace()
        }
    }
}