package org.quintilis.factions.services

import dev.triumphteam.gui.builder.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.extensions.setGlowing
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.TranslationManager
import org.quintilis.factions.util.Keys

class AnticoreService {
    fun createCompassItem(player: Player): ItemStack {
        val locale =  player.locale()
        val nameComponent = TranslationManager.render(
            "anticore.compass.name",
            locale
        )

        val loreComponents = TranslationManager.renderList("anticore.compass.lore",locale)

        val item = ItemBuilder.from(Material.COMPASS)
            .name(nameComponent)
            .pdc { pdc ->
                pdc.set(Keys.ANTI_CORE_COMPASS_EXPIRY, PersistentDataType.LONG, -1L)
            }
            .lore(loreComponents)
            .build()
        item.setGlowing(true)
        return item
    }
}