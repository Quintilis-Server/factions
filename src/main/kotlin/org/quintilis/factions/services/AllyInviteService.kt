package org.quintilis.factions.services

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.quintilis.factions.cache.AllyInviteCache
import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.invite.InviteStatus
import org.quintilis.factions.entities.invite.ally.AllyInviteEntity
import org.quintilis.factions.exceptions.invite.AlreadyInvitedError
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.DatabaseManager
import java.time.Instant

class AllyInviteService {
    companion object {
        val allyInviteDao : AllyInviteDao = DatabaseManager.getDAO(AllyInviteDao::class)
        val allyInviteCache = AllyInviteCache(allyInviteDao)
        private val maxInviteTime: Instant = Instant.now().plusSeconds(ConfigManager.getMaxAllyInvitationTime() * 86400L)

        fun createInvite(clanDao: ClanDao, clan: ClanEntity, target: ClanEntity){
            if(allyInviteDao.hasInvite(clan.id!!, targetId = target.id!!)){
                throw AlreadyInvitedError(target.name)
            }
            val invite: AllyInviteEntity = AllyInviteEntity(
                targetClanId = target.id,
                senderClanId = clan.id,
                status = InviteStatus.PENDING,
                expiresAt = maxInviteTime,
            ).save()

            // Invalida o cache para forçar atualização na próxima busca
            allyInviteCache.invalidate(target.id)
            allyInviteCache.invalidate(clan.id)

            invite.getTargetClan(clanDao)?.getLeader()?.sendMessage {
                Component.translatable(
                    "clan.ally.invite.message",
                    Argument.string("clan_name", clan.name)
                )
            }
        }
    }
}