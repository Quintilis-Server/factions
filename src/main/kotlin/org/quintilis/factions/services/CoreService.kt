package org.quintilis.factions.services

import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.Factions
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.extensions.setGlowing
import org.quintilis.factions.managers.TranslationManager
import org.quintilis.factions.util.Keys
import java.time.Instant
import java.util.Locale

class CoreService(private val plugin: Factions) {

    private val coreDao = FactionsServices.coreCache

    fun placeCore(location: Location, core: ClanCoreEntity) {
        core.placed = true
        core.placedAt = Instant.now()
        core.x = location.x.toInt()
        core.y = location.y.toInt()
        core.z = location.z.toInt()
        core.worldUuid = location.world.uid
    }

    private fun getNexusDisplayName(tag: String, locale: Locale): Component {
        // Usamos o renderizador do MiniMessage com um Placeholder chamado "tag"
        return TranslationManager.render(
            "nexus.name",
            locale,
            Placeholder.unparsed("tag", tag) // Isso substitui o <tag> no seu YAML pelo texto real
        )
    }

    private fun getAnticoreDisplayName(tag: String, locale: Locale): Component {
        return TranslationManager.render(
            "anticore.name",
            locale,
            Placeholder.unparsed("tag", tag)
        )
    }

    fun createExistingNexusItem(nexusEntity: ClanCoreEntity, locale: Locale): ItemStack {
        val keyType = when(nexusEntity.type) {
            CoreType.NEXUS -> Keys.NEXUS_ITEM
            CoreType.SUB_CORE -> Keys.CORE_ITEM
        }
        val clan = nexusEntity.getClan()!!
        val nameComponent = this.getNexusDisplayName(clan.tag ?: clan.name, locale)

        val item = ItemBuilder.from(Material.BEACON)
            .name(nameComponent)
            .pdc { pdc ->
                pdc.set(keyType, PersistentDataType.INTEGER, nexusEntity.id!!)
            }
            .build()
        item.setGlowing(nexusEntity.type == CoreType.NEXUS)
        return item
    }

    fun createAntiCoreItem(antiCore: AntiCoreEntity, locale: Locale): ItemStack{
        val clan = antiCore.getClan()!!
        val nameComponent = this.getAnticoreDisplayName(clan.tag ?: clan.name, locale)

        val item = ItemBuilder.from(Material.RESPAWN_ANCHOR)
            .name(nameComponent)
            .pdc {
                it.set(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER, antiCore.id!!)
            }
            .build()
        return item
    }

//    fun placeNexus(item: ItemStack, block: Block, clan: ClanEntity) {
//        val pdcValue = item.itemMeta.persistentDataContainer.get(Keys.NEXUS_ITEM, PersistentDataType.STRING)
//
//        var core: ClanCoreEntity
//
//        if (pdcValue == "NEW"){
//            core = ClanCoreEntity(
//                clanId = clan.id!!,
//                type = CoreType.NEXUS,
//            ).save()
//        }else{
//            val id = pdcValue?.toIntOrNull() ?: return
//            core = coreDao.findById(id) ?: return
//        }
//
//        core = core.updateLocation(block.location)
//
//
//
//        core.save<ClanCoreEntity>()
//    }
}