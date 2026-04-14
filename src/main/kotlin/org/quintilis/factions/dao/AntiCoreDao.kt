package org.quintilis.factions.dao

import org.bukkit.Location
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.AntiCoreEntity
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

    // Método de conveniência atualizado para passar o world.uid
    fun findByLocation(location: Location): AntiCoreEntity? {
        return this.findByLocation(
            location.world.uid,
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }
}