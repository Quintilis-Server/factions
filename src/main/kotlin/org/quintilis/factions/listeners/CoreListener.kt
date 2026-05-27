package org.quintilis.factions.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
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
import org.quintilis.factions.extensions.isOverworld
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.TranslationManager
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
            if (!item.isCoreItem()) return

            val coreId = item.itemMeta.persistentDataContainer.get(Keys.CORE_ITEM, PersistentDataType.INTEGER)
                ?: return run { event.isCancelled = true }

            val clan = player.getClanAsLeader() ?: return cancelEventWithError(event, player)
            val core = coreCache.findById(coreId) ?: return cancelEventWithError(event, player)

            // --- 1. VALIDAÇÃO FÍSICA (DEVE SER A PRIMEIRA COISA) ---
            val location = event.blockPlaced.location
            val center = location.clone().subtract(0.0, 1.0, 0.0)
            val centerChunk = location.chunk
            val world = location.world

            if(!world.isOverworld()){
                event.isCancelled = true
                return
            }

            for (x in -1..1) {
                for (z in -1..1) {
                    val targetLoc = Location(world, center.x + x, center.y, center.z + z)

                    // Verifica Borda de Chunk
                    if (targetLoc.chunk != centerChunk) {
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return cancelEventWithError(event, player, Result.Error("nexus.place.error_border"))
                    }

                    // Verifica se o terreno é substituível (Pedra/Terra vs Obsidian/Bedrock)
                    val block = world.getBlockAt(targetLoc)
                    if (!block.isReplaceable && !REPLACEABLE_MATERIALS.contains(block.type)) {
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return cancelEventWithError(event, player, Result.Error("nexus.place.error"))
                    }
                }
            }

            // --- 2. VALIDAÇÃO DE SISTEMA (ONION / DISTÂNCIA) ---
            // Aqui você chamaria a lógica da cebola que discutimos antes, se necessário.

            // --- 3. EXECUÇÃO NO BANCO DE DADOS (SÓ SE TUDO ACIMA PASSAR) ---

            // Primeiro tentamos o Claim (se houver um inimigo perto, ele falha aqui e não salva nada)
            val chunkResult = chunkService.claimChunk(
                player = player,
                clan = clan,
                chunk = event.blockPlaced.chunk,
                core = core
            )

            if (chunkResult is Result.Error) {
                return cancelEventWithError(event, player, chunkResult)
            }

            // Se o claim deu certo, salvamos a entidade do core
            coreService.placeCore(location, core)
            core.save<ClanCoreEntity>()

            // --- 4. CONSTRUÇÃO FÍSICA ---
            val structure = CoreStructure.fromCore(core) ?: return cancelEventWithError(event, player)
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
            player.sendActionBar(TranslationManager.render(
                "core.damage",
                player.locale(),
                Placeholder.unparsed("core_health", core.health.toString()),
                Placeholder.unparsed("damage", damage.toString())
            ))
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