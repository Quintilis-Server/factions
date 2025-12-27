package org.quintilis.factions.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

abstract class BaseCommand(
    name: String,
    description: String,
    usage: String,
    aliases: List<String>,
    val commands: List<Commands>
): Command(name, description, usage, aliases) {

    abstract fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean

    abstract fun onTabComplete(
        commandSender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String>

    protected val pageSize = 5;

    protected fun unknownSubCommand(sender: CommandSender, subCommand: String) {
        sender.sendMessage {
            Component.translatable(
                "error.unknown_subcommand",
                Argument.string("command_name", subCommand)
            )
        }
        return;
    }
    protected fun <T: Commands> findSubCommand(
        sender: Player,
        args: List<String>,
        entries: List<T>
    ): T? {
        if (args.isEmpty()) {
            argumentsMissing(sender)
            return null
        }

        val subName = args[0]
        val found = entries.find { it.command.equals(subName, ignoreCase = true) }

        if (found == null) {
            unknownSubCommand(sender, subName)
            return null
        }
        return found
    }

    protected fun noPermission(sender: CommandSender): Boolean {
        sender.sendMessage {
            Component.translatable(
                "error.no_permission",
            )
        }
        return true;
    }

    protected fun noPlayer(sender: CommandSender){
        sender.sendMessage {
            Component.translatable(
                "error.no_player",
            )
        }
    }

    protected fun argumentsMissing(sender: CommandSender): Boolean{
        sender.sendMessage {
            Component.translatable(
                "error.arguments_missing",
                Argument.component("command_name", Component.text(this.name))
            )
        }
        return true
    }


    private fun help(sender: CommandSender, alias: String, helpArguments: List<String>){
        val accessibleCommands = this.commands.filter { sender.hasPermission(it.helpEntry.permission) }

        val totalPages = max(1, ceil(accessibleCommands.size.toDouble() / pageSize).toInt())
        val page = helpArguments.getOrNull(0)?.toIntOrNull() ?: 1

        if(page !in 1 .. totalPages){
            sender.sendMessage {
                Component.translatable(
                    "error.invalid_page",
                    Argument.numeric("total_pages", totalPages)
                )
            }
            return
        }

        sender.sendMessage {
            Component.translatable(
                "help.header",
                Argument.numeric("page", page),
                Argument.numeric("total_pages", totalPages)
            )
        }

        val startIndex = (page - 1) * pageSize
        val endIndex = min(startIndex + pageSize, accessibleCommands.size)
        val pageEntries = accessibleCommands.subList(startIndex, endIndex)

        for(entry in pageEntries){
            val descriptionComponent = Component.translatable(entry.helpEntry.descriptionKey)

            val descriptionArgument = Argument.component("description", descriptionComponent)

            val commandFormat = Component.translatable(
                "command.format",
                Argument.string("alias",alias),
                Argument.string("subcommand", entry.command),
            )

            val lineComponent = Component.translatable(
                "help.command.format",
                Argument.component("command", commandFormat),
                Argument.component("description", descriptionComponent),
            )
            sender.sendMessage(lineComponent)
        }
        sender.sendMessage {
            Component.translatable(
                "help.footer",
                Argument.string("command", alias)
            )
        }
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String?> {
        val input = args.lastOrNull() ?: ""

        val suggestions = this.onTabComplete(sender, alias, args)

        if(args.size == 1){
            suggestions.add("help")
        }
        val competitions  = mutableListOf<String>()

        StringUtil.copyPartialMatches(
            input,
            suggestions.distinct(),
            competitions,
        )
        return competitions
    }

    override fun execute(
        commandSender: CommandSender,
        label: String,
        args: Array<String>
    ): Boolean {

        if(commandSender !is Player) {
            commandSender.sendMessage{
                Component.translatable("error.is_not_player")
            }
            return true;
        }

        if(args.isEmpty() || args[0].equals("help", ignoreCase = true)){
            val helpArgs = if(args.isNotEmpty()) {
                args.copyOfRange(1, args.size).toList()
            } else {
                emptyList()
            }

            this.help(commandSender, label, helpArgs)
            return true;
        }

        val helpEntry = commands.find { it.command == args[0] }
        if(helpEntry == null){
            this.unknownSubCommand(commandSender, args[0])
            return true
        }

        if(commandSender.hasPermission("economy.op")){
            commandSender.sendMessage {
                Component.translatable("info.admin_action")
            }
        }
        return this.commandWrapper(commandSender, label, args)
    }


}