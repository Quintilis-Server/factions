package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.entities.clan.Relation

interface ClanRelationDao: BaseDao<ClanRelationEntity, Int> {
    @SqlQuery("""
        SELECT EXISTS(
            SELECT 1 FROM clan_relations 
            WHERE clan1_id = :clanId 
            AND clan2_id = :targetId 
            AND relation = :relation
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
}