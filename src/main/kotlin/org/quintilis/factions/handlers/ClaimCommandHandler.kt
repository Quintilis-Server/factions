package org.quintilis.factions.handlers

import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.services.CoreService

class ClaimCommandHandler(private val coreService: CoreService) {

    fun buy(sender: Player, clan: ClanEntity) {
        val core = coreService.createItem(clan)

        sender.inventory.addItem(core)
        sender.sendTranslatable("clan.claim.response")
    }
}