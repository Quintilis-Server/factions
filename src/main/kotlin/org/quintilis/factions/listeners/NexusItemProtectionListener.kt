package org.quintilis.factions.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.extensions.isNexusItem
import org.quintilis.factions.extensions.sendTranslatable

@AutoRegister
class NexusItemProtectionListener : Listener {

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.isNexusItem()) {
            event.isCancelled = true
            event.player.sendTranslatable("error.nexus_drop")
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        val player = event.whoClicked as Player
        // Verifica se é o Nexus
        if (clickedItem.isNexusItem()) {
            if (event.clickedInventory != event.whoClicked.inventory) {
                event.isCancelled = true
                player.sendTranslatable("error.nexus_leaving_inventory")
            }
        }
    }
}