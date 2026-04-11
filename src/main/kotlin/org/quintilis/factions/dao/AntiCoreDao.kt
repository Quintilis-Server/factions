package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.AntiCoreEntity

interface AntiCoreDao: BaseDao<AntiCoreEntity, Int> {
    @SqlQuery("""
        SELECT * FROM anticore
        WHERE active = true
    """)
    fun findAllActive(): List<AntiCoreEntity>
}