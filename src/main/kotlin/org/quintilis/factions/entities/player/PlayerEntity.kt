package org.quintilis.factions.entities.player

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
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
    fun getPlayer(): Player? = Bukkit.getPlayer(id)
}