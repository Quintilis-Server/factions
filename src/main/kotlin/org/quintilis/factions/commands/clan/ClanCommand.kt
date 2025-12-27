package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.commands.Commands
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.gui.ClanListMenu
import org.quintilis.factions.handlers.AllyCommandHandler
import org.quintilis.factions.handlers.InviteCommandHandler
import org.quintilis.factions.handlers.MemberCommandHandler
import org.quintilis.factions.managers.ErrorManager
import org.quintilis.factions.results.ClanResult
import org.quintilis.factions.services.Services
import kotlin.math.ceil
import kotlin.math.max

/**
 * Comando principal de clã.
 * Refatorado para usar handlers e services.
 */
class ClanCommand: BaseCommand(
    name = "clan",
    description = "Main clan command",
    usage = "/clan <subcommand>",
    aliases = listOf("c"),
    commands = ClanCommands.entries
) {
    // Handlers
    private val allyHandler = AllyCommandHandler()
    private val memberHandler = MemberCommandHandler()
    private val inviteHandler = InviteCommandHandler()
    
    // Services e Caches (via singleton)
    private val clanService get() = Services.clanService
    private val clanCache get() = Services.clanCache
    private val memberInviteCache get() = Services.memberInviteCache
    private val allyInviteCache get() = Services.allyInviteCache

    // ============================================
    // Métodos de erro
    // ============================================
    
    private fun noClanLeader(sender: Player) {
        sender.sendTranslatable("clan.is_not_leader")
    }
    
    private fun clanNotFound(sender: Player) {
        sender.sendTranslatable("error.no_clan")
    }

    // ============================================
    // Comandos principais
    // ============================================

    private fun handleCreate(sender: Player, args: List<String>) {
        if (args.isEmpty()) {
            argumentsMissing(sender)
            return
        }
        
        val name = args[0]
        val tag = args.getOrNull(1)
        
        when (val result = clanService.createClan(sender, name, tag)) {
            is ClanResult.Success -> {
                sender.sendTranslatable(
                    "clan.create.response",
                    Argument.string("clan_name", result.args["clan_name"]?.toString() ?: name)
                )
            }
            is ClanResult.Error -> {
                if (result.args.isNotEmpty()) {
                    sender.sendTranslatable(
                        result.messageKey,
                        *result.args.map { Argument.string(it.key, it.value.toString()) }.toTypedArray()
                    )
                } else {
                    sender.sendTranslatable(result.messageKey)
                }
            }
        }
    }

    private fun handleDelete(sender: Player) {
        val clan = clanCache.getClanByLeaderId(sender.uniqueId)
        if (clan == null) {
            noClanLeader(sender)
            return
        }
        
        // Buscar membros antes de deletar (para notificar)
        val members = clanCache.getMembers(clan.id!!)
        
        when (val result = clanService.deleteClan(sender)) {
            is ClanResult.Success -> {
                // Notifica membros
                members.forEach { member ->
                    Bukkit.getPlayer(member.playerId)?.sendTranslatable(
                        "clan.delete.member_response",
                        Argument.string("leader_name", sender.name)
                    )
                }
                sender.sendTranslatable("clan.delete.response")
            }
            is ClanResult.Error -> {
                sender.sendTranslatable(result.messageKey)
            }
        }
    }

    private fun handleList(sender: Player, args: List<String>) {
        val page = args.getOrNull(0)?.toIntOrNull()
        
        if (page == null) {
            // Abre GUI
            ClanListMenu(sender).open()
            return
        }
        
        if (page <= 0) return
        
        val totalClans = clanService.getTotalClans()
        val totalPages = max(1, ceil(totalClans.toDouble() / pageSize).toInt())
        
        if (page !in 1..totalPages) {
            sender.sendTranslatable(
                "error.invalid_page",
                Argument.numeric("total_page", totalPages)
            )
            return
        }
        
        val clans = clanService.listClans(page, pageSize)
        
        sender.sendTranslatable(
            "clan.list.header",
            Argument.numeric("page", page),
            Argument.numeric("total_page", totalPages)
        )
        
        clans.forEach { clan ->
            sender.sendTranslatable(
                "clan.list.response",
                Argument.string("clan_name", clan.name),
                Argument.string("tag", clan.tag ?: ""),
                Argument.string("leader_name", clan.getLeader()?.name ?: "Unknown")
            )
        }
        
        sender.sendTranslatable(
            "clan.list.footer",
            Argument.string("command", ClanCommands.LIST.usage)
        )
    }

    private fun handleQuit(sender: Player) {
        val clan = clanCache.getClanByMember(sender.uniqueId)
        
        when (val result = clanService.quitClan(sender)) {
            is ClanResult.Success -> {
                // Notifica o líder
                val leaderUuid = result.args["leader_uuid"]
                if (leaderUuid != null) {
                    Bukkit.getPlayer(leaderUuid as java.util.UUID)?.sendTranslatable(
                        "clan.quit.leader_response",
                        Argument.component("player_name", Component.text(sender.name))
                    )
                }
                sender.sendTranslatable("clan.quit.response")
            }
            is ClanResult.Error -> {
                sender.sendTranslatable(result.messageKey)
            }
        }
    }

    // ============================================
    // Handlers de subcomandos
    // ============================================

    private fun handleAllyCommand(sender: Player, args: List<String>) {
        val clan = sender.getClanAsLeader()
        if (clan == null) {
            noClanLeader(sender)
            return
        }
        
        val subCommand = findSubCommand(sender, args, AllySubCommands.entries) ?: return
        
        when (subCommand) {
            AllySubCommands.ADD -> allyHandler.add(sender, clan, args.drop(1))
            AllySubCommands.REMOVE -> allyHandler.remove(sender, clan, args.drop(1))
            AllySubCommands.LIST -> allyHandler.list(sender, clan)
            AllySubCommands.ACCEPT -> allyHandler.accept(sender, clan, args.drop(1))
            AllySubCommands.REJECT -> allyHandler.reject(sender, clan, args.drop(1))
        }
    }

    private fun handleMemberCommand(sender: Player, args: List<String>) {
        val clan = sender.getClanAsLeader()
        if (clan == null) {
            noClanLeader(sender)
            return
        }
        
        val subCommand = findSubCommand(sender, args, MemberSubCommands.entries) ?: return
        
        when (subCommand) {
            MemberSubCommands.INVITE -> memberHandler.invite(sender, clan, args.drop(1))
            MemberSubCommands.REMOVE -> memberHandler.kick(sender, clan, args.drop(1))
            MemberSubCommands.PROMOTE -> {} // TODO
            MemberSubCommands.LIST -> memberHandler.list(sender, clan)
        }
    }

    private fun handleInviteCommand(sender: Player, args: List<String>) {
        val subCommand = findSubCommand(sender, args, InviteSubCommands.entries) ?: return
        
        when (subCommand) {
            InviteSubCommands.ACCEPT -> inviteHandler.accept(sender, args.drop(1))
            InviteSubCommands.REJECT -> inviteHandler.reject(sender, args.drop(1))
            InviteSubCommands.CANCEL -> inviteHandler.cancel(sender, args.drop(1))
            InviteSubCommands.LIST -> inviteHandler.list(sender)
        }
    }

    // ============================================
    // Command wrapper
    // ============================================

    override fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean {
        ErrorManager.runSafe(commandSender) {
            val sender = commandSender as Player
            
            val rootCommand = ClanCommands.entries.find {
                it.command.equals(args[0], ignoreCase = true)
            }
            
            if (rootCommand == null) {
                unknownSubCommand(commandSender, args[0])
                return true
            }

            val subArgs = args.drop(1)

            when (rootCommand) {
                ClanCommands.CREATE -> handleCreate(sender, subArgs)
                ClanCommands.DELETE -> handleDelete(sender)
                ClanCommands.LIST -> handleList(sender, subArgs)
                ClanCommands.ALLY -> handleAllyCommand(sender, subArgs)
                ClanCommands.MEMBER -> handleMemberCommand(sender, subArgs)
                ClanCommands.INVITE -> handleInviteCommand(sender, subArgs)
                ClanCommands.QUIT -> handleQuit(sender)
            }
        }
        return true
    }

    // ============================================
    // Tab Complete
    // ============================================

    override fun onTabComplete(
        commandSender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val sender = commandSender as Player
        val suggestions = mutableListOf<String>()

        when (args.size) {
            1 -> {
                suggestions.addAll(
                    ClanCommands.entries
                        .filter { sender.hasPermission(it.helpEntry.permission) }
                        .map { it.command }
                )
            }
            2 -> {
                val mainCommand = commands.find {
                    it.command.equals(args[0], ignoreCase = true)
                }
                
                if (mainCommand?.subCommands != null) {
                    suggestions.addAll(
                        mainCommand.subCommands!!
                            .filter { sender.hasPermission(it.helpEntry.permission) }
                            .map { it.command }
                    )
                }
            }
            3 -> {
                val clan = sender.getClanAsLeader()
                val mainCommand = args[0].lowercase()
                val subCommand = args[1].lowercase()
                
                when (mainCommand) {
                    // /clan invite <subcommand>
                    ClanCommands.INVITE.command -> {
                        when (subCommand) {
                            InviteSubCommands.ACCEPT.command, InviteSubCommands.REJECT.command -> {
                                suggestions.addAll(memberInviteCache.getClanNames(sender.uniqueId))
                            }
                            InviteSubCommands.CANCEL.command -> {
                                suggestions.addAll(memberInviteCache.getPlayerNames(sender.uniqueId))
                            }
                        }
                    }
                    // /clan member <subcommand>
                    ClanCommands.MEMBER.command -> {
                        if (clan != null) {
                            suggestions.addAll(memberHandler.getSuggestions(subCommand, sender, clan))
                        }
                    }
                    // /clan ally <subcommand>
                    ClanCommands.ALLY.command -> {
                        if (clan != null) {
                            suggestions.addAll(allyHandler.getSuggestions(subCommand, clan))
                        }
                    }
                }
            }
        }

        return suggestions
    }
}