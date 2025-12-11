package org.quintilis.factions.entities.clan

import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import java.time.Instant

@TableName("clan_relation")
data class ClanRelationEntity(
    @PrimaryKey
    val id: Int?,

    @Column("clan1_id")
    val clan1Id: Int,

    @Column("clan2_id")
    val clan2Id: Int,

    @Column("relation")
    val relation: Relation,

    @Column("active")
    val active: Boolean,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
): BaseEntity() {
}