package org.quintilis.factions.entities.models

import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import java.util.*

@TableName("Player")
data class Player(

    @PrimaryKey
    @Transient
    var id: UUID? = null,

    @Column("name")
    val name: String
) : BaseEntity("Player")
