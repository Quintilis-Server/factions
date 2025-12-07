package org.quintilis.factions.entities.invite.member

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.invite.InviteStatus
import java.time.Instant
import java.util.UUID

@TableName("member_invite")
data class MemberInviteEntity(
    @PrimaryKey
    val id: Int? = null,

    @Column("clan_id")
    val clanId: Int,

    @Column("player_id")
    val playerId: UUID,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("expires_at")
    val expiresAt: Instant,

    @Column("active")
    var active: Boolean = true,

    @Column("status")
    var status: InviteStatus = InviteStatus.PENDING,

): BaseEntity() {
    fun getClan(dao: ClanDao): ClanEntity? {
        return dao.findById(clanId)
    }

    fun getPlayer(): Player? {
        return Bukkit.getPlayer(playerId)
    }

    fun getPlayer(dao: PlayerDao): PlayerEntity? {
        return dao.findById(playerId)
    }

    fun accept(): MemberInviteEntity{
        this.active = false;
        this.status = InviteStatus.ACCEPTED
        return this.save()
    }

    fun reject(): MemberInviteEntity{
        this.active = false;
        this.status = InviteStatus.REJECTED
        return this.save()
    }

    fun cancel(): MemberInviteEntity{
        this.active = false;
        this.status = InviteStatus.CANCELLED
        return this.save()
    }
}
