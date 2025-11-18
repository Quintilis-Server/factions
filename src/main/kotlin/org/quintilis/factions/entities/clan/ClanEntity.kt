package org.quintilis.factions.entities.clan

import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.BaseEntity
import java.util.UUID

data class ClanEntity(
    @PrimaryKey
    @Transient
    val id: Int,
    @Column("name")
    val name: String,
    @Column("tag")
    val tag: String,
    @Column("leader_uuid")
    val leaderUuid: UUID
): BaseEntity(){}