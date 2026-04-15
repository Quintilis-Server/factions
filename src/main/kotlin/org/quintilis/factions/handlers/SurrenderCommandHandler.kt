package org.quintilis.factions.handlers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.extensions.broadcastTitleTranslatable
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.FactionsServices

class SurrenderCommandHandler {

    private val clanCache get() = FactionsServices.clanCache
    private val clanRelationCache get() = FactionsServices.clanRelationCache
    private val antiCoreDao get() = FactionsServices.antiCoreDao
    private val antiCoreCache get() = FactionsServices.antiCoreCache
    private val clanRelationDao get() = FactionsServices.clanRelationDao

    fun surrender(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }

        val targetClanName = args[0]
        val targetClan = clanCache.findByName(targetClanName)

        if (targetClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }

        if (targetClan.id == clan.id) {
            sender.sendTranslatable("clan.surrender.error.same_clan")
            return
        }

        // Verifica se estão em guerra
        if (!clanRelationCache.isRelation(clan.id!!, targetClan.id!!, Relation.ENEMY)) {
            sender.sendTranslatable("clan.surrender.error.not_at_war")
            return
        }

        // Descobre o papel do clã na guerra
        val myAnticores = antiCoreDao.findActiveByAttackerAndTarget(clan.id, targetClan.id)
        val theirAnticores = antiCoreDao.findActiveByAttackerAndTarget(targetClan.id, clan.id)

        val isAttacker = myAnticores.isNotEmpty()
        val isDefender = theirAnticores.isNotEmpty()

        // Atacante desistindo: desativa seus anticores
        if (isAttacker) {
            for (anticore in myAnticores) {
                // Remove o bloco físico
                val location = anticore.getLocation()
                if (location != null) {
                    val block = location.block
                    if (block.type == Material.RESPAWN_ANCHOR) {
                        block.type = Material.AIR
                    }
                    antiCoreCache.invalidateSpatialCaches(location)
                }

                // Desativa no banco
                anticore.active = false
                anticore.save<BaseEntity>()
            }
        }

        // Defensor desistindo: destrói os cores que estão sendo atacados
        if (isDefender) {
            for (anticore in theirAnticores) {
                val targetCore = anticore.getCore() ?: continue

                // Destrói o core (com drop de loot)
                targetCore.deleteCore(true)

                // Desativa o anticore também
                val anticoreLocation = anticore.getLocation()
                if (anticoreLocation != null) {
                    val block = anticoreLocation.block
                    if (block.type == Material.RESPAWN_ANCHOR) {
                        block.type = Material.AIR
                    }
                    antiCoreCache.invalidateSpatialCaches(anticoreLocation)
                }

                anticore.active = false
                anticore.save<BaseEntity>()
            }
        }

        // Remove a relação ENEMY
        clanRelationCache.removeRelation(clan.id, targetClan.id)

        // Broadcasts
        if (isAttacker) {
            // Atacante se rendeu
            clan.broadcastTitleTranslatable(
                "war.ended.attacker_surrendered.title",
                "war.ended.attacker_surrendered.subtitle"
            )
            targetClan.broadcastTitleTranslatable(
                "war.ended.defender_won.title",
                "war.ended.defender_won.subtitle"
            )
        } else if (isDefender) {
            // Defensor se rendeu
            clan.broadcastTitleTranslatable(
                "war.ended.defender_surrendered.title",
                "war.ended.defender_surrendered.subtitle"
            )
            targetClan.broadcastTitleTranslatable(
                "war.ended.attacker_won.title",
                "war.ended.attacker_won.subtitle"
            )
        }

        Bukkit.broadcast(Component.translatable(
            "war.ended.broadcast",
            Argument.string("surrendered", clan.name),
            Argument.string("winner", targetClan.name)
        ))

        sender.sendTranslatable("clan.surrender.response", Argument.string("clan_name", targetClan.name))
    }

    fun getSuggestions(clan: ClanEntity): List<String> {
        return clanRelationDao.findRelatedClanNames(clan.id!!, Relation.ENEMY)
    }
}
