package org.quintilis.factions.handlers

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.entity.Player
import org.quintilis.factions.commands.clan.ClaimSubCommands
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.exceptions.points.NotEnoughPointsError
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.CoreService
import org.quintilis.factions.results.Result;

class ClaimCommandHandler(private val coreService: CoreService) {

    fun buy(sender: Player, clan: ClanEntity, args: List<String>) {

        when(args[0]){
            ClaimSubCommands.ClaimBuySubCommands.SUB_CORE.command -> buySubCore(sender, clan)
            ClaimSubCommands.ClaimBuySubCommands.ANTI_CORE.command -> buyAntiCore(sender, clan)
        }
    }

    private fun buySubCore(sender: Player, clan: ClanEntity) {
        if(!clan.havePoints(ConfigManager.getClanCorePrice())){
            throw NotEnoughPointsError()
        }

        clan.removePoints(ConfigManager.getClanCorePrice())


        val coreEntity = ClanCoreEntity(
            clanId = clan.id!!,
            type = CoreType.SUB_CORE
        ).save<ClanCoreEntity>()
        val core = coreService.createExistingNexusItem(coreEntity, sender.locale())
        sender.inventory.addItem(core)
        sender.sendTranslatable("clan.claim.response")
    }

    private fun buyAntiCore(sender: Player, clan: ClanEntity) {
        val price = ConfigManager.getClanAntiCorePrice()

        if (!clan.havePoints(price)) {
            throw NotEnoughPointsError()
        }

        // 1. Remove os pontos do clã
        clan.removePoints(price)

        val antiCoreEntity = AntiCoreEntity(
            clanId = clan.id!!
        ).save<AntiCoreEntity>()

        // 3. Gera o item físico
        val item = coreService.createAntiCoreItem(antiCoreEntity, sender.locale())

        sender.inventory.addItem(item)

        // 4. Feedback
        sender.sendTranslatable(
            "clan.claim.anti_core.purchased",
            Argument.numeric("price", price)
        )
    }
}