package org.quintilis.factions.managers

import me.neznamy.tab.api.TabAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity

object TagManager {
    private val mm = MiniMessage.miniMessage()
    private val legacy = LegacyComponentSerializer.legacySection()

    private fun toTab(miniMsg: String): String {
        return legacy.serialize(mm.deserialize(miniMsg))
    }

    fun update(player: Player, clan: ClanEntity?) {
        val tab = TabAPI.getInstance()

        val tabPlayer = tab.getPlayer(player.uniqueId) ?: return

        val prefix: String? = if (clan?.tag != null) toTab("<gray>[<dark_purple>${clan.tag}</dark_purple>]</gray> ") else null
//        player.sendMessage("§aTAB: prefix = '$prefix', clan = '${clan?.tag}'")

        val nameTag = tab.nameTagManager
        val tabList = tab.tabListFormatManager
//        player.sendMessage("§aTAB: nameTagManager = $nameTag, tabListFormatManager = $tabList")

        nameTag?.setPrefix(tabPlayer, prefix)
        tabList?.setPrefix(tabPlayer, prefix)
//        player.sendMessage("§aTAB: done!")
    }

    fun remove(player: Player) {
        val tab = TabAPI.getInstance()
        val tabPlayer = tab.getPlayer(player.uniqueId) ?: return

        tab.nameTagManager?.setPrefix(tabPlayer, null)
        tab.tabListFormatManager?.setPrefix(tabPlayer, null)
    }
}