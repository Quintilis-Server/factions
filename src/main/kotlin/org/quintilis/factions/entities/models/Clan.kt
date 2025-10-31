package org.quintilis.factions.entities.models

import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import java.util.*

@TableName("clans")
data class Clan(

    @PrimaryKey
    @Transient
    var id: Int? = null,

    @Column("name")
    val name: String,

    @Column("tag")
    val tag: String,

    @Column("leader_uuid")
    val leaderUuid: UUID

) : BaseEntity("clans")