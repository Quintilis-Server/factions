package org.quintilis.factions.dao

import org.bukkit.Chunk
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

    @SqlQuery("""
        SELECT * FROM clan_cores 
        JOIN clan_chunk cc on clan_cores.id = cc.owner_core
        JOIN chunk c on cc.chunk_id = c.id
        where c.chunk_x = :chunk_x AND c.chunk_z = :chunk_z AND cc.active = true
    """)
    fun findByChunk(
        @Bind("chunk_x") chunkX: Int,
        @Bind("chunk_z") chunkZ: Int
    ): ClanCoreEntity?

//    fun findByChunk(chunk: Chunk): ClanCoreEntity?{
//        return this.findByChunk(chunk.x, chunk.z)
//    }

    fun findByLocation(location: Location): ClanCoreEntity?{
        return this.findByLocation(location.blockX, location.blockY, location.blockZ)
    }

    fun delete(core: ClanCoreEntity, world: World){
        if (core != null){
            val coreBlock = world.getBlockAt(core.getLocation()!!)
//            coreBlock.setBlockData()
        }
        this.deactivateCore(core.id!!)
    }

    @SqlQuery("SELECT COUNT(*) FROM clan_cores WHERE clan_id = :clanId AND type = 'SUB_CORE' AND placed = true")
    fun countActiveSubCores(@Bind("clanId") clanId: Int): Int

    @SqlQuery("""
        SELECT cc.* FROM clan_cores as cc
        JOIN public.clans c on c.id = cc.clan_id
        WHERE c.id = :clanId AND c.active = true
    """)
    fun findByClanId(@Bind("clanId") clanId: Int): List<ClanCoreEntity>

    @SqlQuery("""
        SELECT cc.* FROM clan_cores as cc
        JOIN clans c on cc.id = cc.clan_id
        WHERE c.id = :clanId AND c.active = true AND cc.type = 'NEXUS'
    """)
    fun findNexusByClanId(@Bind("clanId") clanId: Int): ClanCoreEntity?

    @SqlQuery("""
        SELECT core.* FROM clan_cores core
        JOIN clan_chunk cc ON cc.owner_core = core.id
        JOIN chunk c ON c.id = cc.chunk_id
        WHERE c.world_uuid = :worldUuid 
        AND c.chunk_x = :chunkX 
        AND c.chunk_z = :chunkZ
        AND cc.active = true 
        AND core.active = true
    """)
    fun findByChunkCoords(
        @Bind("worldUuid") worldUuid: UUID,
        @Bind("chunkX") chunkX: Int,
        @Bind("chunkZ") chunkZ: Int
    ): ClanCoreEntity?

    fun findByChunk(chunk: Chunk): ClanCoreEntity? {
        return this.findByChunkCoords(chunk.world.uid, chunk.x, chunk.z)
    }

    @SqlQuery(
        """
        SELECT core.* FROM clan_cores core
        WHERE ABS(x - :x) < :radius  -- Limite rápido de 50 blocos no eixo X
          AND ABS(z - :z) < :radius
        ORDER BY (
            (x - :x) * (x - :x) +
            (y - :y) * (y - :y) +
            (z - :z) * (z - :z)
        )
        LIMIT 1
    """
    )
    fun findClosestCore(
        @Bind("radius") radius: Int,
        @Bind("x") x: Int,
        @Bind("y") y: Int,
        @Bind("z") z: Int
    ): ClanCoreEntity?

    fun findClosestCore(location: Location, radius: Int): ClanCoreEntity?{
        return this.findClosestCore(radius,location.blockX, location.blockY, location.blockZ)
    }

}