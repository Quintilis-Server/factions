package org.quintilis.factions.commands.clan

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quintilis.factions.cache.AllyInviteCache
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.cache.MemberInviteCache
import org.quintilis.factions.cache.PlayerCache
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.commands.Commands
import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.ClanRelationDao
import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.gui.ClanListMenu
import org.quintilis.factions.managers.DatabaseManager
import org.quintilis.factions.managers.ErrorManager
import org.quintilis.factions.services.AllyInviteService
import org.quintilis.factions.services.MemberInviteService
import kotlin.math.ceil
import kotlin.math.max

class ClanCommand: BaseCommand(
    name = "clan",
    description = "Main clan command",
    usage = "/clan <subcommand>",
    aliases = listOf("c"),
    commands = ClanCommands.entries
) {
    private val clanDao = DatabaseManager.getDAO(ClanDao::class)
    private val clanCache = ClanCache(clanDao)
    private val clanRelationDao = DatabaseManager.getDAO(ClanRelationDao::class)
    private val playerDao = DatabaseManager.getDAO(PlayerDao::class)
    private val playerCache = PlayerCache(playerDao)
    private val memberInviteDao = DatabaseManager.getDAO(MemberInviteDao::class)
    private val memberInviteCache = MemberInviteCache(memberInviteDao)
    private val allyInviteDao = DatabaseManager.getDAO(AllyInviteDao::class)
    private val allyInviteCache = AllyInviteCache(allyInviteDao)

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
    private fun clanNotFound(sender: CommandSender){
        sender.sendMessage {
            Component.translatable(
                "error.no_clan"
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
        if(clanCache.existsByName(name)){
            sender.sendMessage {
                Component.translatable(
                    "clan.create.error.already_exists",
                    Argument.string("clan_name", name)
                )
            }
           return
        }

        val clan = ClanEntity(name = name, tag = tag, leaderUuid = sender.uniqueId).save<ClanEntity>()
        ClanMemberEntity(clanId = clan.id!!,playerId = sender.uniqueId).save<ClanMemberEntity>()
        
        // Invalida caches após criar clã
        clanCache.invalidateGlobalCaches()
        clanCache.invalidateMember(sender.uniqueId)
        
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

        val clan = clanCache.getClanByLeaderId(sender.uniqueId) ?: return this.noClanLeader(sender)
        val clanMembers = clanCache.getMembers(clan.id!!)
        try{
            clanDao.deleteByIdAndLeader(clan.id)
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

        // Invalida caches após deletar clã
        clanCache.invalidateClan(clan)
        clanMembers.forEach { clanCache.invalidateMember(it.playerId) }
        
        sender.sendMessage {
            Component.translatable(
                "clan.delete.response",
            )
        }
    }

    private fun listGui(sender: CommandSender) {
        val menu = ClanListMenu(sender as Player)
        menu.open()
    }

    private fun list(sender: CommandSender, args: List<String>) {
        sender as Player

        val page = args.getOrNull(0)?.toIntOrNull() ?: return this.listGui(sender)

        if(page <= 0){
            return
        }

        val totalClans = clanCache.getTotalClans()

        val totalPages = max(1, ceil(totalClans.toDouble() / pageSize).toInt())

        if (page !in 1..totalPages) {
            sender.sendMessage {
                Component.translatable(
                    "error.invalid_page", // Crie essa chave no seu properties
                    Argument.numeric("total_page", totalPages)
                )
            }
            return
        }
        val pageOffset = (page - 1) * pageSize

        val clans = clanCache.getClans(page, pageSize)

        sender.sendMessage {
            Component.translatable(
                "clan.list.header",
                Argument.numeric("page", page),
                Argument.numeric("total_page", totalPages)
            )
        }

        clans.forEach {
            sender.sendMessage {
                Component.translatable(
                    "clan.list.response",
                    Argument.string("clan_name", it.name),
                    Argument.string("tag", it.tag?: ""),
                    Argument.string("leader_name", it.getLeader()!!.name)
                )
            }
        }

        sender.sendMessage {
            Component.translatable(
                "clan.list.footer",
                Argument.string("command", ClanCommands.LIST.usage)
            )
        }
    }

    private fun quit(commandSender: CommandSender){
        val player = commandSender as Player
        val uuid = player.uniqueId

        val clan = clanCache.getClanByMember(uuid)

        if (clan == null) {
            player.sendMessage(Component.translatable("error.not_in_clan"))
            return
        }

        if (clan.leaderUuid == uuid) {
            player.sendMessage(Component.translatable("clan.quit.error.leader"))
            return
        }

        val leaderPlayer = Bukkit.getPlayer(clan.leaderUuid)
        leaderPlayer?.sendMessage(
            Component.translatable(
                "clan.quit.leader_response",
                Argument.component("player_name", Component.text(player.name))
            )
        )
        clanDao.deleteMemberById(uuid)
        
        // Invalida caches após membro sair
        clanCache.invalidateMember(uuid)
        clanCache.invalidateMembersOfClan(clan.id!!)

        player.sendMessage(Component.translatable("clan.quit.response"))
    }

    override fun commandWrapper(
        commandSender: CommandSender,
        label: String,
        args: Array<out String>
    ): Boolean {
        ErrorManager.runSafe(commandSender) {
            val rootCommand = ClanCommands.entries.find{
                it.command.equals(args[0], ignoreCase = true)
            }
            if(rootCommand == null){
                this.unknownSubCommand(commandSender, args[0])
                return true
            }

            val subArgs = args.drop(1)


            when(rootCommand){
                ClanCommands.CREATE -> this.create(sender = commandSender, subArgs)
                ClanCommands.DELETE -> this.delete(sender = commandSender)
                ClanCommands.LIST -> this.list(sender = commandSender, subArgs)
                ClanCommands.ALLY -> this.handleAllyCommand(commandSender, subArgs)
                ClanCommands.MEMBER -> this.handleMemberCommand(commandSender, subArgs)
                ClanCommands.INVITE -> this.handleInviteCommand(commandSender, subArgs)
                ClanCommands.QUIT -> this.quit(commandSender)
            }
        }
        return true
    }

    /**
     * Handler ally commands
     */
    private fun handleAllyCommand(sender: CommandSender, args: List<String>) {
        sender as Player
        val clan = clanCache.getClanByLeaderId(sender.uniqueId) ?: return this.noClanLeader(sender)

        fun add(args: List<String>){
            if(args.isEmpty()) return
            val targetClan = clanCache.getClanByName(args[0]) ?: return this.clanNotFound(sender)

            if(clanRelationDao.isRelation(clan.id!!, targetClan.id!!, Relation.ALLY)){
                sender.sendMessage {
                    Component.translatable("clan.ally.error.is_ally")
                }
                return
            }
            if(clanRelationDao.isRelation(clan.id, targetClan.id, Relation.ENEMY)){
                sender.sendMessage {
                    Component.translatable("clan.ally.error.is_enemy")
                }
                return
            }

            AllyInviteService.createInvite(clanDao, clan, targetClan)

            sender.sendMessage {
                Component.translatable(
                    "clan.ally.invite.response",
                    Argument.string("clan_name", targetClan.name)
                )
            }

        }

        fun remove(){

        }

        fun list(){

        }

        fun accept(){

        }

        fun reject(){

        }

        val subCommand = findSubCommand(sender, args, AllySubCommands.entries) ?: return

        when(subCommand){
            AllySubCommands.ADD -> add(args.drop(1))
            AllySubCommands.REMOVE -> remove()
            AllySubCommands.LIST -> list()
            AllySubCommands.ACCEPT -> accept()
            AllySubCommands.REJECT -> reject()
        }
    }

    private fun handleMemberCommand(sender: CommandSender, args: List<String>){
        sender as Player
        val subCommand = findSubCommand(sender, args, MemberSubCommands.entries) ?: return

        fun invite(args: List<String>){
            if(args.isEmpty()) return
            val playerEntity = playerDao.findByName(args[0])
            if(playerEntity == null){
                this.noPlayer(sender)
                return
            }
            val player = playerEntity.getPlayer()
            val isMember: Boolean = clanCache.isMember(player.uniqueId)
            if(isMember){
                sender.sendMessage {
                    Component.translatable(
                        "error.already_in_a_clan",
                        Argument.string("player_name",player.name!!)
                    )
                }
                return
            }
            val clan = clanCache.getClanByLeaderId(sender.uniqueId) ?: return this.noClanLeader(sender)
            try{
                val invite = MemberInviteService.createInvite(clan, playerEntity)
                memberInviteCache.put(playerEntity.id, listOf(invite.getClan(clanDao)!!.name))
            }catch (e: Exception){
                sender.sendMessage {
                    Component.translatable(
                        "error.already_invited",
                        Argument.string("name", player.name!!)
                    )
                }
                return
            }

            sender.sendMessage {
                Component.translatable(
                    "clan.invite.response",
                    Argument.string("player_name", player.name!!)
                )
            }

        }


        when(subCommand){
            MemberSubCommands.INVITE -> invite(args.drop(1))
            else -> {}
        }
    }

    private fun handleInviteCommand(sender: CommandSender, args: List<String>) {
        sender as Player
        fun accept(args: List<String>){
            val clan = clanCache.getClanByName(args[0]) ?: return this.clanNotFound(sender)

            val isMember: Boolean = clanCache.isMember(sender.uniqueId)
            if(isMember){
                sender.sendMessage {
                    Component.translatable(
                        "error.already_in_a_clan",
                        Argument.string("player_name",sender.name)
                    )
                }
                return
            }

            try{
                MemberInviteService.acceptInvite(memberInviteDao, clan, sender)
            }catch (e: Error){
                sender.sendMessage {
                    Component.translatable("error.no_member_invite")
                }
                return
            }

            clan.getLeader()?.sendMessage {
                Component.translatable(
                    "clan.invite.accept.clan_response",
                    Argument.string("player_name", sender.name)
                )
            }
            sender.sendMessage {
                Component.translatable(
                    "clan.invite.accept.response",
                    Argument.string("clan_name", clan.name)
                )
            }
        }

        fun reject(args: List<String>){
            val clan = clanCache.getClanByName(args[0]) ?: return this.clanNotFound(sender)

            try{
                MemberInviteService.rejectInvite(memberInviteDao, clan, sender)
            }catch (e: Error){
                sender.sendMessage {
                    Component.translatable("error.no_member_invite")
                }
                return
            }

            clan.getLeader()?.sendMessage {
                Component.translatable(
                    "clan.invite.reject.clan_response",
                    Argument.string("player_name", sender.name)
                )
            }
            sender.sendMessage {
                Component.translatable(
                    "clan.invite.reject.response",
                    Argument.string("clan_name", clan.name)
                )
            }
        }

        fun cancel(args: List<String>){
            val playerName = args.getOrNull(0) ?: return this.noPlayer(sender)
            val player = playerCache.getPlayer(playerName) ?: return this.noPlayer(sender)

            memberInviteDao.cancelInvite(player.id)
            memberInviteCache.invalidate(player.id)

            sender.sendMessage {
                Component.translatable(
                    "clan.invite.cancel.response",
                    Argument.string("player_name", player.name)
                )
            }
        }

        val inviteCommand = findSubCommand(sender, args, InviteSubCommands.entries) ?:
            return this.unknownSubCommand(sender, args[0])

        when(inviteCommand){
            InviteSubCommands.ACCEPT -> accept(args.drop(1))
            InviteSubCommands.REJECT -> reject(args.drop(1))
            InviteSubCommands.CANCEL -> cancel(args.drop(1))
        }
    }

    private fun <T: Commands> findSubCommand(
        sender: CommandSender,
        args: List<String>,
        entries: List<T>
    ): T?{
        if(args.isEmpty()){
            this.argumentsMissing(sender)
            return null
        }

        val subName = args[0]
        val found = entries.find { it.command.equals(subName, ignoreCase = true) }

        if(found == null){
            this.unknownSubCommand(sender, subName)
            return null
        }
        return found
    }

    override fun onTabComplete(
        commandSender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        commandSender as Player
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
            3 ->{
                val subcommand = args[1]
                val clan = clanCache.getClanByLeaderId(commandSender.uniqueId)
                when(subcommand){
                    InviteSubCommands.ACCEPT.command, InviteSubCommands.REJECT.command-> {
                        // Busca apenas as Strings (nomes dos clãs) numa única query rápida
                        val clanNames = memberInviteCache.getClanNames(commandSender.uniqueId)
                        suggestions.addAll(clanNames)
                    }
                    InviteSubCommands.CANCEL.command -> {
                        suggestions.addAll(memberInviteCache.getPlayerNames(commandSender.uniqueId))
                    }


                    MemberSubCommands.INVITE.command -> {

                        if(clan != null){
                            val members = clanCache.getMembers(clan.id!!).map { it.playerId }
                            suggestions.addAll(Bukkit.getOnlinePlayers().filter { player ->
                                player.uniqueId !in members && player.uniqueId != commandSender.uniqueId
                            }.map { it.name })
                        }

                    }

                    AllySubCommands.ADD.command -> {
                        if(clan != null){
                            suggestions.addAll(clanCache.getClanNames().filter { it != clan.name })
                        }
                    }
                    AllySubCommands.ACCEPT.command, AllySubCommands.REJECT.command -> {
                        if(clan != null){
                            // Busca nomes dos clãs que enviaram convite de aliança
                            val senderClanNames = allyInviteCache.getSenderClanNames(clan.id!!)
                            suggestions.addAll(senderClanNames)
                        }
                    }
                }
            }
        }

        return suggestions
    }
}