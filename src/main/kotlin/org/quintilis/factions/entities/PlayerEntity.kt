package org.quintilis.factions.entities

import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import java.util.UUID

@TableName("players")
data class PlayerEntity(
    @PrimaryKey
    val id: UUID,
    @Column("name")
    val name: String,
    @Column("points")
    var points: Int
): BaseEntity() {}