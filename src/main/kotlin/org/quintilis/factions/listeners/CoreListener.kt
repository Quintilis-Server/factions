package org.quintilis.factions.listeners

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.events.CoreDamageEvent
import org.quintilis.factions.extensions.broadcastTranslatable
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.isClanLeader
import org.quintilis.factions.extensions.isCoreItem
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices.chunkService
import org.quintilis.factions.services.FactionsServices.coreCache
import org.quintilis.factions.services.FactionsServices.coreService
import org.quintilis.factions.structure.CoreStructure
import org.quintilis.factions.structure.CoreStructure.Companion.REPLACEABLE_MATERIALS
import org.quintilis.factions.util.Keys

@AutoRegister
class CoreListener: Listener {

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        try{
            val item = event.itemInHand

            if(!item.isCoreItem()) return

            val coreId = event.itemInHand.itemMeta.persistentDataContainer
                .get(Keys.CORE_ITEM, PersistentDataType.INTEGER)
            if(coreId == null){
                event.isCancelled = true
                return
            }
            val clan = player.getClanAsLeader()

            if(!player.isClanLeader() || clan == null){
                return cancelEventWithError(event, event.player)
            }

            val core = coreCache.findById(coreId) ?: return cancelEventWithError(event, player)

            coreService.placeCore(event.blockPlaced.location, core)

            val chunkResult =  chunkService.claimChunk(
                player = player,
                clan = clan,
                chunk = event.blockPlaced.chunk,
                core
            )
            if(chunkResult is Result.Error){
                cancelEventWithError(event, player, chunkResult)
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

            player.sendTranslatable("core.place.success")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } catch (e: Exception) {
            event.isCancelled = true
            player.sendTranslatable("nexus.error")
            e.printStackTrace()
        }
    }
    private fun cancelEventWithError(event: Cancellable, player: Player, error: org.quintilis.factions.results.Result.Error? = null) {
        event.isCancelled = true
        if(error != null) {
            player.sendTranslatable(error)
        }else{
            val error = Result.Error("error.internal_error")
            player.sendTranslatable(error)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onCoreDamage(event: CoreDamageEvent) {
        val core = event.targetCore
        val damage = event.damage

        val anticoreLocation = event.antiCore.getLocation()

        val defenderClan = core.getClan() ?: return
        val attackerClan = event.antiCore.getClan() ?: return

        val onlinePlayers = Bukkit.getOnlinePlayers().map { it.uniqueId }.toSet()

        val hasDefenderOnline = defenderClan.getMembers().any {
            it.playerId in onlinePlayers
        }

        val hasAttackerNearby = if (anticoreLocation != null) {
            attackerClan.getMembers().any { member ->
                if (member.playerId !in onlinePlayers) return@any false
                val player = Bukkit.getPlayer(member.playerId) ?: return@any false
                player.location.distance(anticoreLocation) <= 20.0
            }
        } else false

        if (!hasDefenderOnline || !hasAttackerNearby) {
            event.isCancelled = true
            return
        }

        core.takeDamage(damage)

        val location = core.getLocation() !!
        core.getWorld()?.getNearbyPlayers(location, 20.0)?.forEach { player ->
            player.sendActionBar(Component.text("<yellow>NEXUS: §f${core.health} HP §7(-$damage)"))
        }

        if (core.health <= 0) {
            core.active = false
            core.save<ClanCoreEntity>()
            handleCoreDestruction(core, event.antiCore)
        } else {
            // Apenas salva a vida atual
            core.save<ClanCoreEntity>()
        }

        location.world.spawnParticle(Particle.ELECTRIC_SPARK, location.add(0.5, 1.0, 0.5), 10)
    }
    private fun handleCoreDestruction(core: ClanCoreEntity, attacker: AntiCoreEntity) {
        // Lógica de explosão, remoção do bloco e anúncio global de vitória
        core.deleteCore(true)
        val attackerClan = attacker.getClan()!!
        val targetClan = core.getClan()!!
        //fazer a tradução aq
        attackerClan.broadcastTranslatable("")
        targetClan.broadcastTranslatable("§4§l[!] §fUm Core de §6${core.clanId} §ffoi destruído!")
    }
}