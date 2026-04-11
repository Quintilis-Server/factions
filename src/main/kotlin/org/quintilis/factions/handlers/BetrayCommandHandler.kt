package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.services.FactionsServices

class BetrayCommandHandler {

    private val clanCache get() = FactionsServices.clanCache
    private val clanRelationDao get() = FactionsServices.clanRelationDao

    fun betray(sender: Player, clan: ClanEntity, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendTranslatable("error.missing_arguments")
            return
        }

        val targetClanName = args[0]
        val targetClan = clanCache.getClanByName(targetClanName)

        if (targetClan == null) {
            sender.sendTranslatable("error.no_clan")
            return
        }

        if (targetClan.id == clan.id) {
            sender.sendTranslatable("clan.betray.error.same_clan")
            return
        }

        // Must be an ally to betray
        if (!clanRelationDao.isRelation(clan.id!!, targetClan.id!!, Relation.ALLY)) {
            sender.sendTranslatable("clan.betray.error.not_ally")
            return
        }

        val redisKey = "factions:betray:intent:${clan.id}:${targetClan.id}"

        RedisManager.run { jedis ->
            jedis.setex(redisKey, 60, "true")
        }

        sender.sendTranslatable("clan.betray.response", Argument.string("clan_name", targetClan.name))
    }

    fun getSuggestions(clan: ClanEntity): List<String> {
        return clanRelationDao.findRelatedClanNames(clan.id!!, Relation.ALLY)
    }
}
