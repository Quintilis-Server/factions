package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.extensions.getClanAsLeader
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.gui.ClanListMenu
import org.quintilis.factions.handlers.AdminCommandHandler
import org.quintilis.factions.handlers.AllyCommandHandler
import org.quintilis.factions.handlers.ClaimCommandHandler
import org.quintilis.factions.handlers.BetrayCommandHandler
import org.quintilis.factions.handlers.SurrenderCommandHandler
import org.quintilis.factions.handlers.InviteCommandHandler
import org.quintilis.factions.handlers.MemberCommandHandler
import org.quintilis.factions.managers.ErrorManager
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.CoreService
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.services.FactionsServices.coreCache
import kotlin.math.ceil
import kotlin.math.max

/**
 * Comando principal de clã.
 * Refatorado para usar handlers e services.
 */
class ClanCommand(
    private val coreService: CoreService,
    private val plugin: JavaPlugin
): BaseCommand(
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
    private val adminHandler = AdminCommandHandler()
    private val claimHandler = ClaimCommandHandler(coreService)
    private val betrayHandler = BetrayCommandHandler()
    private val surrenderHandler = SurrenderCommandHandler()

    // Services e Caches (via singleton)
    private val clanService get() = FactionsServices.clanService
    private val clanCache get() = FactionsServices.clanCache
    private val memberInviteCache get() = FactionsServices.memberInviteCache
    private val allyInviteCache get() = FactionsServices.allyInviteCache

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
            is Result.Success -> {
                sender.sendTranslatable(
                    "clan.create.response",
                    Argument.string("clan_name", result.args["clan_name"]?.toString() ?: name)
                )
            }
            is Result.Error -> {
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
        val clan = clanCache.findByLeaderId(sender.uniqueId) ?: return noClanLeader(sender)

        // Buscar membros antes de deletar (para notificar)
        val members = clanCache.getMembers(clan.id!!)

        // 1. BUSCAR TUDO ANTES DE DELETAR
        val cores = coreCache.findByClanId(clan.id) // Busca enquanto ainda são "ativos"

        // 2. EXECUTAR A DELEÇÃO FÍSICA (Blocos)
        cores.forEach { c ->
            c.deleteCore() // Isso remove os blocos e limpa chunks
        }

        // 3. EXECUTAR A DELEÇÃO LÓGICA (Banco/Service)
        when (val result = clanService.deleteClan(sender)) {
            is Result.Success -> {
                members.forEach { member ->
                    Bukkit.getPlayer(member.playerId)?.sendTranslatable(
                        "clan.delete.member_response",
                        Argument.string("leader_name", sender.name)
                    )
                }
                sender.sendTranslatable("clan.delete.response")
            }
            is Result.Error -> {
                sender.sendTranslatable(result.messageKey)
            }
        }
    }

    private fun handleList(sender: Player, args: List<String>) {
        val page = args.getOrNull(0)?.toIntOrNull()
        
        if (page == null) {
            // Abre GUI
            ClanListMenu(sender, plugin).open()
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
            is Result.Success -> {
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
            is Result.Error -> {
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
        val clan = sender.getClanAsLeader() ?: return this.noClanLeader(sender)
        
        val subCommand = findSubCommand(sender, args, MemberSubCommands.entries) ?: return
        
        when (subCommand) {
            MemberSubCommands.INVITE -> memberHandler.invite(sender, clan, args.drop(1))
            MemberSubCommands.REMOVE -> memberHandler.kick(sender, clan, args.drop(1))
            MemberSubCommands.PROMOTE -> memberHandler.promote(sender, clan, args.drop(1))
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

    private fun handleAdminCommand(sender: Player, args: List<String>) {
        // Verifica permissão de admin
        if (!sender.hasPermission("factions.admin")) {
            sender.sendTranslatable("error.no_permission")
            return
        }
        
        val subCommand = findSubCommand(sender, args, AdminSubCommands.entries) ?: return
        
        when (subCommand) {
            AdminSubCommands.DELETE -> adminHandler.delete(sender, args.drop(1))
            AdminSubCommands.SETNAME -> adminHandler.setName(sender, args.drop(1))
            AdminSubCommands.SETTAG -> adminHandler.setTag(sender, args.drop(1))
            AdminSubCommands.SETLEADER -> adminHandler.setLeader(sender, args.drop(1))
            AdminSubCommands.SPAWNNPC -> adminHandler.spawnNpc(sender, args.drop(1))
        }
    }

    private fun handleClaimCommand(sender: Player, args: List<String>) {
        val clan = sender.getClanAsLeader() ?: return this.noClanLeader(sender);
        val subCommand = findSubCommand(sender, args, ClaimSubCommands.entries) ?: return
        when (subCommand) {
            ClaimSubCommands.BUY -> claimHandler.buy(sender, clan)
            ClaimSubCommands.NEXUS -> claimHandler.nexus(sender, clan)
        }
    }

    private fun handleBetrayCommand(sender: Player, args: List<String>) {
        val clan = sender.getClanAsLeader() ?: return this.noClanLeader(sender)
        betrayHandler.betray(sender, clan, args)
    }

    private fun handleSurrenderCommand(sender: Player, args: List<String>) {
        val clan = sender.getClanAsLeader() ?: return this.noClanLeader(sender)
        surrenderHandler.surrender(sender, clan, args)
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
                ClanCommands.ADMIN -> handleAdminCommand(sender, subArgs)
                ClanCommands.CLAIM -> handleClaimCommand(sender, subArgs)
                ClanCommands.BETRAY -> handleBetrayCommand(sender, subArgs)
                ClanCommands.SURRENDER -> handleSurrenderCommand(sender, subArgs)
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
                val clan = sender.getClanAsLeader()
                val mainCommand = commands.find {
                    it.command.equals(args[0], ignoreCase = true)
                }
                
                if (mainCommand?.subCommands != null) {
                    suggestions.addAll(
                        mainCommand.subCommands!!
                            .filter { sender.hasPermission(it.helpEntry.permission) }
                            .map { it.command }
                    )
                } else if (mainCommand?.command.equals("betray", ignoreCase = true)) {
                    if (clan != null) {
                        suggestions.addAll(betrayHandler.getSuggestions(clan))
                    }
                } else if (mainCommand?.command.equals("surrender", ignoreCase = true)) {
                    if (clan != null) {
                        suggestions.addAll(surrenderHandler.getSuggestions(clan))
                    }
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
                    // /clan admin <subcommand>
                    ClanCommands.ADMIN.command -> {
                        if (sender.hasPermission("factions.admin")) {
                            suggestions.addAll(adminHandler.getSuggestions(subCommand))
                        }
                    }
                }
            }
        }

        return suggestions
    }
}