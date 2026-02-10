package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.chunk.ChunkEntity
import java.util.UUID

interface ChunkDao: BaseDao<ChunkEntity, Int> {

    fun getEntityClass() = ChunkEntity::class.java

    /**
     * Busca um chunk por suas coordenadas.
     */
    @SqlQuery("SELECT * FROM chunk WHERE world_uuid = :worldUuid AND chunk_x = :chunkX AND chunk_z = :chunkZ")
    fun findByCoordinates(
        @Bind("worldUuid") worldUuid: UUID,
        @Bind("chunkX") chunkX: Int,
        @Bind("chunkZ") chunkZ: Int
    ): ChunkEntity?

    /**
     * Busca o ID do clã dono de um chunk.
     */
    @SqlQuery("""
        SELECT cc.clan_id 
        FROM clan_chunk cc 
        WHERE cc.chunk_id = :chunkId 
          AND cc.active = true
    """)
    fun findClanIdByChunkId(@Bind("chunkId") chunkId: Int): Int?

    /**
     * Lista todos os chunks ativos de um clã.
     */
    @SqlQuery("""
        SELECT c.* 
        FROM chunk c
            JOIN clan_chunk cc ON c.id = cc.chunk_id
        WHERE cc.clan_id = :clanId 
          AND cc.active = true
    """)
    fun findByClanId(@Bind("clanId") clanId: Int): List<ChunkEntity>

    /**
     * Conta quantos chunks um clã possui.
     */
    @SqlQuery("""
        SELECT COUNT(*) 
        FROM clan_chunk 
        WHERE clan_id = :clanId 
          AND active = true
    """)
    fun countByClanId(@Bind("clanId") clanId: Int): Int

    @SqlQuery("""
        SELECT cc.clan_id FROM clan_chunk cc
            JOIN chunk c ON c.id = cc.chunk_id
        WHERE c.world_uuid = :worldUuid 
            AND c.chunk_x = :x 
            AND c.chunk_z = :z
            AND cc.active = true
    """)
    fun findClanIdByChunkCoordinates(
        @Bind("worldUuid") worldUuid: UUID,
        @Bind("x") x: Int,
        @Bind("z") z: Int
    ): Int?

    @SqlUpdate("""
        INSERT INTO clan_chunk (chunk_id, clan_id, transaction_id, active) 
        VALUES (:chunkId, :clanId, :transactionId, true)
    """)
    @GetGeneratedKeys("id")
    fun claimChunk(
        @Bind("chunkId") chunkId: Int,
        @Bind("clanId") clanId: Int,
        @Bind("transactionId") transactionId: Int
    ): Int

    @SqlUpdate("UPDATE clan_chunk SET active = false WHERE chunk_id = :chunkId AND active = true")
    fun unclaimChunk(@Bind("chunkId") chunkId: Int): Int

    @SqlUpdate("UPDATE clan_chunk SET active = false WHERE clan_id = :clanId AND active = true")
    fun unclaimAllChunksByClan(@Bind("clanId") clanId: Int): Int

    @SqlQuery("""
        SELECT COUNT(*) > 0 
        FROM clan_chunk cc
        JOIN chunk c ON cc.chunk_id = c.id
        WHERE cc.clan_id = :clanId
        AND cc.active = true
        AND c.world_uuid = :worldUuid
        AND (
            (c.chunk_x = :x + 1 AND c.chunk_z = :z) OR 
            (c.chunk_x = :x - 1 AND c.chunk_z = :z) OR 
            (c.chunk_x = :x AND c.chunk_z = :z + 1) OR 
            (c.chunk_x = :x AND c.chunk_z = :z - 1)
        )
    """)
    fun hasNeighboringClaim(
        @Bind("clanId") clanId: Int,
        @Bind("worldUuid") worldUuid: java.util.UUID,
        @Bind("x") x: Int,
        @Bind("z") z: Int
    ): Boolean

    @SqlQuery("SELECT id FROM chunk WHERE chunk_x = :x AND chunk_z = :z AND world_uuid = :worldUuid")
    fun findChunkId(@Bind("x") x: Int, @Bind("z") z: Int, @Bind("worldUuid") worldUuid: UUID): Int?

    @SqlQuery("""
        SELECT COUNT(*) > 0 
        FROM clan_chunk cc
        JOIN chunk c ON cc.chunk_id = c.id
        WHERE c.chunk_x = :x AND c.chunk_z = :z AND c.world_uuid = :worldUuid AND cc.active = true
    """)
    fun isChunkOccupied(@Bind("x") x: Int, @Bind("z") z: Int, @Bind("worldUuid") worldUuid: java.util.UUID): Boolean
}