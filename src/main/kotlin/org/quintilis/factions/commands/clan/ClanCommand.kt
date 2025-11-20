package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
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

//    private fun delete()

    override fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean {
        when(args[0].lowercase()){
            ClanCommands.CREATE.command -> this.create(commandSender, args.drop(1))
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
//                when (args[0].lowercase()) {
//
//                }
            }
        }

        return suggestions
    }
}