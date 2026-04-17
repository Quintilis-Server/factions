package org.quintilis.factions.entities.clan

import org.bukkit.Bukkit
import org.bukkit.Location
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.coreCache
import org.quintilis.factions.structure.AntiCoreStructure
import java.time.Instant
import java.util.UUID

@TableName("anticore")
data class AntiCoreEntity(
    @PrimaryKey
    @Column
    val id: Int? = null,

    @Column("clan_id")
    var clanId: Int? = null,

    @Column
    var x: Int? = null,

    @Column
    var y: Int? = null,

    @Column
    var z: Int? = null,

    @Column
    var placed: Boolean = false,

    @Column("target_core_id")
    var targetCoreId: Int? = null,

    @Column("placed_at")
    var placedAt: Instant? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("active")
    var active: Boolean = true,

    @Column("shots")
    val shots: Int = ConfigManager.getAnticoreShots(),

    @Column("shots_left")
    var shotsLeft: Int = ConfigManager.getAnticoreShots(),

    @Column("world_uuid")
    var worldUuid: UUID? = null,

    @Column("glowstone_charges")
    var glowstoneCharges: Int = 0
): BaseEntity() {
    fun getClan(): ClanEntity?{
        return clanCache.getClan(clanId!!)
    }

    fun getAttackerClan(): ClanEntity?{
        return clanCache.getClan(clanId!!)
    }

    fun getTargetClan(): ClanEntity?{
        return clanCache.findByCore(this.targetCoreId!!)
    }

    fun getCore(): ClanCoreEntity?{
        return coreCache.findById(targetCoreId!!)
    }

    fun getLocation(): Location? {
        val uuid = worldUuid ?: return null
        val world = Bukkit.getWorld(uuid) ?: return null
        return Location(world, x!!.toDouble(), y!!.toDouble(), z!!.toDouble())
    }

    fun getStructure(): AntiCoreStructure{
        return AntiCoreStructure.fromEntity(this)
    }

    fun place(attackerClan: ClanEntity, targetCore: ClanCoreEntity, location: Location){
        this.x = location.blockX;
        this.y = location.blockY;
        this.z = location.blockZ;
        this.worldUuid = location.world.uid
        this.placed = true
        this.placedAt = Instant.now()
        this.clanId = attackerClan.id!!
        this.targetCoreId = targetCore.id
        this.save<BaseEntity>()
    }

    fun deactivate(){
        this.active = false;
        this.save<BaseEntity>()
        getStructure().destroy()
    }
}