package org.quintilis.factions.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity

class WarBeginEvent(val receiverClan: ClanEntity, val attackerClan: ClanEntity): BaseClanEvent(receiverClan) {
    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
    override fun getHandlers() = handlerList
}