package org.quintilis.factions.dao

import org.bukkit.Location
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import java.util.UUID

interface AntiCoreDao: BaseDao<AntiCoreEntity, Int> {
    @SqlQuery("""
        SELECT * FROM anticore
        WHERE active = true
    """)
    fun findAllActive(): List<AntiCoreEntity>

    @SqlQuery("""
        SELECT * FROM anticore
        WHERE world_uuid = :world
        AND x = :x
        AND y = :y
        AND z = :z
        AND active = TRUE
        LIMIT 1
    """)
    fun findByLocation(
        @Bind("world") world: UUID,
        @Bind("x") x: Int,
        @Bind("y") y: Int,
        @Bind("z") z: Int
    ): AntiCoreEntity?

    @SqlQuery("""
        SELECT ac.* FROM anticore ac
        JOIN clan_cores cc ON ac.target_core_id = cc.id
        WHERE ac.clan_id = :attackerClanId
        AND cc.clan_id = :targetClanId
        AND ac.active = TRUE
        AND ac.placed = TRUE
    """)
    fun findActiveByAttackerAndTarget(
        @Bind("attackerClanId") attackerClanId: Int,
        @Bind("targetClanId") targetClanId: Int
    ): List<AntiCoreEntity>

    @SqlQuery("""
        SELECT * FROM anticore
        WHERE clan_id = :clanId
        AND active = TRUE
        AND placed = TRUE
    """)
    fun findActiveByClanId(@Bind("clanId") clanId: Int): List<AntiCoreEntity>

    // Método de conveniência atualizado para passar o world.uid
    fun findByLocation(location: Location): AntiCoreEntity? {
        return this.findByLocation(
            location.world.uid,
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }

    @SqlQuery("""
        SELECT a.* FROM anticore a
        JOIN clan_cores c ON a.target_core_id = c.id
        WHERE a.active = true AND (
            (a.clan_id = :clan1 AND c.clan_id = :clan2) OR
            (a.clan_id = :clan2 AND c.clan_id = :clan1)
        )
    """)
    fun findActiveBetween(@Bind("clan1") clan1: Int, @Bind("clan2") clan2: Int): List<AntiCoreEntity>

    @SqlUpdate("""
        UPDATE anticore a
        SET active = false
        FROM clan_cores c
        WHERE a.target_core_id = c.id
        AND a.active = true
        AND (
            (a.clan_id = :clan1 AND c.clan_id = :clan2) OR
            (a.clan_id = :clan2 AND c.clan_id = :clan1)
        )
    """)
    fun deactivateAllBetween(@Bind("clan1") clan1: Int, @Bind("clan2") clan2: Int)

    @SqlQuery("""
        SELECT a.* FROM anticore a
        JOIN clan_cores cc ON a.target_core_id = cc.id
        WHERE cc.clan_id = :clanId 
        AND a.active = TRUE
    """)
    fun findAllAttackingClan(@Bind("clanId") clanId: Int): List<AntiCoreEntity>

    fun findAllAttackingClan(clan: ClanEntity): List<AntiCoreEntity>{
        return this.findAllAttackingClan(clan.id!!)
    }
}