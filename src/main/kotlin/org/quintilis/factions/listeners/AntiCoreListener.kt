package org.quintilis.factions.listeners

import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.exceptions.BaseError
import org.quintilis.factions.exceptions.clan.ClanNotFoundError
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.gui.AntiCoreGUI
import org.quintilis.factions.services.FactionsServices.antiCoreCache
import org.quintilis.factions.services.FactionsServices.coreCache
import org.quintilis.factions.util.Keys
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.extensions.broadcastInRadius
import org.quintilis.factions.extensions.broadcastTitleTranslatable
import org.quintilis.factions.extensions.isOverworld
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.services.FactionsServices.clanRelationCache
import redis.clients.jedis.Jedis

@AutoRegister
class AntiCoreListener(val plugin: JavaPlugin) : Listener {

    @EventHandler
    fun onNpcClick(event: NpcInteractEvent) {
        if (event.interactionType != ActionTrigger.RIGHT_CLICK) return
        // filtra pelo nome ou tag configurada no NPC
        if (event.npc.data.name != "anticore") return
        AntiCoreGUI(event.player, plugin = plugin).open()
    }

    @EventHandler
    fun onAntiCorePlace(event: BlockPlaceEvent) {
        val player = event.player
        try {
            val itemInHand = event.itemInHand
            val meta = itemInHand.itemMeta ?: return

            if (!meta.persistentDataContainer.has(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER)) return

            val antiCoreId = meta.persistentDataContainer.get(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER) ?: return
            val location = event.blockPlaced.location
            if(!location.world.isOverworld()){
                event.isCancelled = true

                player.sendTranslatable("error.only_overworld")
            }



            val placerClan = player.getClan() ?: throw ClanNotFoundError()

            val targetCore = coreCache.findByChunk(location.chunk)
                ?: throw BaseError("anticore.error.no-influence-zone")

            if (targetCore.type == CoreType.NEXUS) {
                val targetClan = targetCore.getClan() ?: throw ClanNotFoundError()

                val hasSubCore = coreCache.hasActiveSubCores(targetClan.id!!)
                if (hasSubCore) {
                    throw BaseError("anticore.error.nexus-protected")
                }
            }

            val targetClan = targetCore.getClan() ?: throw ClanNotFoundError()

            if (targetClan == placerClan) throw BaseError("anticore.error.attacking-same-clan")

            // Lógica de Traição (Aliados)
            val isAlly = clanRelationCache.isRelation(placerClan.id!!, targetClan.id!!, Relation.ALLY)
            if (isAlly) {
                val redisKey = "factions:betray:intent:${placerClan.id}:${targetClan.id}"
                val hasIntent = RedisManager.run { jedis: Jedis -> jedis.exists(redisKey) }

                if (!hasIntent) {
                    throw BaseError("clan.betray.require_command")
                } else {
                    clanRelationCache.removeRelation(placerClan.id, targetClan.id)
                    RedisManager.run { jedis: Jedis -> jedis.del(redisKey) }
                }
            }

            val antiCoreEntity = antiCoreCache.findById(antiCoreId)
                ?: throw BaseError("error.anticore-entity-not-found")

            // --- LÓGICA DE RELAÇÃO CONSOLIDADA ---
            val relation = clanRelationCache.findRelation(placerClan.id, targetClan.id)

            // Se NÃO for inimigo, criamos a relação e anunciamos a guerra
            if (relation == null || relation.relation != Relation.ENEMY) {
                relation?.deactivate() // Desativa relação antiga (ex: neutral ou o que sobrar)

                ClanRelationEntity(
                    clan1Id = placerClan.id,
                    clan2Id = targetClan.id,
                    relation = Relation.ENEMY,
                ).save<BaseEntity>()

                // Broadcasts de Início de Guerra
                targetClan.broadcastTitleTranslatable("war.started.attacker.title", "war.started.attacker.subtitle")
                placerClan.broadcastTitleTranslatable("war.started.defender.title", "war.started.defender.subtitle")
                Bukkit.broadcast(Component.translatable(
                    "war.started.broadcast",
                    Argument.string("attacker", placerClan.name),
                    Argument.string("defender", targetClan.name)
                ))
            }

            // Posiciona o AntiCore no mundo/banco
            antiCoreEntity.place(
                attackerClan = placerClan,
                targetCore = targetCore,
                location = location
            )

            event.player.sendTranslatable("anticore.place.success", Argument.string("defender_clan", targetClan.name))

        } catch (e: BaseError) {
            event.isCancelled = true
            player.sendTranslatable(e.component)
        } catch (e: Exception) {
            event.isCancelled = true
            e.printStackTrace()
        }
    }

    @EventHandler
    fun onAnchorInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.RESPAWN_ANCHOR || event.action != Action.RIGHT_CLICK_BLOCK) return
        val item = event.item ?: return
        if (item.type != Material.GLOWSTONE) return

        val antiCore = antiCoreCache.findByLocation(block.location) ?: return

        antiCore.glowstoneCharges += 1

        // 5ª Carga: O bloco VAI explodir agora
        if (antiCore.glowstoneCharges >= 5) {
            event.isCancelled = true

            if (event.hand == org.bukkit.inventory.EquipmentSlot.HAND) {
                item.amount -= 1
            }

            block.type = Material.AIR

            if(antiCore.active){
                antiCore.active = false
                antiCore.save<BaseEntity>()
                antiCoreCache.invalidateSpatialCaches(block.location)

                val loc = block.location
                loc.world.spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, loc.clone().add(0.5, 0.5, 0.5), 1)
                loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f)

                loc.broadcastInRadius(50.0, Component.translatable(
                    "anticore.destroyed.glowstone",
                    Argument.string("attacker_clan", antiCore.getClan()?.name!!),
                    Argument.string("destroyer_player", event.player.name) // Agora você tem o player de forma fácil!
                ))
            }
            return
        }

        antiCore.save<BaseEntity>()
        antiCoreCache.invalidateSpatialCaches(block.location)
    }

//    @EventHandler
//    fun onAntiCoreExplosion(event: BlockExplodeEvent){
//        val block = event.block
//
//        // 1. Verificamos se o bloco tem a nossa "Tag" de memória
//        val metadata = block.getMetadata("factions_exploding_anticore")
//        println(metadata)
//        if (metadata.isNotEmpty()) {
//            // Se tem a tag, é 100% de certeza que é o AntiCore detonando
//            val antiCoreId = metadata[0].asInt()
//
//            // 2. PROTEGE OS BLOCOS (Limpa a lista de destruição)
//            event.blockList().clear()
//
//            // 3. AGORA sim, desativamos ele no banco de forma segura
//            val anticore = antiCoreCache.findById(antiCoreId)
//            if (anticore != null && anticore.active) {
//                anticore.active = false
//                anticore.save<BaseEntity>()
//                antiCoreCache.invalidateSpatialCaches(block.location)
//
//                // Avisos e efeitos
//                block.world.playSound(block.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f)
//                block.location.broadcastInRadius(50.0, Component.translatable(
//                    "anticore.destroyed.glowstone",
//                    Argument.string("attacker_clan", anticore.getClan()?.name!!),
////                    Argument.string("destroyer_player", event.)
//                ))
//            }
//
//            // Remove a metadata para limpar a memória
//            block.removeMetadata("factions_exploding_anticore", Bukkit.getPluginManager().getPlugin("Factions")!!)
//        }
//    }

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        // Verifica se algum dos blocos sendo empurrados é um AntiCore
        if (event.blocks.any { isAntiCore(it) }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        // Verifica se algum dos blocos sendo puxados (sticky piston) é um AntiCore
        if (event.blocks.any { isAntiCore(it) }) {
            event.isCancelled = true
        }
    }

    // Função auxiliar privada para deixar o código limpo
    private fun isAntiCore(block: org.bukkit.block.Block): Boolean {
        // Primeiro um filtro rápido de performance: o AntiCore é um RESPAWN_ANCHOR
        if (block.type != Material.RESPAWN_ANCHOR) return false

        // Se for âncora, vai no cache checar se está registrado
        return antiCoreCache.findByLocation(block.location) != null
    }

    @EventHandler
    fun onAntiCoreBlockBreak(event: BlockBreakEvent){
        val block = event.block
        if(block.type != Material.RESPAWN_ANCHOR) return

        val anticore = antiCoreCache.findByLocation(block.location) ?: return

        anticore.active = false
        anticore.save<BaseEntity>()

        antiCoreCache.invalidateSpatialCaches(block.location)
        event.player.sendTranslatable("anticore.removed")
        block.world.spawnParticle(Particle.SMOKE, block.location.add(0.5, 0.5, 0.5), 20)
    }
}
