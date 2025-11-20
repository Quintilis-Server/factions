package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class ClanCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
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
    )
}