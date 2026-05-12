package org.quintilis.factions.listeners

import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.events.ClanAllyCreateEvent
import org.quintilis.factions.events.ClanAllyDeleteEvent
import org.quintilis.factions.events.ClanCreateEvent
import org.quintilis.factions.events.ClanDisbandEvent
import org.quintilis.factions.events.MemberAcceptEvent
import org.quintilis.factions.events.MemberRemoveEvent
import org.quintilis.factions.events.WarBeginEvent
import org.quintilis.factions.extensions.broadcastTranslatable


@AutoRegister
class BroadcastListener: Listener {
    private val server = Bukkit.getServer()

    //Clã
    @EventHandler
    fun onClanCreate(event: ClanCreateEvent) {
        server.broadcastTranslatable(
            "broadcast.clan.create",
            Argument.string("clan_name", event.clan.name),
            Argument.string("leader", event.player.name)
        )
    }
    @EventHandler
    fun onClanDisband(event: ClanDisbandEvent) {
        server.broadcastTranslatable(
            "broadcast.clan.disband",
            Argument.string("clan_name", event.clan.name)
        )
    }

    //Ally
    @EventHandler
    fun onClanAllyCreate(event: ClanAllyCreateEvent) {
        server.broadcastTranslatable(
            "broadcast.clan.ally.create",
            Argument.string("clan_name", event.clan.name),
            Argument.string("clan_name2", event.clan2.name)
        )
    }

    @EventHandler
    fun onClanAllyDelete(event: ClanAllyDeleteEvent) {
        server.broadcastTranslatable(
            "broadcast.clan.ally.delete",
            Argument.string("clan_name", event.clan.name),
            Argument.string("clan_name2", event.clan2.name)
        )
    }

    //Member
    @EventHandler
    fun onMemberAdd(event: MemberAcceptEvent){
        event.clan.broadcastTranslatable(
            "broadcast.member.add",
            Argument.string("player_name", event.member.name)
        )
    }

    @EventHandler
    fun onMemberRemove(event: MemberRemoveEvent){
        event.clan.broadcastTranslatable(
            "broadcast.member.remove",
            Argument.string("player_name", event.member.name)
        )
    }

    @EventHandler
    fun onWarBegin(event: WarBeginEvent){
        server.broadcastTranslatable(
            "broadcast.war.begin",
            Argument.string("attacker_clan", event.attackerClan.name),
            Argument.string("receiver_clan", event.receiverClan.name)
        )
    }
}