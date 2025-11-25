package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.managers.DatabaseManager

class ClanCommand: BaseCommand(
    name = "clan",
    description = "Main clan command",
    usage = "/clan <subcommand>",
    aliases = listOf("c"),
    commands = ClanCommands.entries
) {
    private val clanDao = DatabaseManager.getDAO(ClanDao::class)
    private val playerDao = DatabaseManager.getDAO(PlayerDao::class)

    //
    // Errors
    //
    private fun noClanLeader(sender: CommandSender){
        sender.sendMessage {
            Component.translatable(
                "clan.is_not_leader"
            )
        }
    }

    //
    // Clan commands
    //

    private fun create(sender: CommandSender, args: List<String> ) {
        sender as Player

        if(args.isEmpty()) {
            this.argumentsMissing(sender)
            return
        }

        if(playerDao.isInClan(sender.uniqueId) || playerDao.isClanOwner(sender.uniqueId)) {
            sender.sendMessage {
                Component.translatable(
                    "clan.already_in_clan"
                )
            }
            return
        }
        val name = args[0]
        val tag = args.getOrNull(1)
        if(clanDao.existsByName(name)){
            sender.sendMessage {
                Component.translatable(
                    "clan.create.error.already_exists",
                    Argument.string("clan_name", name)
                )
            }
           return
        }

        val clan = ClanEntity(name = name, tag = tag, leaderUuid = sender.uniqueId).save<ClanEntity>()

        sender.sendMessage {
            Component.translatable(
                "clan.create.response",
                Argument.string("clan_name", clan.name)
            )
        }
    }

    private fun delete(sender: CommandSender) {
        sender as Player
        if(!playerDao.isClanOwner(sender.uniqueId)){
            return this.noClanLeader(sender)
        }

        val clan = clanDao.findByLeaderId(sender.uniqueId) ?: return this.noClanLeader(sender)
        val clanMembers = clanDao.findMembersByClan(clan.id!!)
        try{
            clanDao.deleteByIdAndLeader(clan.id, sender.uniqueId)
        }catch(e: Exception){
            sender.sendMessage(
                Component.text("Error").color(NamedTextColor.RED)
            )
            e.printStackTrace()
        }

        clanMembers.forEach {
            Bukkit.getPlayer(it.playerId)?.sendMessage {
                Component.translatable(
                    "clan.delete.member_response",
                    Argument.string("leader_name",sender.name)
                )
            }
        }

        sender.sendMessage {
            Component.translatable(
                "clan.delete.response",
            )
        }
    }

    override fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean {
        commandSender as Player
        when(args[0].lowercase()){
            ClanCommands.CREATE.command -> this.create(commandSender, args.drop(1))
            ClanCommands.DELETE.command -> this.delete(commandSender)
            ClanCommands.ALLY.command ->{
                when(args[1].lowercase()){
                    AllySubCommands.ADD.command ->{}
                }
            }
            else -> this.unknownSubCommand(commandSender, args[0])
        }
        return true
    }

    override fun onTabComplete(
        commandSender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val suggestions = mutableListOf<String>()

        when (args.size) {
            1 -> {
                val subcommands = ClanCommands.entries
                    .filter { commandSender.hasPermission(it.helpEntry.permission) }
                    .map { it.command }
                suggestions.addAll(subcommands)
            }
            2 -> {
                val mainCommandName = args[0]

                val mainCommand = this.commands.find {
                    it.command.equals(mainCommandName, ignoreCase = true)
                }

                if(mainCommand != null && mainCommand.subCommands != null){
                    val subSuggestions = mainCommand.subCommands!!
                        .filter { commandSender.hasPermission(it.helpEntry.permission) }
                        .map { it.command }

                    suggestions.addAll(subSuggestions)
                }
            }
        }

        return suggestions
    }
}