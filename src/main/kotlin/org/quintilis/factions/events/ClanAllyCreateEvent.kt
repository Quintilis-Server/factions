package org.quintilis.factions.events

import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity

class ClanAllyCreateEvent(clan: ClanEntity, val clan2: ClanEntity): BaseClanEvent(clan) {
    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
    override fun getHandlers() = handlerList
}