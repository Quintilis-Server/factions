package org.quintilis.factions.handlers

import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.exceptions.points.NotEnoughPointsError
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.CoreService
import org.quintilis.factions.services.FactionsServices.coreCache

class ClaimCommandHandler(private val coreService: CoreService) {

    fun buy(sender: Player, clan: ClanEntity) {
        if(!clan.havePoints(ConfigManager.getClanCorePrice())){
            throw NotEnoughPointsError()
        }

        clan.removePoints(ConfigManager.getClanCorePrice())

        val coreEntity = ClanCoreEntity(
            clanId = clan.id!!,
            type = CoreType.SUB_CORE
        ).save<ClanCoreEntity>()
        val core = coreService.createExistingNexusItem(coreEntity, sender.locale())
        
        val leftovers = sender.inventory.addItem(core)
        if(leftovers.isNotEmpty()) {
            leftovers.values.forEach { leftover ->
                sender.world.dropItem(sender.location, leftover)
            }
        }
        
        sender.sendTranslatable("clan.claim.response")
    }
    fun nexus(sender: Player, clan: ClanEntity) {
        val nexusCore = coreCache.findNexusByClanId(clan.id!!)

        if (nexusCore == null) {
            sender.sendTranslatable("error.generic")
            return
        }

        if (nexusCore.placed) {
            sender.sendTranslatable("clan.claim.nexus.already_placed")
            return
        }

        val nexusItem = coreService.createExistingNexusItem(nexusCore, sender.locale())
        
        val leftovers = sender.inventory.addItem(nexusItem)
        if(leftovers.isNotEmpty()) {
            leftovers.values.forEach { leftover ->
                sender.world.dropItem(sender.location, leftover)
            }
        }
        
        sender.sendTranslatable("clan.claim.nexus.success")
    }
}