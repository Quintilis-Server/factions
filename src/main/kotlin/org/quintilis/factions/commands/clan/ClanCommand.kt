package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.entities.managers.ClanManager
import org.quintilis.factions.entities.managers.InviteManager
import org.quintilis.factions.entities.managers.PlayerManager
import org.quintilis.factions.entities.models.Clan
import org.quintilis.factions.entities.models.PlayerEntity
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
            ClanCommands.SET.command -> set(sender, args.drop(1).toTypedArray())
            ClanCommands.QUIT.command -> quit(sender)
            ClanCommands.MEMBER.command -> {
                val newArray = args.sliceArray(1 until args.size)
                if(newArray.isEmpty()) {
                    return CommandException.sendAllUsage(sender, ClanMemberSubCommands.entries.toTypedArray())
                }
                when(args[1]){
                    ClanMemberSubCommands.INVITE.command -> sendInvite(sender, args.sliceArray(2 until args.size))
                    ClanMemberSubCommands.KICK.command -> kick(sender, args.drop(2).toTypedArray())
                    ClanMemberSubCommands.LIST.command -> listMembers(sender, args.drop(2).toTypedArray())
                    else -> return CommandException.sendAllUsage(sender, ClanMemberSubCommands.entries.toTypedArray())
                }
            }
            else -> return CommandException.sendAllUsage(sender, ClanCommands.entries.toTypedArray())
        }
        return true
    }

    private fun listMembers(sender: CommandSender, args: Array<String>){

        var clan: Clan?;

        if (args.isEmpty()) {

            clan = ClanManager.getClanByPlayer(sender as Player)
        } else {

            clan = ClanManager.getClanByName(args[0])
        }

        if(clan == null){
            CommandException.notInAClan(sender)
            return
        }

        sender.sendMessage(Component.text("Membros do clã ${clan.name}", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))

        val members = ClanManager.getMembers(clan)

        if (members.isEmpty()) {
            sender.sendMessage(
                Component.text("Nenhum membro encontrado.", NamedTextColor.RED)
            )
            return
        }

        // Lista os membros de forma estilizada
        members.forEach { member ->
            val onlinePlayer = Bukkit.getPlayer(member.id!!) // null se offline

            val nameComponent = if (onlinePlayer != null) {
                Component.text(member.name, NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
            } else {
                Component.text(member.name, NamedTextColor.GRAY)
            }

            val statusComponent = if (onlinePlayer != null) {
                Component.text(" [Online]", NamedTextColor.AQUA)
            } else {
                Component.text(" [Offline]", NamedTextColor.DARK_GRAY)
            }

            sender.sendMessage(nameComponent.append(statusComponent))
        }
    }

    private fun sendInvite (sender: CommandSender, args: Array<out String>) {

        val clan = ClanManager.getClanByOwner(sender as Player)

        if(clan == null) {
            CommandException.notClanLeader(sender)
            return
        }

        if(args.isEmpty()) {
            CommandException.sendUsage(sender, ClanMemberSubCommands.INVITE)
            return
        }

        val playerSender: PlayerEntity = PlayerManager.getPlayerUUID(sender.uniqueId)!!
        val playerReceiver = PlayerManager.getPlayerByName(args[0])

        if(playerReceiver == null){
            CommandException.notFound(sender, "jogador")
            return
        }

        try{

            InviteManager.addPlayerInvite(playerSender, playerReceiver, clan)

        }catch (e: Exception){
            sender.sendMessage(Component.text("Erro ao mandar convite, talvez já tenha um convite para essa pessoa.", NamedTextColor.RED).decorate(TextDecoration.BOLD))
            println(e.message)
        }
        sender.sendMessage(
            Component.text("Convite enviado ", NamedTextColor.WHITE)
                .append(Component.text(playerReceiver.name, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .append(Component.text(" para o clã ", NamedTextColor.WHITE))
                .append(Component.text(clan.name, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
        )
    }

    private fun kick(sender: CommandSender, args: Array<String>){

        val clan = ClanManager.getClanByOwner(sender as Player)

        if(clan == null) {
            CommandException.notClanLeader(sender)
            return
        }

        if(args.isEmpty()) {
            CommandException.sendUsage(sender, ClanMemberSubCommands.KICK)
            return
        }

        val playerReceiver = PlayerManager.getPlayerByName(args[0])

        if(playerReceiver == null){
            CommandException.notFound(sender, "jogador")
            return
        }

        if(playerReceiver.id == clan.leaderUuid) {
            sender.sendMessage(Component.text("Você não pode remover o dono do clã.", NamedTextColor.RED).decorate(
                TextDecoration.BOLD))
            return
        }

        ClanManager.removeMember(playerReceiver, clan)
        sender.sendMessage(
            Component.text("Usuário ", NamedTextColor.RED)
                .append(Component.text(playerReceiver.name, NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text(" foi removido do clã ", NamedTextColor.RED))
                .append(Component.text(clan.name, NamedTextColor.RED).decorate(TextDecoration.BOLD))
        )
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
        val playerEntity = PlayerManager.getPlayerUUID(sender.uniqueId);

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

    private fun list(sender: CommandSender) {

        val out = Component.text()

        ClanManager.listClans().forEach { clan ->


            out.append(
                Component.text("Nome: ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .append(Component.text(clan.name, NamedTextColor.WHITE))
                    .append(Component.text(", Tag: ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(clan.tag ?: "", NamedTextColor.WHITE))
                    .append(Component.text(", Dono: ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(PlayerManager.getPlayerUUID(clan.leaderUuid)!!.name, NamedTextColor.WHITE))
                    .append(Component.newline())
            )
        }

        sender.sendMessage(out)
    }

    private fun quit(sender: CommandSender) {

        if(sender !is Player){
            CommandException.notPlayer(sender)
            return
        }

        val clan = ClanManager.getClanByPlayer(sender)

        if(clan == null){
            CommandException.notInAClan(sender)
            return
        }

        val clan2 = ClanManager.getClanByOwner(sender)

        if(clan2 != null){
            val deleteCommand = Component.text(ClanCommands.DELETE.usage, NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
            sender.sendMessage(
                Component.text("Você é o dono do clã, use o ")
                    .append(deleteCommand)
                    .append(Component.text(" para deletar o clã"))
            )
            return
        }

        val playerEntity = PlayerManager.getPlayerUUID(sender.uniqueId)

        ClanManager.removeMember(playerEntity, clan)

        sender.sendMessage(
            Component.text("Você saiu do clã ", NamedTextColor.GREEN)
                .append(Component.text(clan.name, NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        )
    }

    private fun set(sender: CommandSender, args: Array<String>) {

        val clan = ClanManager.getClanByOwner(sender as Player)

        if(clan == null) {
            CommandException.notClanLeader(sender)
            return
        }

        val setValue = args.getOrNull(1)
        if(setValue == null) {
            CommandException.sendAllUsage(sender, ClanSetSubCommands.entries.toTypedArray())
            return
        }

        when(args.getOrNull(0)){
            ClanSetSubCommands.NAME.command -> ClanManager.setName(setValue, clan)
            ClanSetSubCommands.TAG.command -> ClanManager.setTag(setValue, clan)
            else -> {
                CommandException.sendAllUsage(sender, ClanSetSubCommands.entries.toTypedArray())
                return
            }
        }

        sender.sendMessage("Valor ${args.getOrNull(0)} alterado para: $setValue")
    }
}