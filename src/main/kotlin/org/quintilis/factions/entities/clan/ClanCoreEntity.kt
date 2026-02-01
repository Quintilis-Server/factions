package org.quintilis.factions.entities.clan

import org.bukkit.Location
import org.bukkit.World
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.enums.CoreType
import java.time.Instant

@TableName("clan_cores")
data class ClanCoreEntity(
    @PrimaryKey
    val id: Int? = null, // Null ao criar, Preenchido ao ler do DB

    @Column("clan_id")
    val clanId: Int,

    @Column("type")
    val type: CoreType, // Seu ORM precisa saber converter Enum <-> String

    @Column("parent_core")
    val parentCoreId: Int? = null, // Null se for NEXUS

    @Column("health")
    var health: Int = 1000,

    @Column
    var x: Int? = null,

    @Column
    var y: Int? = null,

    @Column
    var z: Int? = null,

    // --- Auditoria / Soft Delete ---
    @Column
    var active: Boolean = true,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("deleted_at")
    var deletedAt: Instant? = null,

    @Column
    var placed: Boolean = false,

    @Column("placed_at")
    var placedAt: Instant? = null,

) : BaseEntity() {
    fun getLocation(world: World): Location? {
        if(placed && x != null && y != null && z != null) {
            return Location(world, x!!.toDouble(), y!!.toDouble(), z!!.toDouble())
        }
        return null
    }
    fun takeDamage(amount: Int): Boolean {
        this.health -= amount
        return this.health <= 0
    }

    fun updateLocation(location: Location): ClanCoreEntity {
        this.x = location.x.toInt()
        this.y = location.y.toInt()
        this.z = location.z.toInt()

        return this
    }
}