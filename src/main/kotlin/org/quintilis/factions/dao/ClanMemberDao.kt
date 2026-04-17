package org.quintilis.factions.dao

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.services.FactionsServices.clanMemberCache
import java.util.UUID

interface ClanMemberDao: BaseDao<ClanMemberEntity, Int> {
    @SqlQuery("""
        SELECT 1 FROM clan_member
        WHERE clan_member.id = :clanId
        AND clan_member.player_id = :playerId
        AND active = TRUE
    """)
    fun isMember(
        @Bind("clanId") clanId: Int,
        @Bind("playerId") playerId: UUID
    ): Boolean

    @SqlQuery("""
        SELECT EXISTS(SELECT 1 FROM clan_member WHERE player_id = :playerId AND active = true)
    """)
    fun isAnyMember(@Bind("playerId")playerId: UUID): Boolean

    fun isAnyMember(player: Player): Boolean{
        return clanMemberCache.isAnyMember(player.uniqueId)
    }

    fun isAnyMember(player: OfflinePlayer): Boolean{
        return clanMemberCache.isAnyMember(player.uniqueId)
    }
}