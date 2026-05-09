package org.quintilis.factions.entities.clan

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.annotations.Transient
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.playerCache
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@TableName("clans")
data class ClanEntity(
    @PrimaryKey
//    @Transient
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
    val createdAt: Instant = Instant.now(),
    @Column("points")
    var points: Int = 0,
): BaseEntity(){
    fun getLeader() = Bukkit.getPlayer(leaderUuid)

    fun addPoints(points: Int) {
        this.points += points
        this.save<BaseEntity>()
    }

    fun havePoints(x: Int): Boolean {
        return this.points >= x
    }

    fun removePoints(points: Int) {
        this.points -= points
        this.save<BaseEntity>()
    }

    fun getMembers(): List<ClanMemberEntity>{
        return clanCache.getMembers(this.id!!)
    }

    fun getOnlinePlayers(): List<Player> {
        // Pega todos os membros do cache e filtra apenas os que retornam um Player válido
        return getMembers().mapNotNull { it.getPlayer() }
    }

    /**
     * Retorna as entidades ClanMemberEntity apenas dos jogadores online.
     */
    fun getOnlineMemberEntities(): List<ClanMemberEntity> {
        return getMembers().filter { it.getPlayer() != null }
    }
}