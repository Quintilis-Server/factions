package org.quintilis.factions.entities.clan

import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.services.FactionsServices.chunkCache
import org.quintilis.factions.services.FactionsServices.clanChunkCache

@TableName("clan_chunk")
data class ClanChunkEntity(
    @PrimaryKey
    @Column("id")
    val id: Int? = null,

    @Column("chunk_id")
    val chunkId: Int,

    @Column("clan_id")
    val clanId: Int,

    // Fundamental para sua lógica: Quem segura esse claim é o Core
    @Column("owner_core")
    val ownerCore: Int,

    // Nullable porque "Claimar com bloco" não gera transação de mercado
    @Column("transaction_id")
    val transactionId: Int? = null,

    @Column("active")
    var active: Boolean = true
): BaseEntity() {
    fun getChunkEntity(): ChunkEntity? {
        return chunkCache.findById(chunkId)
    }

    fun declaim(){
        this.active = false
        this.save<ClanCoreEntity>()
        val chunkRel = getChunkEntity()!!
        clanChunkCache.invalidateChunk(chunkRel.worldUuid, chunkRel.chunkX, chunkRel.chunkZ)
    }
}