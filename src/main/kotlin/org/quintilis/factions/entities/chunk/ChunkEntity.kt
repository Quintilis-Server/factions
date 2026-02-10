package org.quintilis.factions.entities.chunk

import org.bukkit.Bukkit
import org.bukkit.Chunk
import java.util.UUID
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.annotations.Transient

@TableName("chunk")
data class ChunkEntity(
    @PrimaryKey @Transient val id: Int? = null,
    @Column("world_uuid") val worldUuid: UUID,
    @Column("chunk_x") val chunkX: Int,
    @Column("chunk_z") val chunkZ: Int,
) : BaseEntity() {
    constructor(worldUuid: UUID, chunkX: Int, chunkZ: Int) : this(null, worldUuid, chunkX, chunkZ)
    constructor(chunk: Chunk) : this(null,chunk.world.uid, chunk.x, chunk.z)
    fun getChunk(): Chunk?{
        return Bukkit.getWorld(worldUuid)?.getChunkAt(chunkX, chunkZ)
    }
}
