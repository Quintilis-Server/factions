package org.quintilis.factions.entities.invite.ally

import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.enums.InviteStatus
import java.time.Instant

@TableName("ally_invite")
data class AllyInviteEntity(
    @PrimaryKey()
    val id: Int? = null,

    @Column("sender_clan_id")
    val senderClanId: Int,

    @Column("target_clan_id")
    val targetClanId: Int,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),

    @Column("expires_at")
    val expiresAt: Instant,

    @Column("active")
    val active: Boolean = true,

    @Column("status")
    val status: InviteStatus,

    ): BaseEntity(){
    fun getSenderClan(clanCache: ClanCache): ClanEntity?{
        return clanCache.findById(senderClanId)
    }

    fun getTargetClan(clanCache: ClanCache): ClanEntity?{
        return clanCache.findById(targetClanId)
    }
}