package org.quintilis.factions.dao

import org.bukkit.Location
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.AntiCoreEntity

interface AntiCoreDao: BaseDao<AntiCoreEntity, Int> {
    @SqlQuery("""
        SELECT * FROM anticore
        WHERE active = true
    """)
    fun findAllActive(): List<AntiCoreEntity>

    @SqlQuery("""
        SELECT * FROM anticore
        WHERE x = :x
        AND y = :y
        AND z = :z
        AND active = TRUE
    """)
    fun findByLocation(
        @Bind("x") x: Int,
        @Bind("y") y: Int,
        @Bind("z") z: Int
    ): AntiCoreEntity?

    fun findByLocation(location: Location): AntiCoreEntity? {
        return this.findByLocation(location.blockX, location.blockY, location.blockZ)
    }
}