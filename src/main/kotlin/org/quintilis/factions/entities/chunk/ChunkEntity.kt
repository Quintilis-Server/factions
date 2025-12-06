package org.quintilis.factions.entities.chunk

import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import java.util.UUID

@TableName("chunk")
data class ChunkEntity(
    @PrimaryKey
    @Transient
    val id: Int? = null,
    @Column("world_uuid")
    val worldUuid: UUID,
    @Column("chunk_x")
    val chunkX: Int,
    @Column("chunk_z")
    val chunkZ: Int,
) : BaseEntity(){

}