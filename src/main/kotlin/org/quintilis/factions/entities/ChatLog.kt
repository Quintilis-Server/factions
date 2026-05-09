package org.quintilis.factions.entities

import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import java.time.Instant
import java.util.UUID

@TableName("chat_logs")
class ChatLog(
    @PrimaryKey
    val id: Int? = null,

    @Column("player_id")
    val playerId: UUID,

    @Column("content")
    val content: String,

    @Column("channel")
    val channel: String,

    @Column("recipient_id")
    val recipientId: UUID? = null,

    @Column("clan_id")
    val clanId: Int? = null,

    @Column("location_x")
    val locationX: Int? = null,

    @Column("location_z")
    val locationZ: Int? = null,

    @Column("world_uuid")
    val worldUuid: UUID? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
): BaseEntity() {

}