package org.quintilis.factions.entities.models

import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import java.util.*

@TableName("clan_members")
data class ClanMember(

    @PrimaryKey
    @Column("clan_id")
    val clanId: Int,

    @PrimaryKey
    @Column("player_id")
    val playerId: UUID,

    @Column("created_at")
    val createdAt: java.time.OffsetDateTime? = null

) : BaseEntity("clan_members")
