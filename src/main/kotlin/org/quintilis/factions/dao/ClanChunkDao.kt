package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.entities.clan.ClanChunkEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import java.util.UUID

interface ClanChunkDao: BaseDao<ClanChunkEntity, Int> {
    /**
     * Lista todos os chunks afetados pelo core
     */
    @SqlQuery("""
        SELECT * from clan_chunk
        WHERE owner_core = :ownerId
    """)
    fun findByCoreId(@Bind("ownerId") ownerId: Int): List<ClanChunkEntity>

    fun findByCore(core: ClanCoreEntity): List<ClanChunkEntity>{
        return this.findByCoreId(core.id!!)
    }

    // Descobre quem é o dono de uma coordenada física
    @SqlQuery("""
        SELECT cc.clan_id FROM clan_chunk cc
        JOIN chunk c ON c.id = cc.chunk_id
        WHERE c.world_uuid = :worldUuid AND c.chunk_x = :x AND c.chunk_z = :z AND cc.active = true
    """)
    fun findClanIdByCoordinates(
        @Bind("worldUuid") worldUuid: UUID,
        @Bind("x") x: Int,
        @Bind("z") z: Int
    ): Int?

    // Pega todos os chunks físicos que pertencem a um clã
    @SqlQuery("""
        SELECT c.* FROM chunk c
        JOIN clan_chunk cc ON c.id = cc.chunk_id
        WHERE cc.clan_id = :clanId AND cc.active = true
    """)
    fun findChunksByClanId(@Bind("clanId") clanId: Int): List<ChunkEntity>

    @SqlQuery("""
        SELECT c.* FROM chunk c
        JOIN clan_chunk cchunk ON c.id = cchunk.chunk_id
        JOIN clan_cores cc on cc.id = cchunk.owner_core
        WHERE cc.id = :coreId
    """)
    fun findChunksByCoreId(@Bind("coreId") coreId: Int): List<ChunkEntity>

    // Conta quantos chunks o clã tem
    @SqlQuery("SELECT COUNT(*) FROM clan_chunk WHERE clan_id = :clanId AND active = true")
    fun countByClanId(@Bind("clanId") clanId: Int): Int

    // Remove a posse de UM chunk específico (Unclaim)
    @SqlUpdate("UPDATE clan_chunk SET active = false WHERE chunk_id = :chunkId AND active = true")
    fun unclaimChunk(@Bind("chunkId") chunkId: Int)

    // Remove a posse de TODOS os chunks de um clã (Disband)
    @SqlUpdate("UPDATE clan_chunk SET active = false WHERE clan_id = :clanId AND active = true")
    fun unclaimAllChunksByClan(@Bind("clanId") clanId: Int): Int
}