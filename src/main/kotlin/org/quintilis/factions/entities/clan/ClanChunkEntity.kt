package org.quintilis.factions.entities.clan

import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity

@TableName("clan_chunk")
data class ClanChunkEntity(
    @PrimaryKey
    val id: Int? = null,

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