package org.quintilis.factions.entities.models

import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName

@TableName("clans")
data class Clan(

    @PrimaryKey
    @Transient
    var id: Int? = null,

    @Column("name")
    val name: String,

    @Column("tag")
    val tag: String

) : BaseEntity("clans")