package org.quintilis.factions.entities.clan

import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import java.time.OffsetDateTime
import java.util.UUID

@TableName("clan_member")
data class ClanMemberEntity(
    @PrimaryKey
    @Column("clan_id")
    val clanId: Int,

    @PrimaryKey
    @Column("player_id")
    val playerId: UUID,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): BaseEntity() {}
