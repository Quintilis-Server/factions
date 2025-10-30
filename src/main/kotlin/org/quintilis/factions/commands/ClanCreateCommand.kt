package org.quintilis.factions.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.entities.managers.ClanManager

class ClanCreateCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Somente jogadores podem usar esse comando.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso correto: /clan create <nome> <tag>").color(NamedTextColor.RED))
            return true
        }

        val name = args[0]
        val tag = args[1].uppercase()

        if ((tag.length < 3) || (tag.length > 4)){
            sender.sendMessage(Component.text("Tag nao pode ter menos que 3 caracteres ou mais que 4").color(NamedTextColor.RED))
            return true
        }

        // Checa se já existe nome/tag
        if (ClanManager.existsByName(name)) {
            sender.sendMessage(Component.text("❌ Já existe um clã com esse nome!").color(NamedTextColor.RED))
            return true
        }

        if (ClanManager.existsByTag(tag)) {
            sender.sendMessage(Component.text("❌ Já existe um clã com essa tag!").color(NamedTextColor.RED))
            return true
        }

        val clan = ClanManager.createClan(name, tag)

        sender.sendMessage(
            Component.text("✅ Clã '${clan.name}' [${clan.tag}] criado com sucesso!")
                .color(NamedTextColor.GREEN)
        )

        return true
    }
}