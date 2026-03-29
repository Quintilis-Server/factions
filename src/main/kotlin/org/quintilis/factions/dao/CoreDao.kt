package org.quintilis.factions.dao

import org.bukkit.Location
import org.bukkit.World
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.clan.ClanCoreEntity
import java.util.UUID

interface CoreDao: BaseDao<ClanCoreEntity, Int> {
    @SqlUpdate("UPDATE clan_cores SET active = false, deleted_at = now() WHERE id = :id")
    fun deactivateCore(@Bind("id") id: Int): Int

    @SqlQuery("SELECT * FROM clan_cores WHERE x = :x AND y = :y AND z = :z AND active = true")
    fun findByLocation(@Bind("x") x: Int, @Bind("y") y: Int, @Bind("z") z: Int): ClanCoreEntity?

    fun findByLocation(location: Location): ClanCoreEntity?{
        return this.findByLocation(location.blockX, location.blockY, location.blockZ)
    }

    fun delete(core: ClanCoreEntity, world: World){
        if (core != null){
            val coreBlock = world.getBlockAt(core.getLocation(world)!!)
//            coreBlock.setBlockData()
        }
        this.deactivateCore(core.id!!)
    }

    @SqlQuery("SELECT COUNT(*) FROM clan_cores WHERE clan_id = :clanId AND type = 'SUB_CORE' AND placed = true")
    fun countActiveSubCores(@Bind("clanId") clanId: Int): Int
}