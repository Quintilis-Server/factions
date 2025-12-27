package org.quintilis.factions.services

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.cache.MemberInviteCache
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.entities.invite.member.MemberInviteEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.exceptions.invite.AlreadyInvitedError
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.DatabaseManager
import java.time.Instant

class MemberInviteService {
    companion object {
        val memberInviteDao: MemberInviteDao = DatabaseManager.getDAO(MemberInviteDao::class)
        val maxInvitationTime: Instant = Instant.now().plusSeconds(ConfigManager.getMaxInvitationTime() * 60.toLong())
        fun createInvite(clan: ClanEntity, player: PlayerEntity): MemberInviteEntity{
            if(memberInviteDao.hasInvite(player.id, clan.id!!)){
                throw AlreadyInvitedError(player.name)
            }

            val invite = MemberInviteEntity(
                clanId = clan.id,
                playerId = player.id,
                expiresAt = this.maxInvitationTime
            ).save<MemberInviteEntity>()

            player.getOnlinePlayer()?.sendMessage {
                Component.translatable(
                    "clan.invite.invitation_text",
                    Argument.string("clan_name", clan.name)
                )
            }

            return invite
        }
        fun acceptInvite(dao: MemberInviteDao, clan: ClanEntity, player: Player): ClanMemberEntity {
            val inviteEntity = dao.findByPlayerIdAndClanId(player.uniqueId, clan.id!!) ?: throw Error("Invite not found")
            inviteEntity.accept()
            return ClanMemberEntity(clanId = clan.id, playerId = player.uniqueId).save()
        }
        
        fun rejectInvite(dao: MemberInviteDao, clan: ClanEntity, player: Player){
            val inviteEntity = dao.findByPlayerIdAndClanId(player.uniqueId, clan.id!!) ?: throw Error("Invite not found")
            inviteEntity.reject()
        }
    }
}