package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class MemberSubCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    INVITE(
        "invite",
        "/clan member invite <player>",
        HelpEntry("member.invite.description", "factions.usage"),
    ),
    REMOVE(
        "remove",
        "/clan member remove <member>",
        HelpEntry("member.remove.description", "factions.usage"),
    ),
    PROMOTE(
        "promote",
        "/clan member promote <member>",
        HelpEntry("member.promote.description", "factions.usage"),
    ),
    LIST(
        "list",
        "/clan member list",
        HelpEntry("member.list.description", "factions.usage"),
    )
}