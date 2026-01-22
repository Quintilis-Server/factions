package org.quintilis.factions.dao

import org.bukkit.World
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.clan.ClanCoreEntity
import java.util.UUID

interface CoreDao: BaseDao<ClanCoreEntity, Int> {
    @SqlUpdate("UPDATE clan_cores SET active = false, deleted_at = now() WHERE id = :id")
    fun deactivateCore(@Bind("id") id: Int): Int

    fun delete(core: ClanCoreEntity, world: World){
        if (core != null){
            val coreBlock = world.getBlockAt(core.getLocation(world)!!)
            coreBlock.setBlockData()
        }
        this.deactivateCore(core.id!!)
    }
}