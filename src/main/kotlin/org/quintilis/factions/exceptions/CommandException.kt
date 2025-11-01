package org.quintilis.factions.exceptions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.quintilis.factions.commands.CommandInterface
import org.quintilis.factions.string.color

object CommandException {

    fun notPlayer(commandSender: CommandSender):Boolean {
        commandSender.sendMessage(Component.text("You must be a player.").color(NamedTextColor.RED));
        return true
    }

    fun <T> sendAllUsage(commandSender: CommandSender, commands: Array<T>): Boolean
            where T : Enum<T>, T : CommandInterface {

        // Cria uma lista de Components formatados
        val components = commands.map {
            Component.text(it.usage)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        }

        // Junta os components com um separador “ | ”
        val separator = Component.text(" | ").color(NamedTextColor.WHITE)
        val joined = Component.join(JoinConfiguration.separator(separator), components)

        // Mensagem final: “Uso: ...”
        val message = Component.text("Uso: ", NamedTextColor.GRAY).append(joined)

        // Envia
        commandSender.sendMessage(message)
        return true
    }

    fun <T> sendUsage(commandSender: CommandSender, command: T): Boolean
            where T : Enum<T>, T : CommandInterface {

        val usageComponent = Component.text(command.usage)
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.BOLD, true)

        val message = Component.text("Uso: ", NamedTextColor.GRAY).append(usageComponent)

        commandSender.sendMessage(message)
        return true
    }

    fun notClanLeader(commandSender: CommandSender): Boolean {
        commandSender.sendMessage("Você tem que ser o líder do clã para fazer isso!".color(NamedTextColor.RED))
        return true
    }
    /*
    fun notEnoughArgs(commandSender: CommandSender, args: Array<out String>, min: Int): Boolean {
        commandSender.sendMessage("Argumentos insuficientes. Necessário $min, fornecido ${args.size}".color(ChatColor.RED))
        return true
    }
    */
    fun notFound(commandSender: CommandSender, type:String): Boolean{
        val message = (Component.text("O $type não foi encontrado.", NamedTextColor.RED))

        commandSender.sendMessage(message)

        return true

    }

    fun notInAClan(commandSender: CommandSender): Boolean {
        val message = Component.text("You are not in a clan.", NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)

        commandSender.sendMessage(message)
        return true
    }

    fun alreadyInClan(commandSender: CommandSender): Boolean {
        commandSender.sendMessage("You already in a clan.".color(NamedTextColor.RED))
        return true
    }
}
