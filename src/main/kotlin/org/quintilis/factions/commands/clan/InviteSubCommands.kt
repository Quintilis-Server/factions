package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class InviteSubCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    ACCEPT(
        "accept",
        "/clan invite accept <clan_name>",
        HelpEntry("clan.invite.accept", "factions.usage")
    ),
    REJECT(
        "reject",
        "/clan invite reject <clan_name>",
        HelpEntry("clan.invite.reject", "factions.usage")
    ),
    CANCEL(
        "cancel",
        "/clan invite cancel <player_name>",
        HelpEntry("clan.invite.cancel", "factions.usage")
    ),
    LIST(
        "list",
        "/clan invite list",
        HelpEntry("clan.invite.list", "factions.usage")
    )
}