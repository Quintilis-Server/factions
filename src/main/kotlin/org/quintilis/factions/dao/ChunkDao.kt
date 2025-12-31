package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.chunk.ChunkEntity
import java.util.UUID

interface ChunkDao: BaseDao<ChunkEntity, Int> {

    override fun getEntityClass() = ChunkEntity::class.java

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
}