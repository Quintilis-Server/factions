package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class ClanCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    CREATE(
        "create",
        "/clan create [name] [tag]",
        HelpEntry(
            "clan.create.description",
            "factions.usage"
        )
    ),
    DELETE(
        "delete",
        "/clan delete",
        HelpEntry(
            "clan.delete.description",
            "factions.usage"
        )
    ),
    LIST(
        "list",
        "/clan list <page>",
        HelpEntry(
            "clan.list.description",
            "factions.usage"
        )
    ),
    ALLY(
        "ally",
        "/clan ally <subcommand>",
        HelpEntry(
            "clan.ally.description",
            "factions.usage"
        ),
        AllySubCommands.entries.toTypedArray()
    ),
    QUIT(
        "quit",
        "/clan quit",
        HelpEntry(
            "clan.quit.description",
            "factions.usage"
        )
    ),
    MEMBER(
        "member",
        "/clan member <subcommand>",
        HelpEntry("clan.member.description", "factions.usage"),
        MemberSubCommands.entries.toTypedArray()
    ),
    INVITE(
        "invite",
        "/clan invite <subcommand>",
        HelpEntry("clan.invite.description", "factions.usage"),
        InviteSubCommands.entries.toTypedArray()
    )
}