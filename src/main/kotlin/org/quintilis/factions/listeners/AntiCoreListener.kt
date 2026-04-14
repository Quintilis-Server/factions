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
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
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
import org.quintilis.factions.extensions.broadcastInRadius
import org.quintilis.factions.extensions.broadcastTitleTranslatable
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.services.FactionsServices.clanRelationCache
import redis.clients.jedis.Jedis

@AutoRegister
class AntiCoreListener : Listener {

    @EventHandler
    fun onNpcClick(event: NpcInteractEvent) {
        if (event.interactionType != ActionTrigger.RIGHT_CLICK) return
        // filtra pelo nome ou tag configurada no NPC
        if (event.npc.data.name != "anticore") return
        AntiCoreGUI(event.player).open()
    }

    @EventHandler
    fun onAntiCorePlace(event: BlockPlaceEvent) {
        val player = event.player
        try{
            val itemInHand = event.itemInHand;

            // Verifica se o item possui MetaData
            val meta = itemInHand.itemMeta ?: return

            // Verifica se é realmente um item de AntiCore conferindo a marcação interna dele na PDC
            if (!meta.persistentDataContainer.has(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER)) {
//                println(meta.persistentDataContainer.has(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER))
                return
            }

            // Pega o ID da entidade do AntiCore gravada no item
            val antiCoreId = meta.persistentDataContainer.get(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER) ?: return

            // Pega a exata localização em que o jogador colocou o bloco
            val location = event.blockPlaced.location

            val placerClan = player.getClan() ?: throw ClanNotFoundError()

            //SE ele não achar um core por perto o evento é cancelado
            val targetCore = coreCache.findByChunk(location.chunk)
                ?: throw BaseError("anticore.error.no-influence-zone")

            val targetClan = targetCore.getClan() ?: throw ClanNotFoundError()

            //SE o core for do próprio clã o evento é cancelado
            if(targetClan == placerClan) throw BaseError("anticore.error.attacking-same-clan")

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

            //SE o anticore não esta na área de influencia do core então o evento é cancelado

            val antiCoreEntity = antiCoreCache.findById(antiCoreId)
                ?: throw BaseError("error.anticore-entity-not-found")

            val relation = clanRelationCache.findRelation(placerClan.id, targetClan.id)
            if (relation != null && relation.relation == Relation.ENEMY) {
                // Já é enemy, passa direto
            } else {
                relation?.deactivate() // desativa ally se existir
                ClanRelationEntity(
                    clan1Id = placerClan.id,
                    clan2Id = targetClan.id,
                    relation = Relation.ENEMY,
                ).save<BaseEntity>()
            }

            antiCoreEntity.place(
                attackerClan = placerClan,
                targetCore = targetCore,
                location = location
            )

            if (relation != null && relation.relation == Relation.ENEMY) {
                // Já é enemy, NÃO anuncia guerra de novo
            } else {
                relation?.deactivate()
                ClanRelationEntity(
                    clan1Id = placerClan.id,
                    clan2Id = targetClan.id,
                    relation = Relation.ENEMY,
                ).save<BaseEntity>()

                // Move os broadcasts pra cá
                targetClan.broadcastTitleTranslatable("war.started.attacker.title", "war.started.attacker.subtitle")
                placerClan.broadcastTitleTranslatable("war.started.defender.title", "war.started.defender.subtitle")
                Bukkit.broadcast(Component.translatable(
                    "war.started.broadcast",
                    Argument.string("attacker", placerClan.name),
                    Argument.string("defender", targetClan.name)
                ))
            }
            event.player.sendTranslatable("anticore.placed")
        }catch (e: BaseError){
            event.isCancelled = true
            player.sendTranslatable(e.component)
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
            // Marcamos o BLOCO na memória do servidor (Metadata)
            // O valor pode ser o ID do AntiCore para facilitar a busca depois
            block.setMetadata("factions_exploding_anticore",
                org.bukkit.metadata.FixedMetadataValue(org.bukkit.Bukkit.getPluginManager().getPlugin("Factions")!!, antiCore.id)
            )
            // NÃO definimos active = false aqui ainda!
        }

        antiCore.save<BaseEntity>()
        antiCoreCache.invalidateSpatialCaches(block.location)
    }

    @EventHandler
    fun onAntiCoreExplosion(event: BlockExplodeEvent){
        val block = event.block

        // 1. Verificamos se o bloco tem a nossa "Tag" de memória
        val metadata = block.getMetadata("factions_exploding_anticore")

        if (metadata.isNotEmpty()) {
            // Se tem a tag, é 100% de certeza que é o AntiCore detonando
            val antiCoreId = metadata[0].asInt()

            // 2. PROTEGE OS BLOCOS (Limpa a lista de destruição)
            event.blockList().clear()

            // 3. AGORA sim, desativamos ele no banco de forma segura
            val anticore = antiCoreCache.findById(antiCoreId)
            if (anticore != null && anticore.active) {
                anticore.active = false
                anticore.save<BaseEntity>()
                antiCoreCache.invalidateSpatialCaches(block.location)

                // Avisos e efeitos
                block.world.playSound(block.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f)
                block.location.broadcastInRadius(50.0, Component.translatable("anticore.destroyed.glowstone"))
            }

            // Remove a metadata para limpar a memória
            block.removeMetadata("factions_exploding_anticore", org.bukkit.Bukkit.getPluginManager().getPlugin("Factions")!!)
        }
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
