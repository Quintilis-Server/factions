package org.quintilis.factions.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.BaseGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.managers.DatabaseManager
import org.quintilis.factions.services.FactionsServices.clanCache
import kotlin.math.ceil

class ClanListMenu(
    player: Player,
    plugin: JavaPlugin,
    parent: BaseGui? = null,
): BaseGUI(
    player = player,
    titleKey = "clan.list_menu.title",
    pageSize = 45,
    plugin = plugin,
) {

    override fun loadItems() {
        loadPageData(
            page = 1,
            fetcher = {
                clanCache.getClans((currentPageIndex - 1) * pageSize, pageSize)
            },
            totalFetcher = {
                clanCache.getTotalClans()
            },
            itemMapper = { clan ->
                ItemBuilder.skull()
                    .owner(Bukkit.getOfflinePlayer(clan.leaderUuid))
                    // ... etc
                    .asGuiItem()
            }
        )
    }

//    private fun updateNavigationButton() {
//        val totalClans = clanCache.getTotalClans()
//        val totalPages = ceil(totalClans.toDouble() / pageSize).toInt()
//
//        if(currentPageIndex > 1){
//            gui.setItem(rows, 3, ItemBuilder.from(Material.PAPER)
//                .name(Component.translatable("gui.previous_page"))
//                .asGuiItem {
//                    loadPage(currentPageIndex -1)
//                })
//        }else{
//            gui.removeItem(rows, 3)
//        }
//
//        if(currentPageIndex < totalPages){
//            gui.setItem(rows, 7, ItemBuilder.from(Material.PAPER)
//                .name(Component.translatable("gui.next_page"))
//                .asGuiItem {
//                    loadPage(currentPageIndex + 1)
//                })
//        }else{
//            gui.removeItem(rows, 7)
//        }
//    }
}