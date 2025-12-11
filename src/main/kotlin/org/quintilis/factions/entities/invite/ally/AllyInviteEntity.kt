package org.quintilis.factions.entities.invite.ally

import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.invite.InviteStatus
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

    @Column("active")
    val active: Boolean = true,

    @Column("status")
    val status: InviteStatus,

): BaseEntity(){
    fun getSenderClan(clanDao: ClanDao): ClanEntity?{
        return clanDao.findById(senderClanId)
    }

    fun getTargetClan(clanDao: ClanDao): ClanEntity?{
        return clanDao.findById(targetClanId)
    }
}