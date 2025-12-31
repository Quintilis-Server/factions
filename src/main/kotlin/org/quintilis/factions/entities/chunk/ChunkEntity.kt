package org.quintilis.factions.entities.chunk

import java.util.UUID
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import org.quintilis.factions.entities.annotations.Transient

@TableName("chunk")
data class ChunkEntity(
        @PrimaryKey @Transient val id: Int? = null,
        @Column("world_uuid") val worldUuid: UUID,
        @Column("chunk_x") val chunkX: Int,
        @Column("chunk_z") val chunkZ: Int,
) : BaseEntity()
