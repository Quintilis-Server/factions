package org.quintilis.factions.entities.player

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.services.FactionsServices.clanCache
import java.util.UUID

@TableName("players")
data class PlayerEntity(
    @PrimaryKey
    val id: UUID,
    @Column("name")
    val name: String,
    @Column("points")
    var points: Int
): BaseEntity() {
    fun getOfflinePlayer(): OfflinePlayer = Bukkit.getOfflinePlayer(id)

    fun getPlayer(): Player? = Bukkit.getPlayer(id)

    fun getClan(): ClanEntity? = clanCache.getClanByMember(id)
}