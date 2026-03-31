package org.quintilis.factions.extensions

import org.bukkit.Chunk
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.services.FactionsServices.chunkCache
import org.quintilis.factions.services.FactionsServices.coreCache

fun Chunk.getCore(): ClanCoreEntity? {
    return coreCache.findByChunk(this)
}

fun Chunk.toEntity(): ChunkEntity {
    val chunk: ChunkEntity = chunkCache.getChunk(this.world.uid, this.x, this.z) ?:
        ChunkEntity(this).save()
    return chunk
}