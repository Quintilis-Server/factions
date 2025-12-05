package org.quintilis.factions.entities.clan

import org.bukkit.Bukkit
import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import java.time.OffsetDateTime
import java.util.UUID

@TableName("clans")
data class ClanEntity(
    @PrimaryKey
    @Transient
    val id: Int? = null,
    @Column("name")
    val name: String,
    @Column("tag")
    val tag: String?,
    @Column("leader_uuid")
    val leaderUuid: UUID,
    @Column("active")
    val active: Boolean = true,
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column("points")
    val points: Int = 0,
): BaseEntity(){
    fun getLeader() = Bukkit.getPlayer(leaderUuid)
}