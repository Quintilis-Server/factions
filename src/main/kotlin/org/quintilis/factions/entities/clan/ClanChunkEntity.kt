package org.quintilis.factions.entities.clan

import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.TableName

@TableName("clan_chunk")
data class ClanChunkEntity(
    @Column("chunk_id")
    val chunkId: Int,

    @Column("clan_id")
    val clanId: Int,

    @Column("transaction_id")
    val transactionId: Int,

    @Column("active")
    val active: Boolean,
): BaseEntity() {
}