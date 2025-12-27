package org.quintilis.factions.entities.clan

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.player.PlayerEntity
import java.time.OffsetDateTime
import java.util.UUID

@TableName("clan_member")
data class ClanMemberEntity(
    @PrimaryKey
    val id: Int? = null,

    @Column("clan_id")
    val clanId: Int,

    @Column("player_id")
    val playerId: UUID,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("active")
    val active: Boolean = true
): BaseEntity() {
    fun getPlayer(): Player?{
        return Bukkit.getPlayer(playerId)
    }

    fun getPlayer(dao: PlayerDao): PlayerEntity? {
        return dao.findById(playerId)
    }

    fun getClan(dao: ClanDao): ClanEntity? {
        return dao.findById(clanId)
    }
}
