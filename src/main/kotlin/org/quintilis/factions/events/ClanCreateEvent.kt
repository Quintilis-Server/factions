package org.quintilis.factions.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity

class ClanCreateEvent(
    val player: Player,
    clan: ClanEntity
): BaseClanEvent(clan) {
    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
}