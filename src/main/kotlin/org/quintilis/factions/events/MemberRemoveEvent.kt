package org.quintilis.factions.events

import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.player.PlayerEntity

class MemberRemoveEvent(clan: ClanEntity, val member: PlayerEntity): BaseClanEvent(clan) {
    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
    override fun getHandlers() = handlerList
}