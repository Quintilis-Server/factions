package org.quintilis.factions.events

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity

class ClanDisbandEvent(
    clan: ClanEntity,
    val leader: Player
): BaseClanEvent(clan) {
    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
}