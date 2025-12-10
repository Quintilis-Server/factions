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
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.managers.DatabaseManager
import kotlin.math.ceil

class ClanListMenu(
    player: Player,
    parent: BaseGui? = null,
): BaseGUI(
    player = player,
    titleKey = "clan.list_menu.title",
    pageSize = 45
) {
    private val clanDao = DatabaseManager.getDAO(ClanDao::class)
    private val clanCache = ClanCache(clanDao)

    private var currentPageIndex = 1

    override fun loadItems() {
        loadPage(1)
    }

    private fun loadPage(page: Int) {
        this.currentPageIndex = page
        gui.clearPageItems()
        val offset = (page - 1) * pageSize

        val clans = clanCache.getClans(page, pageSize)

        clans.forEach { clan ->
            val item = ItemBuilder.skull()
                .owner(Bukkit.getOfflinePlayer(clan.leaderUuid))
                .name(
                    trans(
                        "clan.list.item.name",
                        Placeholder.parsed("clan_name", clan.name),
                        Placeholder.parsed("clan_tag", clan.tag ?: "Sem Tag")
                    )
                )
                .lore(
                    transLore(
                        "clan.list.item.lore",
                        Placeholder.parsed("leader_name", Bukkit.getOfflinePlayer(clan.leaderUuid).name ?: "Desconhecido"),
                        Placeholder.parsed("points", clan.points.toString())
                    )
                )
                .asGuiItem()
            gui.addItem(item)
        }

        updateNavigationButton()

        gui.update()
    }

    private fun updateNavigationButton() {
        val totalClans = clanDao.totalClans()
        val totalPages = ceil(totalClans.toDouble() / pageSize).toInt()

        if(currentPageIndex > 1){
            gui.setItem(rows, 3, ItemBuilder.from(Material.PAPER)
                .name(Component.translatable("gui.previous_page"))
                .asGuiItem {
                    loadPage(currentPageIndex -1)
                })
        }else{
            gui.removeItem(rows, 3)
        }

        if(currentPageIndex < totalPages){
            gui.setItem(rows, 7, ItemBuilder.from(Material.PAPER)
                .name(Component.translatable("gui.next_page"))
                .asGuiItem {
                    loadPage(currentPageIndex + 1)
                })
        }else{
            gui.removeItem(rows, 7)
        }
    }
}