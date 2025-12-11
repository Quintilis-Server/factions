package org.quintilis.factions.services

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.invite.InviteStatus
import org.quintilis.factions.entities.invite.ally.AllyInviteEntity
import org.quintilis.factions.managers.ConfigManager
import java.time.Instant

class AllyInviteService {
    companion object {
        private val maxInviteTime: Instant = Instant.now().plusSeconds(ConfigManager.getMaxAllyInvitationTime() * 86400L)
        fun createInvite(clanDao: ClanDao, clan: ClanEntity, targetId: Int){
            val invite: AllyInviteEntity = AllyInviteEntity(
                targetClanId = targetId,
                senderClanId = clan.id!!,
                status = InviteStatus.PENDING,
            ).save()

            invite.getTargetClan(clanDao)?.getLeader()?.sendMessage {
                Component.translatable(
                    "clan.ally.invite.message",
                    Argument.string("clan_name", clan.name)
                )
            }
        }
    }
}