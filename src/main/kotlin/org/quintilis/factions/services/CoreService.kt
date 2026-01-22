package org.quintilis.factions.services

import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.Factions
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.item.setGlowing

class CoreService(private val plugin: Factions) {

    private val nexusKey = NamespacedKey(plugin, "nexus_core_item")

    private val coreDao = Services.coreCache

    fun createItem(clan: ClanEntity): ItemStack {
        val core: ClanCoreEntity = ClanCoreEntity(
            clanId = clan.id!!,
            type = CoreType.NEXUS,
        ).save()
        val item = ItemBuilder.from(Material.BEACON)
            .name(Component.translatable("nexus.name", Argument.component("tag", Component.text(clan.tag?:clan.name))))
            .pdc { pdc ->
                pdc.set(nexusKey, PersistentDataType.STRING, core.id.toString())
            }
            .build()
        return item
    }
    fun createItem(nexusEntity: ClanCoreEntity): ItemStack {
        val item = ItemBuilder.from(Material.BEACON)
            .name(Component.translatable("nexus.name"))
            .pdc { pdc ->
                pdc.set(nexusKey, PersistentDataType.STRING, nexusEntity.id.toString())
            }
            .build()
        item.setGlowing(true)
        return item
    }
}