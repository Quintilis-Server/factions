package org.quintilis.factions.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity

class MemberAcceptEvent(clan: ClanEntity, val member: Player): BaseClanEvent(clan) {
    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
    override fun getHandlers() = handlerList
}