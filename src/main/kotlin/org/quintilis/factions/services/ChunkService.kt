package org.quintilis.factions.services

import org.bukkit.Chunk
import org.quintilis.factions.dao.ChunkDao
import org.quintilis.factions.entities.chunk.ChunkEntity

class ChunkService {

    val chunkCache = Services.chunkCache
    val chunkDao = Services.chunkDao

    fun getChunk(chunk: Chunk): ChunkEntity? {
        return chunkCache.getChunk(chunk)
    }

    fun getOrInsert(chunk: Chunk): ChunkEntity {
        return this.getChunk(chunk) ?: ChunkEntity(chunk).save()
    }
}