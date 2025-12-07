package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.player.PlayerEntity
import java.util.UUID

interface PlayerDao: BaseDao<PlayerEntity, UUID> {
    @SqlQuery("SELECT EXISTS (SELECT 1 FROM players WHERE id = :id);")
    fun isInDatabase(@Bind("id")id: UUID): Boolean

    @SqlQuery("SELECT EXISTS( SELECT 1 FROM clan_member WHERE player_id = :playerId AND active = true)")
    fun isInClan(@Bind("playerId") playerId: UUID): Boolean

    @SqlQuery("SELECT EXISTS( SELECT 1 FROM clans WHERE leader_uuid = :playerId AND active = true)")
    fun isClanOwner(@Bind("playerId") playerId: UUID): Boolean
}