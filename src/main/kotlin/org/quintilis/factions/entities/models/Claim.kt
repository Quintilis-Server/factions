package org.quintilis.factions.entities.models

import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName

@TableName("chunk_claims")
data class Claim(

    @PrimaryKey
    @Transient
    var id: Int? = null,

    @Column("world")
    val world: String,

    @Column("chunk_x")
    val chunkX: Int,

    @Column("chunk_z")
    val chunkZ: Int,

    @Column("clan_id")
    val clanId: Int?
): BaseEntity("chunk_claims")