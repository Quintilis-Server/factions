package org.quintilis.factions.entities.clan

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.services.FactionsServices.chunkCache
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.clanChunkCache
import org.quintilis.factions.services.FactionsServices.clanChunkDao
import org.quintilis.factions.structure.CoreStructure
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

    fun getOwnChunk(): Chunk{
        val world = getWorld()
        val location = this.getLocation(world)!!
        return world.getChunkAt(location)
    }

    fun getOwnedChunks(): List<ChunkEntity> {
        return clanChunkCache.findChunksByCoreId(this.id!!)
    }

    fun getWorld(): World {
        val chunks = chunkCache.getChunksByClan(clanId)
        val world = Bukkit.getWorld(chunks.get(0).worldUuid)!!
        return world
    }

    fun takeDamage(amount: Int): Boolean {
        this.health -= amount
        return this.health <= 0
    }

    fun getStructure(): CoreStructure {
        return CoreStructure.fromCore(this)
    }

    fun deleteCore(){
        val structure = getStructure()
        this.deletedAt = Instant.now()
        this.active = false

        val chunks = clanChunkDao.findByCore(this)

        chunks.forEach {

        }
        this.save<BaseEntity>()
        structure.destroyStructure()
    }

    fun getClan(): ClanEntity?{
        return clanCache.findById(clanId)
    }

//    fun updateLocation(location: Location): ClanCoreEntity {
//        this.x = location.x.toInt()
//        this.y = location.y.toInt()
//        this.z = location.z.toInt()
//
//        return this
//    }
}