package org.quintilis.factions.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.entities.managers.ClanManager
import org.quintilis.factions.entities.managers.PlayerManager
import org.quintilis.factions.exceptions.CommandException

class ClanCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return CommandException.notPlayer(sender)
        }

        if (args.isEmpty()) {
            return CommandException.sendAllUsage(sender,ClanCommands.entries.toTypedArray())
        }

        when(args[0]){
            ClanCommands.CREATE.command -> create(sender, args.sliceArray(1 until args.size))
            ClanCommands.DELETE.command -> delete(sender)
            ClanCommands.LIST.command -> list(sender)
            ClanCommands.SET.command -> set(sender, args.sliceArray(1 until args.size))
            ClanCommands.QUIT.command -> quit(sender)
            ClanCommands.MEMBER.command -> {
                val newArray = p3.sliceArray(1 until p3.size)
                if(newArray.isEmpty()) {
                    return CommandException.sendAllUsage(args, ClanMemberSubCommands.entries.toTypedArray())
                }
                when(args[1]){
                    ClanMemberSubCommands.INVITE.command -> sendInvite(p0, p3.sliceArray(2 until p3.size))
                    ClanMemberSubCommands.KICK.command -> kick(p0, p3.sliceArray(2 until p3.size))
                    ClanMemberSubCommands.LIST.command -> listMembers(p0, p3.sliceArray(2 until p3.size))
                    else -> return CommandException.sendAllUsage(p0, ClanMemberSubCommands.entries.toTypedArray())
                }
            }
            else -> return CommandException.sendAllUsage(args, ClanCommands.entries.toTypedArray())
        }
        return true
    }

    private fun create(sender: CommandSender, args: Array<out String>) {

        if (args.isEmpty()) {
            CommandException.sendUsage(sender, ClanCommands.CREATE)
            return
        }
        val name = args.getOrNull(0)
        val tag = args[1].uppercase()

        if(name==null || name.isEmpty()) {
            sender.sendMessage("Nome do clã esta vazio, uso /clan create <nome do clã> <tag>")
            return
        }

        if ((tag.length < 3) || (tag.length > 4)){
            sender.sendMessage(Component.text("Tag nao pode ter menos que 3 caracteres ou mais que 4").color(NamedTextColor.RED))
            return
        }

        val player = sender as Player
        val playerEntity = PlayerManager.getPlayerUUID(sender);


        if (playerEntity == null) {
            sender.sendMessage("Erro: jogador não encontrado no banco de dados.")
            return
        }

        if(playerEntity.clanId != null){
            CommandException.alreadyInClan(sender);
            return
        }

        if (ClanManager.existsByName(name)) {
            sender.sendMessage(Component.text("❌ Já existe um clã com esse nome!").color(NamedTextColor.RED))
            return
        }

        if (ClanManager.existsByTag(tag)) {
            sender.sendMessage(Component.text("❌ Já existe um clã com essa tag!").color(NamedTextColor.RED))
            return
        }

        val clan = ClanManager.createClan(name, tag, sender.uniqueId)

        sender.sendMessage(
            Component.text("✅ Clã '${clan.name}' [${clan.tag}] criado com sucesso!")
                .color(NamedTextColor.GREEN)
        )
    }

    private fun delete(sender: CommandSender) {

        val clan = ClanManager.getClanByOwner(sender as Player)

        if(clan == null) {
            sender.sendMessage("Você não é dono de nenhum clã!")
            return
        }

        ClanManager.deleteClan(clan)

        sender.sendMessage(
            Component.text("✅ Clã deletado com sucesso!")
                .color(NamedTextColor.GREEN)
        )
    }
}