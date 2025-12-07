package org.quintilis.factions.services

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.invite.member.MemberInviteEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.managers.ConfigManager
import java.time.Instant

class MemberInviteService {
    companion object {
        val maxInvitationTime: Instant = Instant.now().plusSeconds(ConfigManager.getMaxInvitationTime() * 60.toLong())
        fun createInvite(clan: ClanEntity, player: PlayerEntity): MemberInviteEntity{
            val invite = MemberInviteEntity(
                clanId = clan.id!!,
                playerId = player.id,
                expiresAt = this.maxInvitationTime
            ).save<MemberInviteEntity>()

            player.getPlayer()?.sendMessage {
                Component.translatable(
                    "clan.invite.invitation_text",
                    Argument.string("clan_name", clan.name)
                )
            }

            return invite
        }
    }
}