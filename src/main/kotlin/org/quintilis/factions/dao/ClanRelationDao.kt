package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.entities.clan.Relation

interface ClanRelationDao: BaseDao<ClanRelationEntity, Int> {
    @SqlQuery("""
        SELECT EXISTS(
            SELECT 1 FROM clan_relation 
            WHERE clan1_id = :clanId 
            AND clan2_id = :targetId 
            AND relation = :relation
            AND active = true
        )
    """)
    fun isRelation(
        @Bind("clanId")
        clanId: Int,
        @Bind("targetId")
        targetId: Int,
        @Bind("relation")
        relation: Relation
    ): Boolean

    @SqlUpdate("""
        INSERT INTO clan_relation (clan1_id, clan2_id, relation)
        VALUES (:clan1Id, :clan2Id, :relation)
    """)
    fun createRelation(
        @Bind("clan1Id") clan1Id: Int,
        @Bind("clan2Id") clan2Id: Int,
        @Bind("relation") relation: Relation
    )

    @SqlUpdate("""
        DELETE FROM clan_relation 
        WHERE (clan1_id = :clan1Id AND clan2_id = :clan2Id)
        OR (clan1_id = :clan2Id AND clan2_id = :clan1Id)
    """)
    fun removeRelation(
        @Bind("clan1Id") clan1Id: Int,
        @Bind("clan2Id") clan2Id: Int
    )

    @SqlQuery("""
        SELECT c.name FROM clans c
        INNER JOIN clan_relation cr ON (cr.clan2_id = c.id OR cr.clan1_id = c.id)
        WHERE (cr.clan1_id = :clanId OR cr.clan2_id = :clanId)
        AND c.id != :clanId
        AND cr.relation = :relation
        AND cr.active = true
        AND c.active = true
    """)
    fun findRelatedClanNames(
        @Bind("clanId") clanId: Int,
        @Bind("relation") relation: Relation
    ): List<String>
}