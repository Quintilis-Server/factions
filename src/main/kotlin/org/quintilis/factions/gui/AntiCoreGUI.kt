package org.quintilis.factions.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Material
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.exceptions.points.NotEnoughPointsError
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.ErrorManager
import org.quintilis.factions.services.AnticoreService
import org.quintilis.factions.services.FactionsServices

class AntiCoreGUI(player: Player) : BaseGUI(player, "gui.anticore.title", rows = 4, pageSize = 18) {

    private val anticoreService =  AnticoreService()

    override fun loadItems() {
        // AntiCore Setup
        val antiCorePrice = ConfigManager.getClanAntiCorePrice()
        val antiCoreItem = createItem(
            Material.RESPAWN_ANCHOR,
            "gui.anticore.buy_item.name",
            "gui.anticore.buy_item.lore",
            Placeholder.parsed("price", antiCorePrice.toString())
        ).asGuiItem { event ->
            event.isCancelled = true
            gui.close(player)
            
            ErrorManager.runSafe(player) {
                val clan = player.getClanAsLeader() ?: return@runSafe player.sendTranslatable("clan.is_not_leader")

                if (!clan.havePoints(antiCorePrice)) {
                    throw NotEnoughPointsError()
                }

                val coreService = FactionsServices.coreService
                clan.removePoints(antiCorePrice)

                val antiCoreEntity = AntiCoreEntity(
                    clanId = clan.id!!
                ).save<AntiCoreEntity>()

                val itemStack = coreService.createAntiCoreItem(antiCoreEntity, player.locale())
                player.inventory.addItem(itemStack)

                player.sendTranslatable(
                    "clan.claim.anti_core.purchased",
                    Argument.numeric("price", antiCorePrice)
                )
            }
        }
        
        gui.setItem(2, 3, antiCoreItem) // Position shifted left to make room

        // Glowstone Setup
        val glowstonePrice = ConfigManager.getGlowstonePrice()
        val glowstoneItem = createItem(
            Material.GLOWSTONE,
            "gui.anticore.glowstone.name",
            "gui.anticore.glowstone.lore",
            Placeholder.parsed("price", glowstonePrice.toString())
        ).asGuiItem { event ->
            event.isCancelled = true
            
            ErrorManager.runSafe(player) {
                val clan = player.getClanAsLeader() ?: return@runSafe player.sendTranslatable("clan.is_not_leader")

                if (!clan.havePoints(glowstonePrice)) {
                    throw NotEnoughPointsError()
                }

                clan.removePoints(glowstonePrice)
                player.inventory.addItem(org.bukkit.inventory.ItemStack(Material.GLOWSTONE, 1))

                // Optional: a new translation key. If you haven't created it yet, creating in YAML will fix it.
                player.sendTranslatable(
                    "clan.glowstone.purchased",
                    Argument.numeric("price", glowstonePrice)
                )
            }
        }
        
        gui.setItem(2, 5, glowstoneItem) // Flow to the right


        val compassItem = createItem(
            Material.COMPASS,
            "gui.anticore.compass.name",
            "gui.anticore.compass.lore",
            Placeholder.parsed("price", glowstonePrice.toString())
        ).asGuiItem { event ->
            event.isCancelled = true
            ErrorManager.runSafe(player) {
                val compass = anticoreService.createCompassItem(player)

                player.inventory.addItem(compass)
                player.sendTranslatable("anticore.compass.purchased")
            }
        }

        gui.setItem(2, 7, compassItem)
    }
}
